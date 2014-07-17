package com.bluebox.smtp.storage;

import java.io.IOException;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;

public class BlueboxMessage {
	public static final String UID = "Uid";
	public static final String JSON = "Json";
	public static final String FROM = "Sender";
	public static final String TO = "Recipient";
	public static final String SUBJECT = "Subject";
	public static final String RECEIVED = "Received";
	public static final String STATE = "State";
	public static final String SIZE = "Size";
	public static final String INBOX = "Inbox";
	public static final String COUNT = "Count";
	public static final String AUTO_COMPLETE = "Autocomplete";
	public static final String RAW = "pic";
	public enum State {ANY, NORMAL, DELETED};

	private static final Logger log = Logger.getAnonymousLogger();
	private JSONObject properties = new JSONObject();
	private MimeMessageWrapper mmw;

	public BlueboxMessage(String id) {
		setProperty(UID,id);
	}

	public BlueboxMessage(String id, InboxAddress inbox) {
		this(id);
		setInbox(inbox);
	}

	public MimeMessageWrapper getBlueBoxMimeMessage() throws SQLException, IOException, MessagingException {
		return mmw;
	}

	public void setBlueBoxMimeMessage(String from, MimeMessageWrapper bbmm) throws IOException, MessagingException, SQLException {
		mmw = bbmm;
		log.fine("Persisting mime message");
		setProperty(AUTO_COMPLETE, getRecipient(getInbox(),bbmm).toString().toLowerCase());
		if ((bbmm.getFrom()!=null)&&(bbmm.getFrom().length>0))
			setProperty(FROM, bbmm.getFrom()[0].toString());
		else
			setProperty(FROM, getProperty(TO));
		setProperty(BlueboxMessage.TO, getInbox().getFullAddress());
		setProperty(BlueboxMessage.FROM, BlueboxMessage.getFrom(from, bbmm));
		setProperty(INBOX, getInbox().getAddress());
		setProperty(SUBJECT, bbmm.getSubject());
		setLongProperty(RECEIVED, new Date().getTime());
		setProperty(RAW, Utils.convertStreamToString(bbmm.getInputStream()));
		setProperty(STATE, State.NORMAL.name());
		// round the size to KB, minimum 1K
		int size = bbmm.getSize()/1000;
		if (size==0)
			size = 1;
		setProperty(SIZE, size);
	}

	public void loadBlueBoxMimeMessage(MimeMessageWrapper bbmm) {
		mmw = bbmm;
	}

