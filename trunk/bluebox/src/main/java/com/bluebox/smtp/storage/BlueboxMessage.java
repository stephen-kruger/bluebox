package com.bluebox.smtp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.MimeMessageParser;
import com.bluebox.Utils;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.smtp.InboxAddress;

public class BlueboxMessage {
	public static final String UID = "Uid";
	public static final String JSON = "Json";
	public static final String FROM = "Sender";
	public static final String TO = "To";
	public static final String CC = "Cc";
	public static final String SUBJECT = "Subject";
	public static final String RECEIVED = "Received";
	public static final String STATE = "State";
	public static final String SIZE = "Size";
	public static final String INBOX = "Inbox";
	public static final String RECIPIENT = "Recipient";
	public static final String COUNT = "Count";
	public static final String RAW = "pic";
	public static final String ATTACHMENT = "Attachment";
	public static final String HTML_BODY = "HtmlBody";
	public static final String TEXT_BODY = "TextBody";

	public enum State {ANY, NORMAL, DELETED};

	private static final Logger log = LoggerFactory.getLogger(BlueboxMessage.class);
	private JSONObject properties = new JSONObject();
	private MimeMessage mmw;
	private MimeMessageParser parser;

	public BlueboxMessage(String id) {
		setProperty(UID,id);
	}

	public BlueboxMessage(JSONObject jo, MimeMessage message) {
		properties = jo;
		mmw = message;
	}
	
	public BlueboxMessage(String id, InboxAddress inbox) {
		this(id);
		setInbox(inbox);
	}

	public MimeMessage getBlueBoxMimeMessage() throws SQLException, IOException, MessagingException {
		return mmw;
	}

	public void setBlueBoxMimeMessage(String from, InboxAddress recipient, Date received, MimeMessage bbmm) throws IOException, MessagingException, SQLException, JSONException {
		mmw = bbmm;
		log.debug("Persisting mime message");
		setProperty(FROM,toJSONArray(getBlueBoxMimeMessage().getFrom()));
		setProperty(RECIPIENT, recipient.getFullAddress());
		setProperty(INBOX, getInbox().getAddress());
		setProperty(SUBJECT, bbmm.getSubject());
		setLongProperty(RECEIVED, received.getTime());
//		setProperty(RAW, Utils.convertStreamToString(Utils.streamMimeMessage(bbmm)));
		setIntProperty(STATE, State.NORMAL.ordinal());
		setProperty(SIZE, bbmm.getSize());
	}

