package com.bluebox.search;

import com.bluebox.Utils;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;

public class LuceneIndexer implements SearchIf {
    private static final Logger log = LoggerFactory.getLogger(LuceneIndexer.class);
    private Directory directory;
    private IndexWriterConfig config;
    private IndexWriter indexWriter;
    private IndexSearcher searcher;
    private DirectoryReader diectoryReader;
    private StandardAnalyzer analyzer;
    private long lastCommit = new Date().getTime();// last time a commit was performed

    public LuceneIndexer() throws IOException {
        // this(new SimpleFSDirectory(createTempDirectory()));
        // this(new NIOFSDirectory(createTempDirectory()));
        this(new NIOFSDirectory(Paths.get(createTempDirectory().toURI())));
    }

    private LuceneIndexer(Directory index) {
        this.directory = index;
        log.info("Started SearchIndexer");
    }

    public static File createTempDirectory() {
        //File tmpDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
        try {
            File temp = new File(System.getProperty("java.io.tmpdir") + File.separator + "bluebox4.lucene");
            log.debug("Preparing search indexes in {}", temp.getCanonicalPath());
            if (!(temp.mkdir())) {
                log.debug("Re-using index directory: {}", temp.getAbsolutePath());
            }
            log.debug("Configured search indexes in {}", temp.getCanonicalPath());
            return (temp);
        } catch (Throwable t) {
            log.error("Problem allocating temporary directory, defaulting to " + System.getProperty("java.io.tmpdir"), t);
            return new File(System.getProperty("java.io.tmpdir"));
        }
    }