	/*
	 * Figure out who this mail was addressed to so we can set the "To" field for use in type-ahead matching.
	 * It could be a bcc, cc or a to
	 */
	public static InternetAddress getRecipient(InboxAddress inbox, MimeMessageWrapper bbmm) throws AddressException {
		//		InternetAddress inA=new InternetAddress(inbox);
		//		inbox = inA.getAddress();
		try {
			Address[] addr = bbmm.getRecipients(RecipientType.TO);
			if (addr!=null) {
				for (int i = 0; i < addr.length; i++) {
					InternetAddress ia = (InternetAddress) addr[i];
					if (ia.getAddress().equals(inbox.getAddress())) {
						log.fine("Found TO recipient");
						return ia;
					}
					//					else {
					//						log.info(Utils.getEmail(ia.getAddress())+" not good for "+inbox);
					//					}
				}
			}
			addr = bbmm.getRecipients(RecipientType.CC);
			if (addr!=null) {
				for (int i = 0; i < addr.length; i++) {
					InternetAddress ia = (InternetAddress) addr[i];
					if (ia.getAddress().equals(inbox.getAddress())) {
						log.fine("Found CC recipient");
						return ia;
					}
					//					else {
					//						log.info(Utils.getEmail(ia.getAddress()));
					//					}
				}
			}
			addr = bbmm.getRecipients(RecipientType.BCC);
			if (addr!=null) {
				for (int i = 0; i < addr.length; i++) {
					InternetAddress ia = (InternetAddress) addr[i];
					if (ia.getAddress().equals(inbox.getAddress())) {
						log.fine("Found BCC recipient");
						return ia;
					}
					//					else {
					//						log.info(Utils.getEmail(ia.getAddress()));
					//					}
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		if (inbox.getAddress()!=null)
			if (inbox.getAddress().length()>0)
				return new InternetAddress(inbox.getFullAddress());
		log.severe("Found no recipient for inbox "+inbox+" maybe "+inbox.getFullAddress());
		return new InternetAddress(inbox.getFullAddress());
	}

	public String getIdentifier() {
		return getProperty(UID);
	}

	public String getPropertyString(String name) {
		try {
			return properties.getString(name);
		} 
		catch (JSONException e) {
			return "";
		}
	}

	public String getProperty(String name) {
		return getPropertyString(name);
	}

	public boolean hasProperty(String name) {
		return properties.has(name);
	}

	public void writeAttachment(String index, HttpServletResponse resp) throws SQLException, IOException, MessagingException {
		MimeMessageWrapper bbmm = getBlueBoxMimeMessage();
		try {
			bbmm.writeAttachment(Integer.parseInt(index), resp);
			resp.flushBuffer();
		}
		catch (SocketException se) {
			log.info("Problem writing attachment :"+se.getMessage());
		}
	}

	public void writeInlineAttachment(String name, HttpServletResponse resp) throws SQLException, IOException, MessagingException {
		MimeMessageWrapper bbmm = getBlueBoxMimeMessage();
		try {
			bbmm.writeInlineAttachment(name, resp);
			resp.flushBuffer();
		}
		catch (SocketException se) {
			log.info("Problem writing inline attachment :"+se.getMessage());
		}
	}

	public String toJSON(boolean lite) throws Exception {
		return toJSON(Locale.getDefault(),lite);
	}

	public String toJSON(Locale locale, boolean lite) throws Exception {
		JSONObject json;
		try {
			if (lite) {
				json = new JSONObject();				
			}
			else {
				json = getBlueBoxMimeMessage().toJSON(getProperty(UID),locale);				
			}
			// now convert the date to a user locale specific one
			try {
				log.fine("Converting to locale "+locale.toString()+" for inbox "+getInbox());
				//			log.info(node.getProperty(RECEIVED).getString());
				Date date = new Date(getLongProperty(RECEIVED));
				if (!json.has("Date")) {
					json.put("Date",new JSONArray());
				}
				json.getJSONArray("Date").put(0, dateToString(date,locale));
			}
			catch (Throwable t) {
				log.warning("Problem converting date to user locale :"+t.getMessage());
			}
			json.put(UID,properties.get(UID));
			json.put(FROM,properties.get(FROM));
			if (properties.has(SUBJECT))
				json.put(SUBJECT,properties.get(SUBJECT));
			json.put(INBOX,properties.get(INBOX));
			json.put(RECEIVED,properties.getLong(RECEIVED));
			json.put(STATE,properties.get(STATE));
			json.put(SIZE,properties.get(SIZE));
			json.put(TO,properties.get(TO));
			if (properties.has(AUTO_COMPLETE))
				json.put(AUTO_COMPLETE,properties.get(AUTO_COMPLETE));
			else
				json.put(AUTO_COMPLETE,properties.get(TO));

			return json.toString();

		}
		catch (Throwable t) {
			t.printStackTrace();
			// found some funky node that should not be there
			//			log.warning("Removing funky node "+node.getIdentifier());
			//			node.remove();
			//			node.getSession().save();
			return new JSONObject().toString();
		}				
	}

	public static String dateToString(Date date, Locale locale) {
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.MEDIUM, locale).format(date);
	}

	private void setProperty(String name, long value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setProperty(String name, String value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void setLongProperty(String name, long value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public long getLongProperty(String name) {
		try {
			return properties.getLong(name);
		} 
		catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public InboxAddress getInbox() throws AddressException {
		return new InboxAddress(getProperty(TO));
	}

	public void setInbox(InboxAddress inbox) {
		setProperty(INBOX,inbox.getAddress());
		setProperty(TO,inbox.getFullAddress());
	}

	public static String getFrom(String from, MimeMessageWrapper bbmm) throws MessagingException {
		if (from!=null)
			if (from.length()>0)
				return from;
		if (bbmm.getFrom()!=null) {
			if (bbmm.getFrom().length>0) {
				return bbmm.getFrom()[0].toString();
			}
		}
		throw new MessagingException("No from address specified");
	}
}