	public InboxAddress getRecipient() throws AddressException {
		if (this.hasProperty(RECIPIENT))
			return new InboxAddress(getPropertyString(RECIPIENT));
		else
			return new InboxAddress(getPropertyString(INBOX));
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

//	/*
//	 * Figure out who this mail was addressed to so we can set the "To" field for use in type-ahead matching.
//	 * It could be a bcc, cc or a to
//	 */
//	public static InternetAddress getRecipient(InboxAddress inbox, MimeMessage bbmm) throws AddressException {
//		//		InternetAddress inA=new InternetAddress(inbox);
//		//		inbox = inA.getAddress();
//		try {
//			Address[] addr = bbmm.getRecipients(RecipientType.TO);
//			if (addr!=null) {
//				for (int i = 0; i < addr.length; i++) {
//					InternetAddress ia = (InternetAddress) addr[i];
//					if (ia.getAddress().equals(inbox.getAddress())) {
//						log.debug("Found TO recipient");
//						return ia;
//					}
//				}
//			}
//			addr = bbmm.getRecipients(RecipientType.CC);
//			if (addr!=null) {
//				for (int i = 0; i < addr.length; i++) {
//					InternetAddress ia = (InternetAddress) addr[i];
//					if (ia.getAddress().equals(inbox.getAddress())) {
//						log.debug("Found CC recipient");
//						return ia;
//					}
//				}
//			}
//			addr = bbmm.getRecipients(RecipientType.BCC);
//			if (addr!=null) {
//				for (int i = 0; i < addr.length; i++) {
//					InternetAddress ia = (InternetAddress) addr[i];
//					if (ia.getAddress().equals(inbox.getAddress())) {
//						log.debug("Found BCC recipient");
//						return ia;
//					}
//				}
//			}
//		}
//		catch (Throwable t) {
//			t.printStackTrace();
//		}
//		if (inbox.getAddress()!=null)
//			if (inbox.getAddress().length()>0)
//				return new InternetAddress(inbox.getFullAddress());
//		log.severe("Found no recipient for inbox "+inbox+" maybe "+inbox.getFullAddress());
//		return new InternetAddress(inbox.getFullAddress());
//	}

	public static JSONArray toJSONArray(Address[] r) {
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

	private String getPropertyString(String name) {
		try {
			return properties.getString(name);
		} 
		catch (JSONException e) {
			return "";
		}
	}

	private String getProperty(String name) {
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
			log.info("Problem writing attachment :%s",se.getMessage());
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
			log.warn("Problem writing inline attachment :%s",se.getMessage());
		}
	}

	private void writeDataSource(DataSource ds, HttpServletResponse resp) throws IOException {
		try {
			if (ds==null)
				throw new Exception("No attachment found");
			log.debug("Setting mime type to %s",ds.getContentType());
			resp.setContentType(ds.getContentType());
			Utils.copy(ds.getInputStream(),resp.getOutputStream());		
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			//			t.printStackTrace();
			resp.sendError(HttpStatus.SC_NOT_FOUND, t.getMessage());
		}
		finally {
			resp.flushBuffer();
		}
	}

	public JSONObject toJSON() throws Exception {
		return toJSON(Locale.getDefault());
	}

	public JSONObject toJSON(Locale locale) throws Exception {
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

			@SuppressWarnings("unchecked")
			Iterator<String> keys = properties.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				json.put(key, properties.get(key));
			}
			
			return json;

		}
		catch (Throwable t) {
			t.printStackTrace();
			return new JSONObject();
		}				
	}

	public static String convertCidLinks(HttpServletRequest request, String uid, String htmlString) {
		try {
//			log.info(htmlString.replaceAll("cid:", Utils.getServletBase(request)+"/"+JSONInlineHandler.JSON_ROOT+"/"+uid+"/"));
			return htmlString.replaceAll("cid:", Utils.getServletBase(request)+"/"+JSONInlineHandler.JSON_ROOT+"/"+uid+"/");
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

	private void setProperty(String name, Object value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void setLongProperty(String name, long value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private long getLongProperty(String name) {
		try {
			return properties.getLong(name);
		} 
		catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private void setIntProperty(String name, int value) {
		try {
			properties.put(name, value);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private int getIntProperty(String name) {
		try {
			return properties.getInt(name);
		} 
		catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public String getIdentifier() {
		return getProperty(UID);
	}

	public Date getReceived() {
		return new Date(getLongProperty(RECEIVED));
	}
	
	public InboxAddress getInbox() throws AddressException {
		return new InboxAddress(getProperty(INBOX));
	}

	public void setInbox(InboxAddress inbox) {
		setProperty(INBOX,inbox.getAddress());
	}
	
	public String getSubject() {
		return getProperty(SUBJECT);
	}
	
	public JSONArray getFrom() throws JSONException {
		return properties.getJSONArray(FROM);
	}

	public long getSize() {
		return this.getLongProperty(SIZE);
	}
	
//	public static String getFrom(InboxAddress from, MimeMessage bbmm) throws MessagingException {
//		if (from!=null)
//			if (from.length()>0)
//				return from;
//		if (bbmm.getFrom()!=null) {
//			if (bbmm.getFrom().length>0) {
//				return bbmm.getFrom()[0].toString();
//			}
//		}
//		throw new MessagingException("No from address specified");
//	}

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

	public String getHtml(HttpServletRequest request) {
		try {
			String html = getParser().getHtmlContent();
			if (html==null)
				html = "";
			return convertCidLinks(request,getIdentifier(),html);
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
