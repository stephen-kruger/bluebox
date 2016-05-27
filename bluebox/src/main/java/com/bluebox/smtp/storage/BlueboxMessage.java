package com.bluebox.smtp.storage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




//import com.bluebox.MimeMessageParser;
import com.bluebox.Utils;
import com.bluebox.rest.InlineResource;
import com.bluebox.search.SearchUtils;
import com.bluebox.smtp.InboxAddress;

public class BlueboxMessage {
	public static final String UID = StorageIf.Props.Uid.name();
	public static final String JSON = "Json";
	public static final String FROM = StorageIf.Props.Sender.name();
	public static final String TO = "To";
	public static final String CC = "Cc";
	public static final String BCC = "Bcc";
	public static final String ATTACHMENT = "Attachment";
	public static final String HTML_BODY = "HtmlBody";
	public static final String TEXT_BODY = "TextBody";
	public static final String SUBJECT = StorageIf.Props.Subject.name();
	public static final String RECEIVED = StorageIf.Props.Received.name();
	public static final String STATE = StorageIf.Props.State.name();
	public static final String SIZE = StorageIf.Props.Size.name();
	public static final String INBOX = StorageIf.Props.Inbox.name();
	public static final String RECIPIENT = StorageIf.Props.Recipient.name();
	public static final String RAWUID = StorageIf.Props.RawUid.name();
	public static final String COUNT = "Count";

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

	public void setBlueBoxMimeMessage(String from, InboxAddress recipient, Date received, MimeMessage bbmm, String rawId) throws IOException, MessagingException, SQLException, JSONException {
		mmw = bbmm;
		log.debug("Persisting mime message");
		if ((from==null)||(from.length()==0)) {
			setProperty(FROM,toJSONArray(getBlueBoxMimeMessage().getFrom()));
		}
		else {
			setProperty(FROM,toJSONArray(new Address[]{new InternetAddress(from)}));
		}

		setProperty(RAWUID, rawId);
		setProperty(RECIPIENT, StringEscapeUtils.escapeJava(recipient.getFullAddress()));
		setProperty(INBOX, StringEscapeUtils.escapeJava(getInbox().getAddress()));
		if (bbmm.getSubject()==null) {
			setProperty(SUBJECT, "");
		}
		else {
			setProperty(SUBJECT, StringEscapeUtils.escapeJava(bbmm.getSubject()));			
		}
		setLongProperty(RECEIVED, received.getTime());
		setIntProperty(STATE, State.NORMAL.ordinal());
		setLongProperty(SIZE, Utils.getSize(bbmm));
		setProperty(HTML_BODY, SearchUtils.htmlToString(getHtml(null).toLowerCase()));
		setProperty(TEXT_BODY, getText().toLowerCase());
	}

	public State getState() {
		return State.values()[getIntProperty(STATE)];
	}

	public void setState(State state) {
		setIntProperty(STATE,state.ordinal());
	}

	public static JSONArray toJSONArray(Address[] r) {
		JSONArray ja = new JSONArray();
		try {
			if (r!=null)
				for (int i = 0; i < r.length;i++) {
					try {
						ja.put(Utils.decodeQuotedPrintable(r[i].toString()));
					}
					catch (Throwable t) {
						log.error(t.getMessage());
						t.printStackTrace();
					}
				}
		} catch (Throwable e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return ja;
	}

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

	public void writeAttachment(String index, ResponseBuilder response) throws SQLException, IOException, MessagingException {
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.getAttachmentList().get(Integer.parseInt(index));
			response.type(ds.getContentType());
			response.entity(ds);
		}
		catch (Exception se) {
			log.error("Problem writing attachment :{}",se.getMessage());
		}		
	}
	
