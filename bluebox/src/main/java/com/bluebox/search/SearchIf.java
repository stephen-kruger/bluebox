package com.bluebox.search;

import java.io.Writer;

import org.codehaus.jettison.json.JSONArray;

import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public interface SearchIf {

	public void stop();

	public Object[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SortFields orderBy, boolean ascending) throws Exception;

	public long searchInboxes(String search, Writer writer, int start,	int count, SearchUtils.SearchFields fields, SearchUtils.SortFields orderBy, boolean ascending) throws Exception;

	public void indexMail(BlueboxMessage message, boolean commit) throws Exception;
	/* 
	 * Basically stall the commit unless a certain timeout has been reached, 
	 * to prevent multiple, consecutive commits 
	 */
	public void commit(boolean force) throws Exception;

	/* Find which one of the potential recipeints of this mail matches the specified inbox
	 * 
	 */
	public InboxAddress getRecipient(InboxAddress inbox, String recipients);
	
	public void deleteDoc(String uid) throws Exception;

	public void deleteDoc(String value, SearchUtils.SearchFields field) throws Exception;

	public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws Exception;

	public void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received, boolean commit) throws Exception ;

	public void deleteIndexes() throws Exception;

	public boolean containsUid(String uid);
	
	public JSONArray autoComplete(String hint, long start, long count);

}
