package com.bluebox.smtp.storage;

import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONArray;

import com.bluebox.smtp.InboxAddress;

public interface StorageIf {

	public abstract void start() throws Exception;

	public abstract void stop() throws Exception;
	
	public abstract BlueboxMessage store(InboxAddress inbox, String from, MimeMessage bbmm)
			throws Exception;

	public abstract BlueboxMessage retrieve(String uid)
			throws Exception;

	public abstract void deleteAll(InboxAddress inbox) throws Exception;

	public abstract void deleteAll() throws Exception;

	public abstract long getMailCount(BlueboxMessage.State state)
			throws Exception;
	
	public abstract long getMailCount(InboxAddress inbox, BlueboxMessage.State state)
			throws Exception;

	public abstract List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state,
			int start, int count, String orderBy, boolean ascending)
			throws Exception;

	/*
	 * This method writes directly into the server stream, isntead of creating the entire structure in memory, and
	 * then sending it over the wire at the end.
	 * It should allow for larger streams to be sent, more efficiently.
	 */
	public abstract void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer,
			int start, int count, String orderBy, boolean ascending, Locale locale)
			throws Exception;
	
	public abstract void setState(String uid, BlueboxMessage.State state)
			throws Exception;
	
	public String getProperty(String key, String defaultValue);
	
	public void setProperty(String key, String value);

	public boolean hasProperty(String key);
		
	public abstract void logError(String title, InputStream content);
	
	public abstract int logErrorCount();
	
	public abstract void logErrorClear();
	
	public abstract JSONArray logErrorList(int start, int count);
	
	public abstract String logErrorContent(String id);

	public abstract List<String> listUniqueInboxes();

	public void delete(String uid) throws Exception;
	
	/*
	 * Called to perform housekeep tasks on the underlying storage, such as rebuilding indexes.
	 */
	public void runMaintenance() throws Exception;
}