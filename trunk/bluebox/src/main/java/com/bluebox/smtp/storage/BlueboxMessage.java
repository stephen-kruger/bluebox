package com.bluebox.smtp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.mortbay.jetty.HttpStatus;

import com.bluebox.MimeMessageParser;
import com.bluebox.Utils;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.smtp.InboxAddress;

public class BlueboxMessage {
	public static final String UID = "Uid";
	public static final String JSON = "Json";
	public static final String FROM = "Sender";
	public static final String TO = "Recipient";
	public static final String CC = "Cc";
	public static final String SUBJECT = "Subject";
	public static final String RECEIVED = "Received";
	public static final String STATE = "State";
	public static final String SIZE = "Size";
	public static final String INBOX = "Inbox";
	public static final String COUNT = "Count";
	public static final String AUTO_COMPLETE = "Autocomplete";
	public static final String RAW = "pic";
	public static final String ATTACHMENT = "Attachment";
	public static final String HTML_BODY = "HtmlBody";
	public static final String TEXT_BODY = "TextBody";

	public enum State {ANY, NORMAL, DELETED};

	private static final Logger log = Logger.getAnonymousLogger();
	private JSONObject properties = new JSONObject();
	private MimeMessage mmw;
	private MimeMessageParser parser;

	public BlueboxMessage(String id) {
		setProperty(UID,id);
	}

	public BlueboxMessage(String id, InboxAddress inbox) {
		this(id);
		setInbox(inbox);
	}

	public MimeMessage getBlueBoxMimeMessage() throws SQLException, IOException, MessagingException {
		return mmw;
	}

	public void setBlueBoxMimeMessage(String from, MimeMessage bbmm) throws IOException, MessagingException, SQLException {
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
		setProperty(RAW, Utils.convertStreamToString(Utils.streamMimeMessage(bbmm)));
		setProperty(STATE, State.NORMAL.name());
		setProperty(SIZE, bbmm.getSize());
	}

	public void loadBlueBoxMimeMessage(MimeMessage bbmm) {
		mmw = bbmm;
	}

	/*
	 * Figure out who this mail was addressed to so we can set the "To" field for use in type-ahead matching.
	 * It could be a bcc, cc or a to
	 */
	public static InternetAddress getRecipient(InboxAddress inbox, MimeMessage bbmm) throws AddressException {
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

	private JSONArray getRecipient(RecipientType rtype) {
		JSONArray ja = new JSONArray();
		try {
			Address[] r = getBlueBoxMimeMessage().getRecipients(rtype);
			return toJSONArray(r);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return ja;
	}

	private JSONArray toJSONArray(Address[] r) {
		JSONArray ja = new JSONArray();
		try {
			if (r!=null)
				for (int i = 0; i < r.length;i++)
					ja.put(r[i].toString());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return ja;
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
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.getAttachmentList().get(Integer.parseInt(index));
			writeDataSource(ds,resp);
		}
		catch (Exception se) {
			log.info("Problem writing attachment :"+se.getMessage());
		}
	}

	public void writeInlineAttachment(String cid, HttpServletResponse resp) throws SQLException, IOException, MessagingException {
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.getDataSourceForCid(cid);
			if (ds==null) {
				// try by attachment name
				ds = parser.findAttachmentByName(cid);
			}
			writeDataSource(ds,resp);
		}
		catch (Exception se) {
			log.warning("Problem writing inline attachment :"+se.getMessage());
		}
	}

	private void writeDataSource(DataSource ds, HttpServletResponse resp) throws IOException {
		try {
			if (ds==null)
				throw new Exception("No attachment found");
			resp.setContentType(ds.getContentType());
			Utils.copy(ds.getInputStream(),resp.getOutputStream());		
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
			resp.sendError(HttpStatus.ORDINAL_404_Not_Found, t.getMessage());
		}
		finally {
			resp.flushBuffer();
		}
	}

	public String toJSON(boolean lite) throws Exception {
		return toJSON(Locale.getDefault(),lite);
	}

	public String toJSON(Locale locale, boolean lite) throws Exception {
		JSONObject json;
		try {
			//			if (lite) {
			//				json = new JSONObject();				
			//			}
			//			else {
			json = new JSONObject();				
			//				json = getBlueBoxMimeMessage().toJSON(getProperty(UID),locale);	

			List<DataSource> ds = getParser().getAttachmentList();
			JSONArray ja = new JSONArray();
			for (DataSource d : ds) {
				ja.put(d.getName());
			}
			json.put(ATTACHMENT, ja);
			json.put(BlueboxMessage.HTML_BODY, getHtml());
			json.put(BlueboxMessage.TEXT_BODY, getText());

			// now convert the date to a user locale specific one
//			try {
//				log.fine("Converting to locale "+locale.toString()+" for inbox "+getInbox());
//				Date date = new Date(getLongProperty(RECEIVED));
//				if (!json.has("Date")) {
//					json.put("Date",new JSONArray());
//				}
//				json.getJSONArray("Date").put(0, dateToString(date,locale));
//			}
//			catch (Throwable t) {
//				log.warning("Problem converting date to user locale :"+t.getMessage());
//			}
			json.put(UID,properties.get(UID));
			json.put(FROM,toJSONArray(getBlueBoxMimeMessage().getFrom()));
			json.put(SUBJECT,getBlueBoxMimeMessage().getSubject());
			json.put(INBOX,properties.get(INBOX));
			json.put(RECEIVED,properties.getLong(RECEIVED));
			json.put(STATE,properties.get(STATE));
			json.put(SIZE,properties.get(SIZE));

			json.put(TO,getRecipient(MimeMessage.RecipientType.TO));
			json.put(CC,getRecipient(MimeMessage.RecipientType.CC));
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

	public static String convertCidLinks(String uid, String htmlString) {
		try {
			return htmlString.replaceAll("cid:", "../"+JSONInlineHandler.JSON_ROOT+"/"+uid+"/");
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return htmlString;
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

	public static String getFrom(String from, MimeMessage bbmm) throws MessagingException {
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

	protected MimeMessageParser getParser() throws Exception {
		if (parser==null) {
			MimeMessageParser p = new MimeMessageParser(getBlueBoxMimeMessage());
			parser = p.parse();
		}
		return parser;
	}

	public String getText() {
		try {
			String text =  getParser().getPlainContent();
			if (text==null)
				text = "";
			return text;
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

	public String getHtml() {
		try {
			String html = getParser().getHtmlContent();
			if (html==null)
				html = "";
			return convertCidLinks(this.getIdentifier(),html);
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return "";
	}

	public InputStream getRawMessage() throws IOException, MessagingException, SQLException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		getBlueBoxMimeMessage().writeTo(os);
		return new ByteArrayInputStream(os.toByteArray());
	}
}
