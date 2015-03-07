package com.bluebox.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SolrIndexer {
	private static final Logger log = LoggerFactory.getLogger(SolrIndexer.class);
	private static SolrIndexer si;

	private EmbeddedSolrServer server;
	private static final String CORE_NAME="blueboxcore";

	public static SolrIndexer getInstance() throws Exception {
		if (si==null) {
			si = new SolrIndexer();
		}
		return si;
	}

	private SolrIndexer() throws Exception {
		File solrDir = createTempDirectory("bluebox.solr");
		System.setProperty("solr.solr.home", solrDir.getCanonicalPath());
		writeSolrXml(solrDir);
		writeCoreFiles(solrDir,CORE_NAME);
		CoreContainer coreContainer;
		coreContainer = new CoreContainer(solrDir.getPath());

		coreContainer.load();
		server = new EmbeddedSolrServer(coreContainer, CORE_NAME);

		log.info("Started SearchIndexer");
	}

	private void writeCoreFiles(File solrDir, String coreName) {
		File coreDir = new File(solrDir.getPath()+File.separator+coreName);
		coreDir.mkdir();
		FileWriter fw;
		// core.properties
		try {
			fw = new FileWriter(new File(coreDir.getPath()+File.separator+"core.properties"));
			fw.write("\n");
			fw.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}

		File confDir = new File(solrDir.getPath()+File.separator+coreName+File.separator+"conf");
		confDir.mkdir();

		// schema.xml
		try {
			fw = new FileWriter(new File(confDir.getPath()+File.separator+"schema.xml"));
			fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			fw.write("<schema name=\""+coreName+"\" version=\"1.5\">");
			fw.write("<types>");
			fw.write("<fieldType name=\"string\" class=\"solr.TextField\" positionIncrementGap=\"100\">");
			fw.write("<analyzer type=\"query\">");
			fw.write("<tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>");
//			fw.write("<filter class=\"solr.WordDelimiterFilterFactory\" generateWordParts=\"1\" generateNumberParts=\"0\" catenateWords=\"1\" catenateNumbers=\"1\" catenateAll=\"0\" splitOnCaseChange=\"0\"/>");
			fw.write("<filter class=\"solr.LowerCaseFilterFactory\"/>");
			fw.write(" </analyzer>");
			fw.write("</fieldType>");
			fw.write("<fieldType name=\"long\" class=\"solr.TrieLongField\" positionIncrementGap=\"100\"/>");
			fw.write("</types>");

			fw.write("<fields>");
			fw.write("<field name=\""+SearchUtils.SearchFields.UID.name()+"\" type=\"string\" indexed=\"true\" stored=\"true\" multiValued=\"false\" required=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.INBOX.name()+"\" type=\"string\" indexed=\"true\" stored=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.FROM.name()+"\" type=\"string\" indexed=\"true\" stored=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.RECIPIENT.name()+"\" type=\"string\" indexed=\"true\" stored=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.BODY.name()+"\" type=\"string\" indexed=\"true\" stored=\"false\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.SUBJECT.name()+"\" type=\"string\" indexed=\"true\" stored=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.RECIPIENTS.name()+"\" type=\"string\" indexed=\"true\" stored=\"false\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.HTML_BODY.name()+"\" type=\"string\" indexed=\"true\" stored=\"false\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.TEXT_BODY.name()+"\" type=\"string\" indexed=\"true\" stored=\"false\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.SIZE.name()+"\" type=\"long\" indexed=\"true\" stored=\"true\"/>");
			fw.write("<field name=\""+SearchUtils.SearchFields.RECEIVED.name()+"\" type=\"long\" indexed=\"true\" stored=\"true\"/>");
			fw.write("</fields>");
			fw.write("</schema>\n");
			fw.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		// solrconfig.xml
		try {
			fw = new FileWriter(new File(confDir.getPath()+File.separator+"solrconfig.xml"));
			fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
			fw.write("<config>");
			fw.write("<luceneMatchVersion>5.0.0</luceneMatchVersion>");
			fw.write("<requestHandler name=\"standard\" class=\"solr.StandardRequestHandler\" default=\"true\"/>");
			fw.write("</config>\n");
			fw.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File writeSolrXml(File solrDir) {
		File f=null;
		FileWriter fw;
		try {
			fw = new FileWriter(f = new File(solrDir.getPath()+File.separator+"solr.xml"));
			fw.write("<solr>\n");
			fw.write("</solr>\n");
			fw.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}

	public void stop() {
		if (si!=null) {
			server.shutdown();
			si = null;
		}
		else {
			log.warn("Trying to stop an already stopped SearchIndexer instance");
		}
		log.info("Stopped SearchIndexer");
	}

	public SolrDocument[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SearchFields orderBy, boolean ascending) throws IOException, SolrServerException {
		//		System.out.println("before>>>>>"+querystr+"<<<<<<<<<<");
		SolrQuery query = new SolrQuery();
		if (!querystr.endsWith("*")) {
			querystr = ClientUtils.escapeQueryChars(querystr);
			if (querystr.indexOf(' ')<0) {
				querystr = "*"+querystr+"*";
			}
			else
				querystr = "\""+querystr+"\"";
		}
		else {
			querystr = querystr.replace('*', ' ');
			querystr = querystr.trim();
			querystr = "\""+querystr+"\"";
			//querystr = querystr.replaceAll(" ", "*");
		}
		//		System.out.println("after>>>>>"+querystr+"<<<<<<<<<<");

		if (fields.equals(SearchUtils.SearchFields.ANY))
			query.setQuery(SearchUtils.SearchFields.HTML_BODY.name()+":"+querystr+" OR "+SearchUtils.SearchFields.TEXT_BODY.name()+":"+querystr+" OR "+SearchUtils.SearchFields.SUBJECT.name()+":"+querystr+" OR "+SearchUtils.SearchFields.FROM.name()+":"+querystr+" OR "+SearchUtils.SearchFields.RECIPIENTS.name()+":"+querystr);
		else
			query.setQuery(fields.name()+":"+querystr);
		query.setStart(start); 
		query.addSort(orderBy.name(), SolrQuery.ORDER.desc);
		query.setRequestHandler("standard");

		QueryResponse response = server.query(query);
		SolrDocumentList results = response.getResults();
		//		for (int i = 0; i < results.size(); ++i) {
		//			SolrDocument res = results.get(i);
		//			System.out.println(res);
		//		}
		return results.toArray(new SolrDocument[results.size()]);
	}

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchUtils.SearchFields fields, SearchUtils.SearchFields orderBy, boolean ascending) throws IOException, SolrServerException {
		SolrDocument[] hits = search(search, fields, start, count, orderBy, ascending);
		JSONObject curr;
		writer.write("[");
		for (int i = 0; i < hits.length; i++) {
			String uid = hits[i].getFieldValue(SearchUtils.SearchFields.UID.name()).toString();
			try {
				curr = new JSONObject();
				String from = hits[i].getFieldValue(SearchUtils.SearchFields.FROM.name()).toString();
				try {
					from = new JSONArray(from).getString(0);
					from = new InboxAddress(from).getDisplayName();
				}
				catch (Throwable t) {
					log.error("Was expecting json array :"+from);
					t.printStackTrace();
				}
				curr.put(BlueboxMessage.FROM, from);
				curr.put(BlueboxMessage.SUBJECT, hits[i].getFieldValue(SearchUtils.SearchFields.SUBJECT.name()));
				curr.put(BlueboxMessage.RECEIVED, new Date(Long.parseLong(hits[i].getFieldValue(SearchUtils.SearchFields.RECEIVED.name()).toString())));
				curr.put(BlueboxMessage.SIZE, (Long.parseLong(hits[i].getFieldValue(SearchUtils.SearchFields.SIZE.name()).toString())/1000)+"K");
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


	public void indexMail(BlueboxMessage message, boolean commit) throws IOException, JSONException, Exception {
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
	
	public void commit() throws SolrServerException, IOException {
		server.commit();
	}

//	public void indexMail(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws Exception {
//		addDoc(uid,inbox,from,subject,text,html,recipients,size,received);
//	}

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

	public synchronized void deleteDoc(String uid) throws SolrServerException, IOException {
		deleteDoc(uid,SearchUtils.SearchFields.UID);
	}

	public void deleteDoc(String value, SearchUtils.SearchFields field) throws SolrServerException, IOException {
		server.deleteByQuery(field.name()+":"+value);
		server.commit();
	}

	protected void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException, SolrServerException {
		addDoc(uid, inbox, from, subject, text, html, recipients, size, received, true);
	}
	
	protected void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received, boolean commit) throws IOException, SolrServerException {
		SolrInputDocument doc = new SolrInputDocument();
		String htmlStr = SearchUtils.htmlToString(html);
		doc.addField( SearchUtils.SearchFields.UID.name(), uid);
		doc.addField( SearchUtils.SearchFields.FROM.name(), from);
		doc.addField( SearchUtils.SearchFields.RECIPIENT.name(), SearchUtils.getRecipient(recipients,inbox));
		doc.addField( SearchUtils.SearchFields.INBOX.name(), inbox);
		doc.addField( SearchUtils.SearchFields.SUBJECT.name(), subject);
		doc.addField( SearchUtils.SearchFields.TEXT_BODY.name(), text);
		doc.addField( SearchUtils.SearchFields.HTML_BODY.name(), htmlStr);
		doc.addField( SearchUtils.SearchFields.BODY.name(), htmlStr+text);
		doc.addField( SearchUtils.SearchFields.RECIPIENTS.name(), recipients);
		doc.addField( SearchUtils.SearchFields.SIZE.name(), size);
		doc.addField( SearchUtils.SearchFields.RECEIVED.name(), received);
		server.add(doc);
		if (commit)
			server.commit();
	}

	public static File createTempDirectory(String name)  throws IOException {
		//File tmpDir = (File)getServletContext().getAttribute(ServletContext.TEMPDIR);
		File temp = new File(System.getProperty("java.io.tmpdir")+File.separator+name);
		log.debug("Preparing search indexes in "+temp.getCanonicalPath());
		if(!(temp.mkdir())) {
			log.debug("Re-using index directory: " + temp.getAbsolutePath());
		}
		log.debug("Configured search indexes in "+temp.getCanonicalPath());
		return (temp);
	}

	public void deleteIndexes() throws IOException {
		try {
			server.deleteByQuery("*:*");
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}

}
