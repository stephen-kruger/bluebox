package com.bluebox.smtp.storage;

import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage.State;

public abstract class AbstractStorage implements StorageIf {
	public static final String DB_NAME = "bluebox401";
	
	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		List<JSONObject> mail = listMailLite(inbox, state, start, count, orderBy, ascending, locale);
		int index = 0;
		writer.write("[");
		for (JSONObject message : mail) {
			writer.write(message.toString(3));
			if ((index++)<mail.size()-1) {
				writer.write(",");
			}
		}
		writer.write("]");
		writer.flush();
	}
	
	public abstract List<JSONObject> listMailLite(InboxAddress inbox, State state, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception;
	
	public abstract String getDBOString(Object dbo, String key, String def);
	public abstract int getDBOInt(Object dbo, String key, int def);
	public abstract long getDBOLong(Object dbo, String key, long def);
	public abstract Date getDBODate(Object dbo, String key, Date def);
	public abstract InputStream getDBORaw(Object dbo, String key);
	
	public BlueboxMessage loadMessage(Object dbo) throws Exception {
//		JSONObject props = new JSONObject();
//		props.put(BlueboxMessage.UID, getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString()));
//		props.put(BlueboxMessage.FROM,getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com"));
//		props.put(BlueboxMessage.RECIPIENT,getDBOString(dbo,BlueboxMessage.RECIPIENT,""));
//		props.put(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
//		props.put(BlueboxMessage.RECEIVED,getDBODate(dbo,BlueboxMessage.RECEIVED, new Date()).getTime());
//		props.put(BlueboxMessage.STATE,getDBOLong(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.ordinal()));
//		props.put(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
//		
//		long size = getDBOLong(dbo,BlueboxMessage.SIZE,0)/1000;
//		if (size==0)
//			size = 1;
//		props.put(BlueboxMessage.SIZE,size);
		return new BlueboxMessage(loadMessageJSON(dbo,Locale.getDefault()),
				Utils.loadEML(getDBORaw(dbo,getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString()))));
	}
	
	/*
	 * Light-weight method of loading only the extracted properties for a message
	 * to allow efficient listing of inbox contents without re-loading the entire MimeMessage.
	 */
	public JSONObject loadMessageJSON(Object dbo, Locale locale) throws Exception {
		JSONObject message = new JSONObject();
		message.put(BlueboxMessage.UID,getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString()));
		message.put(BlueboxMessage.FROM,new JSONArray(getDBOString(dbo,BlueboxMessage.FROM,"['bluebox@bluebox.com']")));
		message.put(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
		message.put(BlueboxMessage.RECIPIENT,getDBOString(dbo,BlueboxMessage.RECIPIENT,""));
		message.put(BlueboxMessage.RECEIVED,getDBODate(dbo,BlueboxMessage.RECEIVED, new Date()).getTime());
//		message.put(BlueboxMessage.RECEIVED,dateToString(getDBODate(dbo,BlueboxMessage.RECEIVED, new Date()),locale));
		message.put(BlueboxMessage.STATE,getDBOLong(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.ordinal()));
		message.put(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
		long size = getDBOLong(dbo,BlueboxMessage.SIZE,0)/1000;
		if (size==0)
			size = 1;
		message.put(BlueboxMessage.SIZE,size+"K");
		return message;
	}
	
	public static String dateToString(Date date, Locale locale) {
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale).format(date);
	}

}