	@Deprecated
	public void writeAttachment(String index, HttpServletResponse resp) throws SQLException, IOException, MessagingException {
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.getAttachmentList().get(Integer.parseInt(index));
			writeDataSource(ds,resp);
		}
		catch (Exception se) {
			log.error("Problem writing attachment :{}",se.getMessage());
		}
	}

	@Deprecated
	public void writeInlineAttachment(String cid, HttpServletResponse resp) throws SQLException, IOException, MessagingException {
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.findAttachmentByCid(cid);
			if (ds==null) {
				// try by attachment name
				ds = parser.findAttachmentByName(cid);
			}
			writeDataSource(ds,resp);
		}
		catch (Exception se) {
			log.warn("Problem writing inline attachment :{}",se.getMessage());
		}
	}
	
	public void writeInlineAttachment(String cid, ResponseBuilder response) throws SQLException, IOException, MessagingException {
		MimeMessage bbmm = getBlueBoxMimeMessage();
		try {
			MimeMessageParser parser = new MimeMessageParser(bbmm);
			parser.parse();
			DataSource ds = parser.findAttachmentByCid(cid);
			if (ds==null) {
				log.debug("Cid not found {}, trying by name",cid);
				// try by attachment name
				ds = parser.findAttachmentByName(cid);
				if (ds==null) {
					log.debug("Cid not found {}",cid);
					throw new Exception("No attachment found with cid "+cid);
				}
				else {
					log.debug("CID found by name {}",cid);
				}
			}
			else {
				log.debug("CID found by id {}",cid);
			}
			response.type(ds.getContentType());
//			response.entity(ds.getInputStream());		
			response.entity(ds);
		}
		catch (Exception se) {
			log.warn("Problem writing inline attachment :{}",se.getMessage());
		}
	}

	private void writeDataSource(DataSource ds, HttpServletResponse resp) throws IOException {
		try {
			if (ds==null)
				throw new Exception("No attachment found");
			log.debug("Setting mime type to {}",ds.getContentType());
			resp.setContentType(ds.getContentType());
			IOUtils.copy(ds.getInputStream(),resp.getOutputStream());		
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

	//	public JSONObject toJSON() throws Exception {
	//		return toJSON(Locale.getDefault());
	//	}

	public JSONObject toJSON() throws Exception {
		JSONObject json;
		try {
			json = new JSONObject(properties.toString());			

			List<DataSource> ds = getParser().getAttachmentList();
			JSONArray ja = new JSONArray();
			for (DataSource d : ds) {
				ja.put(d.getName());
			}
			if (ja.length()>0)
				json.put(ATTACHMENT, ja);

			return json;

		}
		catch (Throwable t) {
			t.printStackTrace();
			return properties;
		}				
	}

	public static String convertCidLinks(HttpServletRequest request, String uid, String htmlString) {
		try {
			return htmlString.replaceAll("cid:", Utils.getServletBase(request)+InlineResource.PATH+"/get/"+uid);
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return htmlString;
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
		return getLongProperty(SIZE);
	}

	public String getRawUid() {
		return getProperty(RAWUID);
	}

	protected MimeMessageParser getParser() throws Exception {
		if (parser==null) {
			// javax.mail.internet.MimeMultipart cannot be cast to javax.mail.Multipart
			//			javax.mail.internet.MimeMultipart a;
			//			javax.mail.Multipart b;
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
			if (html==null) {
				html = "";
			}
			return convertCidLinks(request,getIdentifier(),html);
		} 
		catch (Throwable e) {
			log.error("Problem getting html content {}",e);
		}
		return "";
	}

	public String getSMTPSender () {
		try {
			String[] header = getBlueBoxMimeMessage().getHeader("Received");
			if ((header!=null)&&(header.length>0)) {
				// discard enclosing [] and ()
				StringTokenizer toks = new StringTokenizer(header[0],"[]()");
				toks.nextToken();// discard the "from"
				String domain = toks.nextToken();
				return domain.trim();
			}
		}
		catch (Throwable t) {
			log.warn("Error checking received header :"+t.getMessage());
		}
		// if no headers found, assume localhost
		return "localhost";
	}


}
