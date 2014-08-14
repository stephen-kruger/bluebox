package com.bluebox.smtp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
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

import org.apache.commons.httpclient.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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

	public void setBlueBoxMimeMessage(String from, Date received, MimeMessage bbmm) throws IOException, MessagingException, SQLException {
		mmw = bbmm;
		log.fine("Persisting mime message");
		setProperty(BlueboxMessage.FROM, BlueboxMessage.getFrom(from, bbmm));
		setProperty(INBOX, getInbox().getAddress());
		setProperty(SUBJECT, bbmm.getSubject());
		setLongProperty(RECEIVED, received.getTime());
		setProperty(RAW, Utils.convertStreamToString(Utils.streamMimeMessage(bbmm)));
		setIntProperty(STATE, State.NORMAL.ordinal());
		setProperty(SIZE, bbmm.getSize());
	}
	public State getState() {
		return State.values()[getIntProperty(STATE)];
	}
	
	public void setState(State state) {
		setIntProperty(STATE,state.ordinal());
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

	private JSONArray toJSONArray(Address[] r) {
		JSONArray ja = new JSONArray();
		try {
			if (r!=null)
				for (int i = 0; i < r.length;i++) {
					ja.put(Utils.decodeQuotedPrintable(r[i].toString()));
				}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return ja;
	}

	//	private JSONArray toJSONArrayDisplay(Address[] r) {
	//		JSONArray ja = new JSONArray();
	//		try {
	//			if (r!=null)
	//				for (int i = 0; i < r.length;i++)
	//					ja.put(new InboxAddress(r[i].toString()).getDisplayName());
	//		} catch (Throwable e) {
	//			e.printStackTrace();
	//		}
	//		return ja;
	//	}

	public String getIdentifier() {
		return getProperty(UID);
	}

	public Date getReceived() {
		return new Date(getLongProperty(RECEIVED));
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
			log.info("Setting mime type to "+ds.getContentType());
			resp.setContentType(ds.getContentType());
			Utils.copy(ds.getInputStream(),resp.getOutputStream());		
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			//			t.printStackTrace();
			resp.sendError(HttpStatus.SC_NOT_FOUND, t.getMessage());
		}
		finally {
			resp.flushBuffer();
		}
	}

	public String toJSON() throws Exception {
		return toJSON(Locale.getDefault());
	}

	public String toJSON(Locale locale) throws Exception {
		JSONObject json;
		try {
			json = new JSONObject();				

			List<DataSource> ds = getParser().getAttachmentList();
			JSONArray ja = new JSONArray();
			for (DataSource d : ds) {
				ja.put(d.getName());
			}
			if (ja.length()>0)
				json.put(ATTACHMENT, ja);

			json.put(UID,properties.get(UID));
			json.put(TO,toJSONArray(getBlueBoxMimeMessage().getRecipients(RecipientType.TO)));
			json.put(CC,toJSONArray(getBlueBoxMimeMessage().getRecipients(RecipientType.CC)));
			json.put(FROM,toJSONArray(getBlueBoxMimeMessage().getFrom()));				
			json.put(SUBJECT,getBlueBoxMimeMessage().getSubject());
			json.put(INBOX,properties.get(INBOX));
			json.put(RECEIVED,properties.get(RECEIVED));
			json.put(STATE,properties.get(STATE));
			json.put(SIZE,properties.get(SIZE));

			return json.toString();

		}
		catch (Throwable t) {
			t.printStackTrace();
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

	public void setIntProperty(String name, int value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public int getIntProperty(String name) {
		try {
			return properties.getInt(name);
		} 
		catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public InboxAddress getInbox() throws AddressException {
		return new InboxAddress(getProperty(INBOX));
	}

	public void setInbox(InboxAddress inbox) {
		setProperty(INBOX,inbox.getAddress());
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
