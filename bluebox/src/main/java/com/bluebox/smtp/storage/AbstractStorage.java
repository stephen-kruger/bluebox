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

public abstract class AbstractStorage implements StorageIf {
	private static final Logger log = Logger.getAnonymousLogger();

	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		long startTime = new Date().getTime();
		// the Query has already been requested to start at correct place
		JSONObject curr;
		List<BlueboxMessage> mail = listMail(inbox, state, start, count, orderBy, ascending);
		int index = 0;
		writer.write("[");
		for (BlueboxMessage message : mail) {
			curr = new JSONObject();
			curr.put(BlueboxMessage.FROM, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.FROM)));
			curr.put(BlueboxMessage.SUBJECT, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.SUBJECT)));
			//			curr.put(MessageImpl.SUBJECT,  Utils.convertEncoding(Utils.decodeRFC2407(msg.getPropertyString(MessageImpl.SUBJECT)),"GB2312"));
			//			ByteBuffer b = ByteBuffer.wrap(Utils.decodeRFC2407(msg.getBlueBoxMimeMessage().getSubject()).getBytes());
			//			curr.put(MessageImpl.SUBJECT,  java.nio.charset.Charset.forName("GB2312").newDecoder().decode(b));
			//			curr.put(MessageImpl.SUBJECT, msg.getBlueBoxMimeMessage().getSubject());
			// convert the date to the locale used by the users browser
			if (message.hasProperty(BlueboxMessage.RECEIVED)) {
				//				curr.put(MessageImpl.RECEIVED, new Date(msg.getLongProperty(MessageImpl.RECEIVED)));
				curr.put(BlueboxMessage.RECEIVED, SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, locale).format(new Date(message.getLongProperty(BlueboxMessage.RECEIVED))));
			}
			if (message.hasProperty(BlueboxMessage.SIZE)) {
				curr.put(BlueboxMessage.SIZE, message.getPropertyString(BlueboxMessage.SIZE)+"K");
			}
			else {
				curr.put(BlueboxMessage.SIZE, "1K");
			}
			curr.put(BlueboxMessage.UID, message.getIdentifier());
			writer.write(curr.toString(3));
			if ((index++)<mail.size()-1) {
				writer.write(",");
			}
		}
		writer.write("]");
		writer.flush();
		log.info("Served inbox contents in "+(new Date().getTime()-startTime)+"ms");
	}
	
	public abstract String getDBOString(Object dbo, String key, String def);
	public abstract int getDBOInt(Object dbo, String key, int def);
	public abstract Date getDBODate(Object dbo, String key);
	public abstract InputStream getDBORaw(Object dbo, String key);
	
	public BlueboxMessage loadMessage(Object dbo) throws Exception {
		String uid = getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString());
		BlueboxMessage message = new BlueboxMessage(uid);
		message.setProperty(BlueboxMessage.TO,getDBOString(dbo,BlueboxMessage.TO,"bluebox@bluebox.com"));
//		message.setProperty(BlueboxMessage.AUTO_COMPLETE,getDBOString(dbo,BlueboxMessage.AUTO_COMPLETE,"bluebox@bluebox.com"));
		message.setProperty(BlueboxMessage.FROM,getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com"));
		message.setProperty(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
		message.setLongProperty(BlueboxMessage.RECEIVED,getDBODate(dbo,BlueboxMessage.RECEIVED).getTime());
		message.setProperty(BlueboxMessage.STATE,getDBOString(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.name()));
		message.setProperty(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
		message.loadBlueBoxMimeMessage(Utils.loadEML(getDBORaw(dbo,BlueboxMessage.RAW)));
		int size = message.getBlueBoxMimeMessage().getSize()/1000;
		if (size==0)
			size = 1;
		message.setProperty(BlueboxMessage.SIZE,Integer.toString(size));
		return message;
	}

}
