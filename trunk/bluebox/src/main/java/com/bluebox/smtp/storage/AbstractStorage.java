package com.bluebox.smtp.storage;

import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage.State;

public abstract class AbstractStorage implements StorageIf {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String DB_NAME = "bluebox401";
	private static final String[] supportedVersions = new String[] {"bluebox400"};
	
	public void migrate() {
		for (int i = 0; i < supportedVersions.length;i++) {
			migrate(supportedVersions[i]);
		}
	}
	
	public abstract void migrate(String version);
	
	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		long startTime = new Date().getTime();
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
		log.info("Served inbox contents in "+(new Date().getTime()-startTime)+"ms");
	}
	
	public abstract List<JSONObject> listMailLite(InboxAddress inbox, State state, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception;
	
	public abstract String getDBOString(Object dbo, String key, String def);
	public abstract int getDBOInt(Object dbo, String key, int def);
	public abstract long getDBOLong(Object dbo, String key, long def);
	public abstract Date getDBODate(Object dbo, String key);
	public abstract InputStream getDBORaw(Object dbo, String key);
	
	public BlueboxMessage loadMessage(Object dbo) throws Exception {
		String uid = getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString());
		BlueboxMessage message = new BlueboxMessage(uid);
//		message.setProperty(BlueboxMessage.TO,getDBOString(dbo,BlueboxMessage.TO,"bluebox@bluebox.com"));
		message.setProperty(BlueboxMessage.FROM,getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com"));
		message.setProperty(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
		message.setLongProperty(BlueboxMessage.RECEIVED,getDBODate(dbo,BlueboxMessage.RECEIVED).getTime());
		message.setProperty(BlueboxMessage.STATE,getDBOString(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.name()));
		message.setProperty(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
		message.loadBlueBoxMimeMessage(Utils.loadEML(getDBORaw(dbo,uid)));
		
		long size = getDBOLong(dbo,BlueboxMessage.SIZE,0)/1000;
		if (size==0)
			size = 1;
		message.setProperty(BlueboxMessage.SIZE,Long.toString(size));
		return message;
	}
	
	/*
	 * Light-weight method of loading only the extracted properties for a message
	 * to allow efficient listing of inbox contents without re-loading the entire MimeMessage.
	 */
	public JSONObject loadMessageJSON(Object dbo, Locale locale) throws Exception {
		JSONObject message = new JSONObject();
		message.put(BlueboxMessage.UID,getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString()));
		message.put(BlueboxMessage.FROM,new InboxAddress(getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com")).getDisplayName());
		message.put(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
		message.put(BlueboxMessage.RECEIVED,dateToString(getDBODate(dbo,BlueboxMessage.RECEIVED),locale));
		message.put(BlueboxMessage.STATE,getDBOString(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.name()));
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
