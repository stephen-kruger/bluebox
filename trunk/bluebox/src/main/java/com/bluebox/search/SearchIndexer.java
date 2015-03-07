package com.bluebox.search;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SearchIndexer {
	private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);
	private Directory directory;
	private IndexWriterConfig config;
	private static SearchIndexer si;
	private IndexWriter indexWriter;
	private IndexSearcher searcher;
	private DirectoryReader diectoryReader;

	private static SearchIndexer getInstance() throws IOException {
		if (si==null) {
			si = new SearchIndexer();
		}
		return si;
	}

	private SearchIndexer() throws IOException {
		//				this(new SimpleFSDirectory(createTempDirectory()));
//		this(new NIOFSDirectory(createTempDirectory()));
		this(new NIOFSDirectory(Paths.get(createTempDirectory().toURI())));
	}

	private SearchIndexer(Directory index) throws IOException {
		this.directory = index;
		log.info("Started SearchIndexer");
	}

	public IndexWriter getIndexWriter() throws IOException {
		if (indexWriter==null) {
			Analyzer analyzer = new StandardAnalyzer();		
//			config = new IndexWriterConfig(Version.LATEST, analyzer);
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
		if (si!=null) {
			try {
				closeIndexWriter();
				closeDirectoryReader();
				closeDirectory();
				si = null;
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			log.warn("Trying to stop an already stopped SearchIndexer instance");
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
		if (diectoryReader==null) {
			diectoryReader = DirectoryReader.open(getDirectory());
		}
		return diectoryReader;
	}

	public void closeDirectoryReader() throws IOException {
		if (diectoryReader!=null) {
			diectoryReader.close();
			diectoryReader=null;
		}
	}

	public IndexSearcher getSearcher() throws IOException {
		if (searcher==null) {
			searcher = new IndexSearcher(getDirectoryReader());
		}
		return searcher;
	}

	public void closeSearcher() {
		searcher=null;
	}

	public Document[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SearchFields orderBy, boolean ascending) throws ParseException, IOException {
		//              querystr = QueryParser.escape(querystr);
		//              querystr = "*"+QueryParser.escape(querystr)+"*";
		//              querystr = "*"+querystr+"*";
		boolean leadingWC=false, trailingWC=false;
		if (querystr.endsWith("*")) {
			querystr = querystr.substring(0,querystr.length()-1);
			trailingWC=true;
		}
		if (querystr.startsWith("*")) {
			querystr = querystr.substring(1,querystr.length());
			leadingWC = true;
		}
		querystr = QueryParser.escape(querystr);

		if (leadingWC)
			querystr = "*"+querystr;
		if (trailingWC) {
			querystr = querystr+"*";
		}
		QueryParser queryParser;

		Analyzer analyzer = new StandardAnalyzer();
		switch (fields) {
		case SUBJECT :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.SUBJECT.name()},
							analyzer);
			break;
		case BODY :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.TEXT_BODY.name(),
							SearchUtils.SearchFields.HTML_BODY.name()},
							analyzer);
			break;
		case RECEIVED :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.RECEIVED.name()},
							analyzer);
			break;
		case FROM :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.FROM.name()},
							analyzer);
			break;
		case RECIPIENT :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.RECIPIENT.name()},
							analyzer);
			break;
		case RECIPIENTS :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchUtils.SearchFields.RECIPIENTS.name()},
							analyzer);
			break;
		case ANY :
		default :
			queryParser = new MultiFieldQueryParser(
					new String[] {
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
				if ((orderBy==SearchUtils.SearchFields.RECEIVED)||(orderBy==SearchUtils.SearchFields.SIZE))
					type = SortField.Type.LONG;
				else
					type = SortField.Type.STRING;
				sort = new Sort(new SortField(orderBy.name(),type,ascending));
			}
			catch (Throwable t) {
				t.printStackTrace();
				log.warn("Unsupported orderBy value :"+orderBy);
				sort = new Sort(new SortField(SearchUtils.SearchFields.RECEIVED.name(),SortField.Type.LONG,ascending));
			}
			// if count is 0, then return only total number of hits, without sending all the data.
			// used to calculate number of search results

			TopFieldCollector collector = TopFieldCollector.create(sort, start+count, true, true, true);
			getSearcher().search(queryParser.parse(querystr),collector);
			ScoreDoc[] hits = collector.topDocs(start,count).scoreDocs;
			Document[] docs = new Document[hits.length];
			for (int i = 0; i < hits.length; i++) {
				int docId = hits[i].doc;
				docs[i] = getSearcher().doc(docId);
			}
			return docs;
		}
		catch (IndexNotFoundException ex) {
			log.error(ex.getMessage());
			ex.printStackTrace();
			return new Document[0];
		}

	}

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchUtils.SearchFields fields, SearchUtils.SearchFields orderBy, boolean ascending) throws ParseException, IOException {
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
				curr.put(BlueboxMessage.SIZE, (Long.parseLong(hits[i].get(SearchUtils.SearchFields.SIZE.name()))/1000)+"K");
				curr.put(BlueboxMessage.UID, uid);
				writer.write(curr.toString(3));
				if (i < hits.length-1) {
					writer.write(",");
				}
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		writer.write("]");
		return hits.length;
	}


	public void indexMail(BlueboxMessage message) throws IOException, JSONException, Exception {
		addDoc(message.getIdentifier(),
				message.getInbox().getFullAddress(),
				Utils.decodeQuotedPrintable(Utils.toCSVString(message.getFrom())),
				Utils.decodeQuotedPrintable(message.getSubject()),
				message.getHtml(null),
				message.getText(),
				getRecipients(message),
				message.getSize(),
				message.getReceived().getTime());
	}

	public void indexMail(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException {
		addDoc(uid,inbox,from,subject,text,html,recipients,size,received);
	}

	/* Find which one of the potential recipeints of this mail matches the specified inbox
	 * 
	 */
	public InboxAddress getRecipient(InboxAddress inbox, String recipients) {
		StringTokenizer tok = new StringTokenizer(recipients,",");
		while (tok.hasMoreElements()) {
			try {
				InboxAddress curr = new InboxAddress(Utils.decodeRFC2407(tok.nextToken()));
				if (inbox.getAddress().equalsIgnoreCase(curr.getAddress()))
					return curr;
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return inbox;
	}

	private String getRecipients(BlueboxMessage message) throws Exception {
		MimeMessage bbmm = message.getBlueBoxMimeMessage();
		StringBuffer sb = new StringBuffer();
		Address[] addr = bbmm.getAllRecipients();
		if (addr!=null) {
			for (int i = 0; i < addr.length;i++) {
				sb.append(Utils.decodeQuotedPrintable(addr[i].toString())).append(",");
			}
		}
		return sb.toString().trim();
	}

	public synchronized void deleteDoc(String uid) throws IOException, ParseException {
		getIndexWriter().deleteDocuments(new Term(SearchUtils.SearchFields.UID.name(),uid));
		getIndexWriter().commit();
		closeDirectoryReader();
		closeSearcher();
	}

	public void deleteDoc(String value, SearchUtils.SearchFields field) throws ParseException, IOException {
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser queryParser = new MultiFieldQueryParser(
				new String[] {
						field.name()},
						analyzer);
		queryParser.setAllowLeadingWildcard(true);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		Query query = queryParser.parse(value);
		getIndexWriter().deleteDocuments(query);
		getIndexWriter().commit();
		closeDirectoryReader();
		closeSearcher();
	}

	protected synchronized void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException {
		log.debug("Indexing mail [] []",uid,from);
		Document doc = new Document();
		doc.add(new StringField(SearchUtils.SearchFields.UID.name(), uid, Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.FROM.name(), from, Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.RECIPIENT.name(), SearchUtils.getRecipient(recipients,inbox), Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.INBOX.name(), inbox, Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.SUBJECT.name(), subject, Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.TEXT_BODY.name(), text, Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.HTML_BODY.name(), SearchUtils.htmlToString(html), Field.Store.YES));
		doc.add(new TextField(SearchUtils.SearchFields.RECIPIENTS.name(), recipients, Field.Store.YES));
		doc.add(new LongField(SearchUtils.SearchFields.SIZE.name(), size, Field.Store.YES));
		doc.add(new LongField(SearchUtils.SearchFields.RECEIVED.name(), received, Field.Store.YES));
		IndexWriter iw = getIndexWriter();
		iw.addDocument(doc);
		iw.commit();
		closeDirectoryReader();
		closeSearcher();
	}
	

	public static File createTempDirectory()  throws IOException {
		//File tmpDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
		File temp = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox4.lucene");
		log.debug("Preparing search indexes in "+temp.getCanonicalPath());
		if(!(temp.mkdir())) {
			log.debug("Re-using index directory: " + temp.getAbsolutePath());
		}
		log.debug("Configured search indexes in "+temp.getCanonicalPath());
		return (temp);
	}

	public synchronized void deleteIndexes() throws IOException {
		getIndexWriter().deleteAll();
		getIndexWriter().commit();
		closeIndexWriter();
		closeDirectoryReader();
		closeSearcher();
	}

}
