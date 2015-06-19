package com.bluebox.smtp.storage;

import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.search.SearchUtils.SortFields;
import com.bluebox.smtp.InboxAddress;

public interface StorageIf {

	public static final String WT_NAME = "dbmaintenance";
	public static final String RAWCLEAN = "rawclean";

	public void start() throws Exception;

	public void stop() throws Exception;
	
	public BlueboxMessage store(String from, InboxAddress recipient, Date received, MimeMessage bbmm, String spooledUid) throws Exception;
	
	public enum Props {Uid,RawUid,Inbox,Recipient,Sender,Subject,Received,State,Size};
	
	/*
	 * Implementations must ensure all the fields in the Props object are persisted.
	 */
	public void store(JSONObject props, String spooledUid) throws Exception;
	
	/*
	 * This method is only ever used when loading out of a zip file from backup restore.
	 */
//	public void store(JSONObject props, InputStream content) throws Exception;

	public BlueboxMessage retrieve(String uid) throws Exception;
	
	public boolean contains(String uid);

	//public void deleteAll(InboxAddress inbox) throws Exception;

	public void deleteAll() throws Exception;

	public long getMailCount(BlueboxMessage.State state)
			throws Exception;
	
	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state)
			throws Exception;

	public List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state,
			int start, int count, String orderBy, boolean ascending)
			throws Exception;
	
	public List<LiteMessage> listMailLite(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception;


	/*
	 * This method writes directly into the server stream, isntead of creating the entire structure in memory, and
	 * then sending it over the wire at the end.
	 * It should allow for larger streams to be sent, more efficiently.
	 */
	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer,
			int start, int count, String orderBy, boolean ascending, Locale locale)
			throws Exception;
	
	public void setState(String uid, BlueboxMessage.State state)
			throws Exception;
		
	public void logError(String title, InputStream content);

	public void logError(String title, String content);

	public int logErrorCount();
	
	public void logErrorClear();
	
	public JSONArray logErrorList(int start, int count);
	
	public String logErrorContent(String id);

	/*
	 * Return a JSONObject with the most active inbox as follows :
	 * {
	 * 	Count : <value>,
	 * 	Inbox : <email>
	 * }
	 */
	public JSONObject getMostActiveInbox();
	
	/*
	 * Return a JSONObject with the most active sender as follows :
	 * {
	 * 	Count : <value>,
	 * 	Sender : <email>
	 * }
	 */
	public JSONObject getMostActiveSender();
		

	public void delete(String uid, String rawId) throws Exception;
	
	/*
	 * Called to perform housekeep tasks on the underlying storage, such as rebuilding indexes.
	 */
	public WorkerThread runMaintenance() throws Exception;
	
	/*
	 * Clean up all blobs not associated with a valid mail entry.
	 */
	public WorkerThread cleanRaw() throws Exception;

	/*
	 * Return a JSON view of the count of email received per day of month
	 * {
	 *  "1" : 200,
	 *  ...
	 *  "31" : 100
	 * }
	 */
	public JSONObject getCountByDay();

	public JSONObject getCountByHour();

	public JSONObject getCountByDayOfWeek();

	/*
	 * Return number of mails per hour
	 */
	public JSONObject getMPH(InboxAddress inbox);
	
	public void setProperty(String key, String value);
	
	public void setLongProperty(String key, long value);
	
	public String getProperty(String key);

	public String getProperty(String key, String defaultValue);
	
	public long getLongProperty(String key);
	
	public long getLongProperty(String key, long defaultValue);

	public boolean hasProperty(String key);
	
	public String spoolStream(InputStream is) throws Exception;
	
	public MimeMessage getSpooledStream(String spooledUid) throws Exception;
	
	public void removeSpooledStream(String spooledUid) throws Exception;

	public long getSpooledStreamSize(String spooledUid) throws Exception;
	
	public long getSpoolCount() throws Exception;
	
	/*
	 * Remove any spooled messages which are not referenced by a mailbox entry
	 */
	public long cleanSpools() throws Exception;

	// time functions are db specific, so expose these mthods here
	
	/*
	 * Return the current time
	 */
	public Date getUTCTime();

	public Object[] search(String querystr, SearchFields fields, int start, int count, SortFields orderBy, boolean ascending) throws Exception;

}