    public IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null) {
            analyzer = new StandardAnalyzer();
            // config = new IndexWriterConfig(Version.LATEST, analyzer);
            config = new IndexWriterConfig(analyzer);
            config.setUseCompoundFile(true);
            indexWriter = new IndexWriter(getDirectory(), config);
        }
        return indexWriter;
    }

    private void closeIndexWriter() throws IOException {
        getIndexWriter().close();
        indexWriter = null;
    }

    public void stop() {
        try {
            closeIndexWriter();
            closeDirectoryReader();
            closeDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SearchFactory.stopInstance();
        }
        log.info("Stopped SearchIndexer");
    }

    public Directory getDirectory() {
        return directory;
    }

    public void closeDirectory() throws IOException {
        getDirectory().close();
        directory = null;
    }

    public DirectoryReader getDirectoryReader() throws IOException {
        if (diectoryReader == null) {
            diectoryReader = DirectoryReader.open(getDirectory());
        }
        return diectoryReader;
    }

    public void closeDirectoryReader() throws IOException {
        if (diectoryReader != null) {
            diectoryReader.close();
            diectoryReader = null;
        }
    }

    public IndexSearcher getSearcher() throws IOException {
        if (searcher == null) {
            searcher = new IndexSearcher(getDirectoryReader());
        }
        return searcher;
    }

    public void closeSearcher() {
        searcher = null;
    }

    public Document[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SortFields orderBy, boolean ascending) throws ParseException, IOException {

        QueryParser queryParser;
        //		log.info("field={}>>>>>>>>>>>>>>query={} sort={}",fields.name(),querystr,orderBy);
        switch (fields) {
            case SUBJECT:
                queryParser = new QueryParser(SearchUtils.SearchFields.SUBJECT.name(), analyzer);
                break;
            case BODY:
                queryParser = new MultiFieldQueryParser(
                        new String[]{
                                SearchUtils.SearchFields.TEXT_BODY.name(),
                                SearchUtils.SearchFields.HTML_BODY.name()},
                        analyzer);
                break;
            case RECEIVED:
                queryParser = new QueryParser(SearchUtils.SearchFields.RECEIVED.name(), analyzer);
                break;
            case FROM:
                queryParser = new QueryParser(SearchUtils.SearchFields.FROM.name(), analyzer);
                break;
            case RECIPIENT:
                queryParser = new QueryParser(SearchUtils.SearchFields.RECIPIENT.name(), analyzer);
                break;
            case RECIPIENTS:
                queryParser = new QueryParser(SearchUtils.SearchFields.RECIPIENTS.name(), analyzer);
                break;
            case ANY:
            default:
                queryParser = new MultiFieldQueryParser(
                        new String[]{
                                SearchUtils.SearchFields.FROM.name(),
                                SearchUtils.SearchFields.SUBJECT.name(),
                                SearchUtils.SearchFields.TEXT_BODY.name(),
                                SearchUtils.SearchFields.HTML_BODY.name(),
                                SearchUtils.SearchFields.RECIPIENTS.name()},
                        analyzer);
        }
        queryParser.setAllowLeadingWildcard(true);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);

        try {


            Sort sort;
            try {
                SortField.Type type;
                if ((orderBy == SearchUtils.SortFields.SORT_RECEIVED) || (orderBy == SearchUtils.SortFields.SORT_SIZE)) {
                    type = SortField.Type.LONG;
                } else {
                    type = SortField.Type.STRING;
                    // hack for the silly docvalue change in solr5
                    // remove when we figure out how to use docvaues
                    type = SortField.Type.LONG;
                    orderBy = SearchUtils.SortFields.SORT_RECEIVED;
                    // end hack
                }
                sort = new Sort(new SortField(orderBy.name(), type, ascending));
            } catch (Throwable t) {
                t.printStackTrace();
                log.warn("Unsupported orderBy value :" + orderBy);
                sort = new Sort(new SortField(SearchUtils.SearchFields.RECEIVED.name(), SortField.Type.LONG, ascending));
            }
            // if count is 0, then return only total number of hits, without sending all the data.
            // used to calculate number of search results
            TopFieldCollector collector = TopFieldCollector.create(sort, start + count, start + count);
            //TopFieldCollector collector = TopFieldCollector.create(sort, start+count, true, true, true);
            getSearcher().search(queryParser.parse(querystr), collector);
            ScoreDoc[] hits = collector.topDocs(start, count).scoreDocs;
            Document[] docs = new Document[hits.length];
            for (int i = 0; i < hits.length; i++) {
                int docId = hits[i].doc;
                docs[i] = getSearcher().doc(docId);
            }
            return docs;
        } catch (IndexNotFoundException ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
            return new Document[0];
        }

    }

    public long searchInboxes(String search, Writer writer, int start, int count, SearchUtils.SearchFields fields, SearchUtils.SortFields orderBy, boolean ascending) throws ParseException, IOException {
        Document[] hits = search(search, fields, start, count, orderBy, ascending);
        JSONObject curr;
        writer.write("[");
        for (int i = 0; i < hits.length; i++) {
            String uid = hits[i].get(SearchUtils.SearchFields.UID.name());
            try {
                curr = new JSONObject();
                curr.put(BlueboxMessage.FROM, hits[i].get(SearchUtils.SearchFields.FROM.name()));
                curr.put(BlueboxMessage.SUBJECT, hits[i].get(SearchUtils.SearchFields.SUBJECT.name()));
                curr.put(BlueboxMessage.RECEIVED, new Date(Long.parseLong(hits[i].get(SearchUtils.SearchFields.RECEIVED.name()))));
                curr.put(BlueboxMessage.SIZE, (Long.parseLong(hits[i].get(SearchUtils.SearchFields.SIZE.name())) / 1000) + "K");
                curr.put(BlueboxMessage.UID, uid);
                writer.write(curr.toString(3));
                if (i < hits.length - 1) {
                    writer.write(",");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        writer.write("]");
        return hits.length;
    }

    public void indexMail(BlueboxMessage message, boolean commit) throws Exception {
        addDoc(message.getIdentifier(),
                message.getInbox().getFullAddress(),
                Utils.decodeQuotedPrintable(Utils.toCSVString(message.getFrom())),
                Utils.decodeQuotedPrintable(message.getSubject()),
                message.getHtml(null),
                message.getText(),
                getRecipients(message),
                message.getSize(),
                message.getReceived().getTime(),
                commit);
    }

    /*
     * Find which one of the potential recipeints of this mail matches the specified inbox
     *
     */
    public InboxAddress getRecipient(InboxAddress inbox, String recipients) {
        StringTokenizer tok = new StringTokenizer(recipients, ",");
        while (tok.hasMoreElements()) {
            try {
                InboxAddress curr = new InboxAddress(Utils.decodeRFC2407(tok.nextToken()));
                if (inbox.getAddress().equalsIgnoreCase(curr.getAddress()))
                    return curr;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return inbox;
    }

    private String getRecipients(BlueboxMessage message) throws Exception {
        MimeMessage bbmm = message.getBlueBoxMimeMessage();
        StringBuffer sb = new StringBuffer();
        Address[] addr = bbmm.getAllRecipients();
        if (addr != null) {
            for (int i = 0; i < addr.length; i++) {
                sb.append(Utils.decodeQuotedPrintable(addr[i].toString())).append(",");
            }
        }
        return sb.toString().trim();
    }

    public synchronized void deleteDoc(String uid) throws IOException, ParseException {
        getIndexWriter().deleteDocuments(new Term(SearchUtils.SearchFields.UID.name(), uid));
        commit(true);
    }

    public void commit(boolean force) {
        if (force) {
            log.debug("Commit being forced");
            lastCommit = 0;
        }
        long currentTime = new Date().getTime();
        if ((currentTime - lastCommit) > SearchUtils.MAX_COMMIT_INTERVAL) {
            log.debug("Performing delayed commit {}ms", SearchUtils.MAX_COMMIT_INTERVAL - (currentTime - lastCommit));
            try {
                IndexWriter iw = getIndexWriter();
                iw.commit();
                closeDirectoryReader();
                closeSearcher();
            } catch (Throwable t) {
                log.error("Problem commiting", t);
            } finally {
                lastCommit = currentTime;
            }
        } else {
            log.debug("Skipping commit {},", SearchUtils.MAX_COMMIT_INTERVAL - (currentTime - lastCommit));
        }
    }

    public void deleteDoc(String value, SearchUtils.SearchFields field) throws Exception {
        QueryParser queryParser = new MultiFieldQueryParser(
                new String[]{
                        field.name()},
                analyzer);
        queryParser.setAllowLeadingWildcard(true);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);
        Query query = queryParser.parse(value);
        getIndexWriter().deleteDocuments(query);
        commit(true);
    }

    public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException {
        this.addDoc(uid, inbox, from, subject, text, html, recipients, size, received, true);
    }

    public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received, boolean force) throws IOException {
        log.debug("Indexing mail [] []", uid, from);
        Document doc = new Document();

        doc.add(new StringField(SearchUtils.SearchFields.UID.name(), uid, Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.FROM.name(), from, Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.RECIPIENT.name(), SearchUtils.getRecipient(recipients, inbox), Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.INBOX.name(), inbox, Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.SUBJECT.name(), subject, Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.TEXT_BODY.name(), text, Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.HTML_BODY.name(), SearchUtils.htmlToString(html), Field.Store.YES));
        doc.add(new TextField(SearchUtils.SearchFields.RECIPIENTS.name(), recipients, Field.Store.YES));
        doc.add(new NumericDocValuesField(SearchUtils.SortFields.SORT_SIZE.name(), size));
        doc.add(new LongPoint(SearchUtils.SearchFields.SIZE.name(), size));
        doc.add(new NumericDocValuesField(SearchUtils.SortFields.SORT_RECEIVED.name(), received));
        doc.add(new LongPoint(SearchUtils.SearchFields.RECEIVED.name(), size));
        IndexWriter iw = getIndexWriter();
        iw.addDocument(doc);
        commit(force);
    }

    public synchronized void deleteIndexes() throws IOException {
        getIndexWriter().deleteAll();
        commit(true);
    }

    @Override
    public boolean containsUid(String uid) {
        try {
            Query q = new TermQuery(new Term(SearchUtils.SearchFields.UID.name(), uid));
            return (getSearcher().search(q, 10).totalHits.value > 0);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public JSONArray autoComplete(String hint, long start, long count) {
        JSONObject curr;
        JSONArray children = new JSONArray();

        if (hint.length() == 1) {
            return children;
        }

        try {
            Object[] results = search(SearchUtils.autocompleteQuery(hint), SearchUtils.SearchFields.RECIPIENT, (int) start, (int) count * 10, SearchUtils.SortFields.SORT_RECEIVED, false);
            for (int i = 0; i < results.length; i++) {
                Document result = (Document) results[i];
                String uid = result.get(SearchFields.UID.name());
                InboxAddress inbox;
                inbox = new InboxAddress(result.get(Utils.decodeRFC2407(SearchFields.INBOX.name())));

                if (!contains(children, inbox.getAddress())) {
                    curr = new JSONObject();
                    curr.put("name", inbox.getAddress());
                    curr.put("label", getRecipient(inbox, result.get(SearchFields.RECIPIENT.name())).getFullAddress());
                    curr.put("identifier", uid);
                    children.put(curr);
                }
                if (children.length() >= count)
                    break;

            }
        } catch (Throwable t) {
            log.error("Error during type-ahead", t);
        }
        return children;
    }

    private boolean contains(JSONArray children, String name) {
        for (int i = 0; i < children.length(); i++) {
            try {
                if (children.getJSONObject(i).getString("name").equals(name)) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
