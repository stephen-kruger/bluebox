package com.bluebox.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Date;
import java.util.StringTokenizer;

import javax.mail.Address;
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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
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
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class SearchIndexer {
	private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);
	private Directory index;
	private IndexWriterConfig config;
	private static SearchIndexer si;
	private IndexWriter indexWriter;
	public enum SearchFields {UID, INBOX, FROM, SUBJECT, RECEIVED, TEXT_BODY, HTML_BODY, SIZE, RECIPIENT, RECIPIENTS, ANY, BODY};

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
	}
	
	public IndexWriter getIndexWriter() throws IOException {
		if (indexWriter==null) {
			Analyzer analyzer = new StandardAnalyzer();		
			config = new IndexWriterConfig(Version.LATEST, analyzer);
			config.setUseCompoundFile(true);
			indexWriter = new IndexWriter(index, config);
		}
		return indexWriter;
	}

	public void stop() {
		try {
			getIndexWriter().close();
			index.close();
			si = null;
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Document[] search(String querystr, SearchFields fields, int start, int count, SearchFields orderBy, boolean ascending) throws ParseException, IOException {
		//		querystr = QueryParser.escape(querystr);
		//		querystr = "*"+QueryParser.escape(querystr)+"*";
		//		querystr = "*"+querystr+"*";
		QueryParser queryParser;

		Analyzer analyzer = new StandardAnalyzer();
		switch (fields) {
		case SUBJECT :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.SUBJECT.name()},
							analyzer);
			break;
		case BODY :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.TEXT_BODY.name(),
							SearchFields.HTML_BODY.name()},
							analyzer);
			break;
		case RECEIVED :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.RECEIVED.name()},
							analyzer);
			break;
		case FROM :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.FROM.name()},
							analyzer);
			break;
		case RECIPIENT :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.RECIPIENT.name()},
							analyzer);
			break;
		case RECIPIENTS :
			queryParser = new MultiFieldQueryParser(
					new String[] {
							SearchFields.RECIPIENTS.name()},
							analyzer);
			break;
		case ANY :
		default :
			queryParser = new MultiFieldQueryParser(
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
		DirectoryReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		try {


			Sort sort;
			try {
				SortField.Type type;
				if ((orderBy==SearchFields.RECEIVED)||(orderBy==SearchFields.SIZE))
					type = SortField.Type.LONG;
				else
					type = SortField.Type.STRING;
				sort = new Sort(new SortField(orderBy.name(),type,ascending));
			}
			catch (Throwable t) {
				t.printStackTrace();
				log.warn("Unsupported orderBy value :"+orderBy);
				sort = new Sort(new SortField(SearchFields.RECEIVED.name(),SortField.Type.LONG,ascending));
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
			log.error(ex.getMessage());
			ex.printStackTrace();
			log.info("Rebuilding search indexes");
			Inbox.getInstance().rebuildSearchIndexes();
			return new Document[0];
		}
		finally {
			reader.close();
		}
	}

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchFields fields, SearchFields orderBy, boolean ascending) throws ParseException, IOException {
		Document[] hits = search(search, fields, start, count, orderBy, ascending);
		JSONObject curr;
		writer.write("[");
		for (int i = 0; i < hits.length; i++) {
			String uid = hits[i].get(SearchFields.UID.name());
			try {
				curr = new JSONObject();
				curr.put(BlueboxMessage.FROM, hits[i].get(SearchFields.FROM.name()));
				curr.put(BlueboxMessage.SUBJECT, hits[i].get(SearchFields.SUBJECT.name()));
				curr.put(BlueboxMessage.RECEIVED, new Date(Long.parseLong(hits[i].get(SearchFields.RECEIVED.name()))));
				curr.put(BlueboxMessage.SIZE, (Long.parseLong(hits[i].get(SearchFields.SIZE.name()))/1000)+"K");
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
				Utils.decodeQuotedPrintable(message.getFrom().getString(0)),
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
		getIndexWriter().deleteDocuments(new Term(SearchFields.UID.name(),uid));
		getIndexWriter().commit();
	}

	protected synchronized void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException {
		log.debug("Indexing mail "+uid+" "+from);
		Document doc = new Document();
		doc.add(new StringField(SearchFields.UID.name(), uid, Field.Store.YES));
		doc.add(new TextField(SearchFields.FROM.name(), from, Field.Store.YES));
		doc.add(new TextField(SearchFields.RECIPIENT.name(), getRecipient(recipients,inbox), Field.Store.YES));
		doc.add(new TextField(SearchFields.INBOX.name(), inbox, Field.Store.YES));
		doc.add(new TextField(SearchFields.SUBJECT.name(), subject, Field.Store.YES));
		doc.add(new TextField(SearchFields.TEXT_BODY.name(), text, Field.Store.YES));
		doc.add(new TextField(SearchFields.HTML_BODY.name(), htmlToString(html), Field.Store.YES));
		doc.add(new TextField(SearchFields.RECIPIENTS.name(), recipients, Field.Store.YES));
		doc.add(new LongField(SearchFields.SIZE.name(), size, Field.Store.YES));
		doc.add(new LongField(SearchFields.RECEIVED.name(), received, Field.Store.YES));
		getIndexWriter().addDocument(doc);
		getIndexWriter().commit();
	}

	/*
	 * Figure out which of the recipients this mail is actually being delivered to. If none match, use the Inbox as default;
	 */
	private String getRecipient(String recipients, String inbox) {
		StringTokenizer tok = new StringTokenizer(recipients,",");
		String linbox = inbox.toLowerCase();
		while (tok.hasMoreElements()) {
			String curr = tok.nextToken();
			if (curr.toLowerCase().contains(linbox)) {
				return curr;
			}
		}
		return inbox;
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
			if (html.length()>0) {
				try {
					new ParserDelegator().parse(new StringReader(html), parserCallback, false);
				}
				catch (Throwable t) {
					log.warn("Could not parse html content - indexing all");
					sb.append(html);
				}
			}
		}
		return sb.toString().trim();
	}

	public static File createTempDirectory()  throws IOException {
		//File tmpDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
		File temp = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox4.lucene");


		log.info("Preparing search indexes in "+temp.getCanonicalPath());
		if(!(temp.mkdir()))
		{
			log.warn("Re-using index directory: " + temp.getAbsolutePath());
		}
		log.info("Configured search indexes in "+temp.getCanonicalPath());
		return (temp);
	}

	public synchronized void deleteIndexes() throws IOException {
		getIndexWriter().deleteAll();
		getIndexWriter().commit();
	}

	public WorkerThread validate() {
		WorkerThread wt = new WorkerThread("validatesearch") {

			@Override
			public void run() {
				DirectoryReader reader=null;
				int issueCount = 0;
				try {
					StorageIf si = StorageFactory.getInstance();
					reader = DirectoryReader.open(index);
					Bits liveDocs = MultiFields.getLiveDocs(reader);
					for (int i=0; i<reader.maxDoc(); i++) {
						if (liveDocs != null && !liveDocs.get(i))
							continue;

						Document doc = reader.document(i);
						if (!si.contains(doc.getField(SearchFields.UID.name()).stringValue())) {
							log.warn("Search index out of sync for id {}",doc.getField(SearchFields.UID.name()).stringValue());
							issueCount++;
						}
						setProgress(i*100/reader.maxDoc());
					}
				}
				catch (Throwable t) {
					log.error("Problem vaidating search indexes",t);
				}
				finally {
					setProgress(100);
					setStatus(issueCount+" invalid docs found");
				}
				if (reader!=null) {
					try {
						reader.close();
					} catch (IOException e) {
						log.error("Problem closing search reader",e);
						e.printStackTrace();
					}
				}
			}
		};
		return wt;
	}

}
