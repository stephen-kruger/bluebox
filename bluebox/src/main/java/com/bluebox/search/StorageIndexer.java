package com.bluebox.search;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.bson.Document;

import org.apache.solr.client.solrj.SolrServerException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		for (int i = 0; i < hits.length; i++) {
			String uid = ((Document)hits[i]).getString(BlueboxMessage.UID);
			log.info(hits[i].toString());
			try {
				curr = new JSONObject();
				curr.put(BlueboxMessage.FROM, ((Document)hits[i]).get(BlueboxMessage.FROM));
				curr.put(BlueboxMessage.SUBJECT, ((Document)hits[i]).get(BlueboxMessage.SUBJECT));
				curr.put(BlueboxMessage.RECEIVED, ((Document)hits[i]).getDate(BlueboxMessage.RECEIVED));
				curr.put(BlueboxMessage.SIZE, (((Document)hits[i]).getLong(BlueboxMessage.SIZE)/1000)+"K");
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

	@Override
	public JSONArray autoComplete(String hint, long start, long count) {
		return null;
	}

	


}
