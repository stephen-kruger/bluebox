package com.bluebox.search;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;

/*
 * This search implementation simply falls back to the underlying storage implementation instead of using Lucence, Solr etc.
 * It's light-weight, probably more solid over the long term, but has less functionality and performance for detailed searching.
 */
public class StorageIndexer implements SearchIf {
	private static final Logger log = LoggerFactory.getLogger(StorageIndexer.class);

	protected StorageIndexer() throws Exception {
		log.info("Started StorageIndexer");
	}

	public void stop() {
		SearchFactory.stopInstance();
		log.info("Stopped StorageIndexer");
	}

	public Object[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SortFields orderBy, boolean ascending) throws Exception {
		return StorageFactory.getInstance().search(querystr, fields,  start,  count, orderBy, ascending);
	}

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchUtils.SearchFields fields, SearchUtils.SortFields orderBy, boolean ascending) throws Exception {
		Object[] hits = search(search, fields, start, count, orderBy, ascending);
		JSONObject curr;
		writer.write("[");
		Document doc;
		for (int i = 0; i < hits.length; i++) {
			doc = (Document)hits[i];
			String uid = doc.getString(BlueboxMessage.UID);
			try {
				curr = new JSONObject();
				curr.put(BlueboxMessage.FROM, doc.get(BlueboxMessage.FROM));
				curr.put(BlueboxMessage.SUBJECT, doc.get(BlueboxMessage.SUBJECT));
				//				log.info(">>>>>>>>>>{}",doc.get(BlueboxMessage.SUBJECT));
				if (doc.get(BlueboxMessage.RECEIVED) instanceof Date)
					curr.put(BlueboxMessage.RECEIVED, doc.getDate(BlueboxMessage.RECEIVED));
				else
					curr.put(BlueboxMessage.RECEIVED, Timestamp.valueOf(doc.getString(BlueboxMessage.RECEIVED)));
				if (doc.get(BlueboxMessage.SIZE) instanceof Long)
					curr.put(BlueboxMessage.SIZE, (doc.getLong(BlueboxMessage.SIZE)/1000)+"K");
				else
					curr.put(BlueboxMessage.SIZE, (Long.valueOf( doc.getString(BlueboxMessage.SIZE))/1000) +"K");
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
		// do nothing
	}

	/* 
	 * Stall the commit unless a certain timeout has been reached, 
	 * to prevent multiple, consecutive commits 
	 */
	public void commit(boolean force) throws SolrServerException, IOException {
		// do nothing
	}

	@Override
	public synchronized void deleteDoc(String uid) throws SolrServerException, IOException {
		// do nothing
	}

	@Override
	public void deleteDoc(String value, SearchUtils.SearchFields field) throws SolrServerException, IOException {
		// do nothing
	}

	@Override
	public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws IOException, SolrServerException {
		// do nothing
	}

	@Override
	public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received, boolean commit) throws IOException, SolrServerException {
		// do nothing
	}

	@Override
	public void deleteIndexes() throws IOException {
		// do nothing
	}

	@Override
	public boolean containsUid(String uid) {
		return StorageFactory.getInstance().contains(uid);
	}

	private boolean contains(JSONArray children, String name) {
		for (int i = 0; i < children.length();i++) {
			try {
				if (children.getJSONObject(i).getString("name").equals(name)) {
					return true;
				}
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/* 
	 * Find which one of the potential recipients of this mail matches the specified inbox
	 * 
	 */
	public InboxAddress getRecipient(InboxAddress inbox, String recipients) {
		StringTokenizer tok = new StringTokenizer(recipients,",");
		while (tok.hasMoreElements()) {
			String tk = tok.nextToken();
			try {
				InboxAddress curr = new InboxAddress(Utils.decodeRFC2407(tk));
				if (inbox.getAddress().equalsIgnoreCase(curr.getAddress()))
					return curr;
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return inbox;
	}

	@Override
	public JSONArray autoComplete(String hint, long start, long count) {
		JSONObject curr;
		JSONArray children = new JSONArray();

		if (hint.length()==1) {
			return children;
		}

		try {
			Object[] results = search(SearchUtils.autocompleteQuery(hint), SearchUtils.SearchFields.RECIPIENT, (int)start, (int)count*10, SearchUtils.SortFields.SORT_RECEIVED,false);
			for (int i = 0; i < results.length;i++) {
				Document result = (Document)results[i];
				InboxAddress inbox;
				inbox = new InboxAddress(result.getString(Utils.decodeRFC2407(BlueboxMessage.INBOX)));
//				log.info(">>>>>>>>>>>>>>>>>>>>>{}",result.getString(Utils.decodeRFC2407(BlueboxMessage.INBOX)));
				if (!contains(children,inbox.getAddress())) {
					curr = new JSONObject();
					curr.put("name", inbox.getAddress());
					curr.put("label",StringEscapeUtils.unescapeJava(getRecipient(inbox,result.getString(BlueboxMessage.RECIPIENT)).getFullAddress()));
					curr.put("identifier", result.getString(BlueboxMessage.UID));
					children.put(curr);
				}
				if (children.length()>=count)
					break;

			}
		}
		catch (Throwable t) {
			log.error("Error during type-ahead",t);
		}
		return children;
	}




}
