package com.bluebox.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SearchIndexer {
	private static final Logger log = Logger.getAnonymousLogger();
	private static Version version = Version.LUCENE_4_9;
	private Directory index;
	private IndexWriterConfig config;
	private static SearchIndexer si;
	private IndexWriter indexWriter;
	public enum SearchFields {UID, FROM, TO, SUBJECT, RECEIVED, TEXT_BODY, HTML_BODY, SIZE, RECIPIENTS, ANY, BODY};

	public static SearchIndexer getInstance() throws IOException {
		if (si==null) {
			si = new SearchIndexer();
		}
		return si;
	}

	private SearchIndexer() throws IOException {
		this(new SimpleFSDirectory(createTempDirectory()));
	}

	private SearchIndexer(Directory index) throws IOException {
		this.index = index;
		Analyzer analyzer = new StandardAnalyzer(version);
		config = new IndexWriterConfig(version, analyzer);
		indexWriter = new IndexWriter(index, config);
	}

	public Document[] search(String querystr, SearchFields fields, int start, int count, SearchFields orderBy) throws ParseException, IOException {
//		querystr = QueryParser.escape(querystr);
//		querystr = "*"+QueryParser.escape(querystr)+"*";
//		querystr = "*"+querystr+"*";
		QueryParser queryParser;
		Analyzer analyzer = new StandardAnalyzer(version);
		switch (fields) {

		case SUBJECT :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.SUBJECT.name()},
					analyzer);
			break;
		case TO :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.TO.name()},
					analyzer);
			break;
		case BODY :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.TEXT_BODY.name(),
					SearchFields.HTML_BODY.name()},
					analyzer);
			break;
		case RECEIVED :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.RECEIVED.name()},
					analyzer);
			break;
		case FROM :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.FROM.name()},
					analyzer);
			break;
		case RECIPIENTS :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.FROM.name(),
					SearchFields.RECIPIENTS.name()},
					analyzer);
			break;
		case ANY :
		default :
			queryParser = new MultiFieldQueryParser(version,
					new String[] {
					SearchFields.FROM.name(),
					SearchFields.SUBJECT.name(),
					SearchFields.TEXT_BODY.name(),
					SearchFields.HTML_BODY.name(),
					SearchFields.RECIPIENTS.name()},
					analyzer);
		}
		queryParser.setAllowLeadingWildcard(true);
		queryParser.setDefaultOperator(QueryParser.Operator.AND);
		try {
			IndexReader reader = DirectoryReader.open(index);
			IndexSearcher searcher = new IndexSearcher(reader);

			Sort sort;
			try {
				sort = new Sort(new SortField(orderBy.name(),SortField.Type.STRING));
			}
			catch (Throwable t) {
				log.warning("Unsupported orderBy value :"+orderBy);
				sort = new Sort(new SortField(SearchFields.RECEIVED.name(),SortField.Type.LONG));
			}
			// if count is 0, then return only total number of hits, without sending all the data.
			// used to calculate number of search results

			TopFieldCollector collector = TopFieldCollector.create(sort, start+count, true, true, true, true);
			searcher.search(queryParser.parse(querystr),collector);
			ScoreDoc[] hits = collector.topDocs(start,count).scoreDocs;
			Document[] docs = new Document[hits.length];
			for (int i = 0; i < hits.length; i++) {
				int docId = hits[i].doc;
				docs[i] = searcher.doc(docId);
			}
			return docs;
		}
		catch (IndexNotFoundException ex) {
			log.severe(ex.getMessage());
			ex.printStackTrace();
			log.info("Rebuilding search indexes");
			Inbox.getInstance().rebuildSearchIndexes();
			return new Document[0];
		}
	}

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchFields fields, SearchFields orderBy, boolean ascending) throws ParseException, IOException {
		Document[] hits = search(search, fields, start, count, orderBy);
		JSONObject curr;
		writer.write("[");
		for (int i = 0; i < hits.length; i++) {
			String uid = hits[i].get(SearchFields.UID.name());
			try {
				curr = new JSONObject();
				curr.put(BlueboxMessage.FROM, hits[i].get(SearchFields.FROM.name()));
				curr.put(BlueboxMessage.SUBJECT, hits[i].get(SearchFields.SUBJECT.name()));
				curr.put(BlueboxMessage.RECEIVED, new Date(Long.parseLong(hits[i].get(SearchFields.RECEIVED.name()))));
				curr.put(BlueboxMessage.SIZE, hits[i].get(SearchFields.SIZE.name())+"K");
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
//		JSONObject json = new JSONObject(message.toJSON(false));
		addDoc(message.getIdentifier(),
				message.getInbox().getFullAddress(),
				message.getProperty(BlueboxMessage.FROM),
				message.getProperty(BlueboxMessage.SUBJECT),
				message.getHtml(),
				message.getText(),
				getRecipients(message),
				message.getLongProperty(BlueboxMessage.SIZE),
				message.getLongProperty(BlueboxMessage.RECEIVED));
	}

	private String getRecipients(BlueboxMessage message) throws Exception {
		MimeMessage bbmm = message.getBlueBoxMimeMessage();
		StringBuffer sb = new StringBuffer();
		Address[] addr;
		addr = bbmm.getRecipients(RecipientType.TO);
		if (addr!=null)
			for (int i = 0; i < addr.length;i++) {
				sb.append(addr[i].toString()).append(" ");
			}

		addr = bbmm.getRecipients(RecipientType.BCC);
		if (addr!=null)
			for (int i = 0; i < addr.length;i++) {
				sb.append(addr[i].toString()).append(" ");
			}

		addr = bbmm.getRecipients(RecipientType.CC);
		if (addr!=null)
			for (int i = 0; i < addr.length;i++) {
				sb.append(addr[i].toString()).append(" ");
			}
		return sb.toString().trim();
	}

	public synchronized void deleteDoc(String uid) throws IOException, ParseException {
		indexWriter.deleteDocuments(new Term(SearchFields.UID.name(),uid));
		indexWriter.commit();
	}

	protected synchronized void addDoc(String uid, String to, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException {
		log.fine("Indexing mail "+uid+" "+from);
		Document doc = new Document();
		doc.add(new StringField(SearchFields.UID.name(), uid, Field.Store.YES));
		doc.add(new TextField(SearchFields.FROM.name(), from, Field.Store.YES));
		doc.add(new TextField(SearchFields.TO.name(), to, Field.Store.YES));
		doc.add(new TextField(SearchFields.SUBJECT.name(), subject, Field.Store.YES));
		doc.add(new TextField(SearchFields.TEXT_BODY.name(), text, Field.Store.YES));
		doc.add(new TextField(SearchFields.HTML_BODY.name(), htmlToString(html), Field.Store.YES));
		doc.add(new TextField(SearchFields.RECIPIENTS.name(), recipients, Field.Store.YES));
		doc.add(new LongField(SearchFields.SIZE.name(), size, Field.Store.YES));
		doc.add(new LongField(SearchFields.RECEIVED.name(), received, Field.Store.YES));
		indexWriter.addDocument(doc);
		indexWriter.commit();
	}

	public static String htmlToString(String html) throws IOException {
		final StringBuilder sb = new StringBuilder();
		HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
			public boolean readyForNewline;

			@Override
			public void handleText(final char[] data, final int pos) {
				String s = new String(data);
				sb.append(s.trim()).append(' ');
				readyForNewline = true;
			}

			@Override
			public void handleStartTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				if (readyForNewline && (t == HTML.Tag.DIV || t == HTML.Tag.BR || t == HTML.Tag.P)) {
					sb.append("\n");
					readyForNewline = false;
				}
			}

			@Override
			public void handleSimpleTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				handleStartTag(t, a, pos);
			}
		};
		if (html!=null) {
			try {
				new ParserDelegator().parse(new StringReader(html), parserCallback, false);
			}
			catch (Throwable t) {
				log.warning("Error indexing html body "+t.getMessage());
			}
		}
		return sb.toString().trim();
	}

	public static File createTempDirectory()  throws IOException {
		//File tmpDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
		File temp = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.lucene");


		log.info("Preparing search indexes in "+temp.getCanonicalPath());
		if(!(temp.mkdir()))
		{
			log.warning("Re-using index directory: " + temp.getAbsolutePath());
		}
		log.info("Configured search indexes in "+temp.getCanonicalPath());
		return (temp);
	}

	public synchronized void deleteIndexes() throws IOException {
		indexWriter.deleteAll();
		indexWriter.commit();
	}

}
