package com.bluebox.smtp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.rest.json.JSONInlineHandler;

public class MimeMessageWrapper extends MimeMessage {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String HTML_BODY = "HtmlBody";
	public static final String TEXT_BODY = "TextBody";
	public static final String ATTACHMENT = "Attachment";
	//	public static final String RAW = "RawText";
	public static final String UTF8 = "UTF-8";

	public MimeMessageWrapper(Session session, InputStream is) throws MessagingException {
		super(session, is);
	}

	public MimeMessageWrapper(MimeMessage mm) throws MessagingException {
		super(mm);
	}

	@Override
	public InputStream getInputStream() throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		writeTo(os);
		return new ByteArrayInputStream(os.toByteArray());
	}

	public JSONObject toJSON(String identifier, Locale locale) throws JSONException, MessagingException, IOException {
		JSONObject result = new JSONObject();
		addHeader(TEXT_BODY, "");
		addHeader(HTML_BODY, "");	
		Header header;
		for(@SuppressWarnings("unchecked")
		Enumeration<Header> headers = getAllHeaders(); headers.hasMoreElements(); ) {
			header = headers.nextElement();
			if (header.getName().equals(MimeMessage.RecipientType.TO.toString())) {
				result.put(header.getName(), Utils.getJSONArray(getRecipients(MimeMessage.RecipientType.TO)));					
			}
			else {
				if (header.getName().equals(MimeMessage.RecipientType.CC.toString())) {
					result.put(header.getName(), Utils.getJSONArray(getRecipients(MimeMessage.RecipientType.CC)));					
				}
				else {
					if (header.getName().equals(MimeMessage.RecipientType.BCC.toString())) {
						result.put(header.getName(), Utils.getJSONArray(getRecipients(MimeMessage.RecipientType.BCC)));					
					}
					else {
						// specific to MongoDB, headers cannot contain '.'
						result.put(header.getName().replace('.', '_'), Utils.getJSONArray(getDecodedHeader(header.getName())));
					}
				}
			}
		}

		if (isMultipart()) {
//			log.fine("Processing "+getCount()+" multi-part sections >>>"+((Multipart)getContent()).getCount());
			for (int i = 0; i < getCount(); i++) {
				BodyPart bp = getBodyPart(i);
//				log.fine("Processing body-part "+i+" ContentType="+bp.getContentType()+" ContentType="+bp.getContentType()+" Disposition="+bp.getDisposition()+" FileName="+bp.getFileName());
				if ((bp.getDisposition()==null)||(bp.getDisposition().equals(Part.INLINE))) {
					log.fine("Processing as INLINE");
					getText(identifier,bp);
				}
				else {
					log.fine("Processing as ATTACHMENT");
					String attachmentName;
					if ((attachmentName=bp.getFileName())==null) {
						attachmentName = "unnamed";
					}
					log.fine("Processing attachment:"+attachmentName);
					addHeader(ATTACHMENT,attachmentName);
				}
			}
		}
		else {
			// mail is either single html or single text content
			if (isMimeType("text/html")) {
//				log.fine("Processing text/html "+getCharset());
				if ((getEncoding()!=null)&&(getEncoding().toLowerCase().contains("base64"))) {
//					log.info("1 "+getCharset());
					addHeader(HTML_BODY, convertCidLinks(identifier,new String(Base64.decodeBase64(safeGetContentAsString(getContentStream(),getCharset())))));
				}
				else {
					if ((getEncoding()!=null)&&(getEncoding().toLowerCase().contains("quoted-printable"))) {
//						log.info("2 "+getCharset());
						addHeader(HTML_BODY, convertCidLinks(identifier,Utils.decodeQuotedPrintable(safeGetContentAsString(getContentStream(),getCharset()))));
					}
					else {
//						log.info("3 "+getCharset());
						addHeader(HTML_BODY, convertCidLinks(identifier,safeGetContentAsString(getContentStream(),getCharset())));						
					}
				}
			}
			else {
//				log.info("Processing text "+getContentType()+" "+getEncoding()+" encoding="+getEncoding());
				//				String textBody = safeGetContentAsString(getContentStream(),getCharset());
				String textBody = javax.mail.internet.MimeUtility.decodeText(getContent().toString());//safeGetContentAsString(getContentStream(),getCharset());
				//				try {
				//					if (getEncoding().toLowerCase().contains("base64")) {
				//						log.fine("Decoding base64");
				//						textBody = javax.mail.internet.MimeUtility.decodeText(textBody);//new String(Base64.decodeBase64(javax.mail.internet.MimeUtility.decodeText(textBody)),getCharset());
				//					} else if (getEncoding().toLowerCase().contains("quoted-printable")) {
				//						log.fine("Decoding quoted-printable");
				//						textBody = new String(Utils.decodeRFC2407(textBody));
				//					}
				//				}
				//				catch (Throwable t) {
				//					log.severe("Problem converting base64 string :"+t.getMessage());
				//				}
				//				log.fine("a>>>>>>"+Utils.decodeRFC2407(textBody));
				//				log.fine("b>>>>>>"+javax.mail.internet.MimeUtility.decodeText(textBody));
				//				log.fine("c>>>>>>"+new String(Base64.decodeBase64(textBody),getCharset()));
				//				log.fine("d>>>>>>"+javax.mail.internet.MimeUtility.decodeWord(textBody));

				//				addHeader(TEXT_BODY, javax.mail.internet.MimeUtility.decodeWord(textBody));
				addHeader(TEXT_BODY, textBody);
			}
		}

		// concat the various body headers into a single value
		result = combineHeaders(result,HTML_BODY);
		result = combineHeaders(result,TEXT_BODY);
		result.put(ATTACHMENT,Utils.getJSONArray(getHeader(ATTACHMENT)));
		return result;
	}

	private String getCharset() {
		try {
			return getCharset(getContentType());
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return UTF8;
	}

	public static String getCharset(String contentType) {
		StringTokenizer props = new StringTokenizer(contentType,";");
		try {
			while (props.hasMoreElements()) {
				Properties prop = new Properties();
				prop.load(new StringReader(props.nextElement().toString()));
				if (prop.containsKey("charset")) {
					return javax.mail.internet.MimeUtility.javaCharset(stripLeadingAndTrailingQuotes(prop.getProperty("charset")));
				}
			}
			return javax.mail.internet.MimeUtility.javaCharset(UTF8);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return UTF8;
	}

	public static String safeGetContentAsString(InputStream is, String charset) throws IOException {
		InputStreamReader reader = new InputStreamReader(is, Charset.forName(charset));
		StringBuffer sb = new StringBuffer();
		try {
			int byteRead = 0;
			while ((byteRead=reader.read())>=0) {
				sb.append((char)byteRead);
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
			sb.append(t.getMessage());
		}
		finally {
			reader.close();
		}
		return sb.toString();
	}

	public static String xsafeGetContentAsString(InputStream is) throws IOException {
		return safeGetContentAsString(is,UTF8);
	}

	public static String stripLeadingAndTrailingQuotes(String str) {
		if (str.startsWith("\"")) {
			str = str.substring(1, str.length());
		}
		if (str.endsWith("\"")) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	private void getText(String identifier, Part p) throws MessagingException, IOException {
		String charset = getCharset(p.getContentType());
		log.fine("Loading part using charset "+charset);

		if (p.isMimeType("text/html")) {
			log.fine("html body "+p.getContent().toString());
			log.fine("html body "+p.getContent());
			addHeader(HTML_BODY, convertCidLinks(identifier,javax.mail.internet.MimeUtility.decodeText(p.getContent().toString())));
			return;
		}
		if (p.isMimeType("text/plain")) {
			log.fine("text plain");
			addHeader(TEXT_BODY, javax.mail.internet.MimeUtility.decodeText(p.getContent().toString()));
			return;
		}
		if (p.isMimeType("multipart/alternative")) {
			log.fine("multipart/alternative");
		
			Object content = p.getContent();
			// prefer html text over plain text
			Multipart mp = (Multipart)content;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/*")) {
					getText(identifier,bp);
				} 
				else {
					getText(identifier,bp);
				}
			}
		} 
		else {
			if (p.isMimeType("multipart/*")) {
				log.fine("multipart/*");
				Multipart mp = (Multipart)p.getContent();
				for (int i = 0; i < mp.getCount(); i++) {
					getText(identifier,mp.getBodyPart(i));
				}
			}
			else {
				if (Part.ATTACHMENT.equals(p.getDisposition())) {
					String attachmentName;
					if ((attachmentName=p.getFileName())==null) {
						attachmentName = "unnamed";
					}
					log.fine("Processing attachment:"+attachmentName);
					addHeader(ATTACHMENT,attachmentName);
				}
				else {
					log.fine("Attachment found with mime type :"+p.getContentType()+" and disposition:"+p.getDisposition());	
				}
			}
		}
	}


	private String convertCidLinks(String identifier, String htmlString) {
		try {
			return htmlString.replaceAll("cid:", "../"+JSONInlineHandler.JSON_ROOT+"/"+identifier+"/");
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return htmlString;
	}

	/* 
	 * Decode as per RFC 2047
	 */
	private String[] getDecodedHeader(String name) throws MessagingException {
		String[] result = getHeader(name);
		for (int i = 0; i < result.length;i++)
			result[i] = Utils.decodeQuotedPrintable(result[i]);
		return result;
	}

	private JSONObject combineHeaders(JSONObject json, String headerName) throws MessagingException, JSONException {
		String headers[] = getHeader(headerName);
		StringBuffer total = new StringBuffer();
		if (headers!=null) {
			for (int i = 0; i < headers.length; i++) {
				total.append(headers[i]);
			}
		}
		json.remove(headerName);
		json.put(headerName, total.toString());
		return json;
	}

	public BodyPart loadInlineAttachment(String cid) throws ParseException, MessagingException, IOException {
		if (isMultipart()) {
			for (int i = 0; i < getCount(); i++) {
				BodyPart bp = getBodyPart(i);
				BodyPart attachment;
				if ((attachment=loadBodyPart(bp,cid))!=null) {
					return attachment;
				}
			}
		}		
		else {
			log.fine("Was not multi-part");
		}
		return null;
	}

	public void writeInlineAttachment(BodyPart attachment, HttpServletResponse resp) throws ParseException, IOException, MessagingException {
		if (attachment!=null) {
			ContentType ct = new ContentType(attachment.getContentType());
			resp.setContentType(ct.getBaseType());
			OutputStream os = resp.getOutputStream();
			InputStream is = attachment.getInputStream();
			int byteRead;
			while((byteRead=is.read())>=0)
				os.write(byteRead);
			os.flush();
			is.close();
			return;
		}	
		log.severe("No inline attachement found");
		resp.sendError(404);
	}

	public void writeInlineAttachment(String cid, HttpServletResponse resp) throws ParseException, IOException, MessagingException {
		BodyPart attachment;
		if ((attachment=loadInlineAttachment(cid))!=null) {
			writeInlineAttachment(attachment, resp);
			return;
		}	
		log.severe("No inline attachement found with cid="+cid);
		resp.sendError(404);
	}

	private static BodyPart loadBodyPart(BodyPart bp, String cid) throws ParseException, MessagingException, IOException {
		if (bp instanceof MimeBodyPart) {
			MimeBodyPart mbp = (MimeBodyPart)bp;
			if (mbp.getContentID()!=null) {
				if (mbp.getContentID().contains(cid)) {
					ContentType ct = new ContentType(mbp.getContentType());
					log.fine("Found inline attachment "+mbp.getContentID()+" as "+ct.getBaseType());
					return bp;							
				}
				else {
					log.info("Not what we are looking for :"+mbp.getContentID()+" vs "+cid);
				}
			}
			else {
				if (mbp.getContentType().contains("multipart/")) {
					MimeMultipart mp = (MimeMultipart) mbp.getContent();
					for (int i = 0; i < mp.getCount(); i++) {
						BodyPart subBp = mp.getBodyPart(i);
						BodyPart is;
						if ((is = loadBodyPart(subBp, cid))!=null) {
							return is;
						}
					}
				}
				else {
					if (cid.equals(mbp.getFileName())) {
						return mbp;
					}
					else {
						log.fine("Ignoring, not an attachment filename="+mbp.getFileName()+" contentype="+mbp.getContentType()+" disposition="+mbp.getDisposition()+" cid="+cid);
					}
				}
			}
		}
		return null;
	}

	public InputStream getAttachment(int index, HttpServletResponse resp) throws ParseException, MessagingException, IOException {
		if (isMultipart()) {
			int count = 0;
			for (int i = 0; i < getCount(); i++) {
				BodyPart bp = getBodyPart(i);
				if ((bp.getDisposition()==null)||(bp.getDisposition().equals(Part.INLINE))) {
					// INLINE - ignore
				}
				else {
					if (count==index) {
						// set the correct mime type
						if (resp!=null) {
							ContentType ct = new ContentType(bp.getContentType());
							log.info("Setting mime type to "+ct.getBaseType());
							resp.setContentType(ct.getBaseType());
						}
						log.fine("Sending attachment "+bp.getFileName());
						InputStream is = bp.getInputStream();
						return is;
					}
					else {
						count++;
					}
				}
			}
		}	

		try {
			JSONArray ja = toJSON("xxx", Locale.getDefault()).getJSONArray(ATTACHMENT);
			return loadInlineAttachment(ja.getString(index)).getInputStream();
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
		log.severe("Failed to find referenced attachment");
		return null;
	}

	public void writeAttachment(int index, HttpServletResponse resp) throws ParseException, IOException, MessagingException {
		OutputStream os = resp.getOutputStream();
		InputStream is = getAttachment(index, resp);

		int byteRead;
		while((byteRead=is.read())>=0)
			os.write(byteRead);

		os.flush();
		return;
	}

	/* Hack method to get around a Javamail bug of Multipart not being casteable to MimeMultipart */
	private BodyPart getBodyPart(int i) {
		try {
			Method m = getContent().getClass().getDeclaredMethod("getBodyPart",new Class[]{Integer.TYPE});
			return (BodyPart) m.invoke(getContent(), new Object[]{new Integer(i)});
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	/* Hack method to get around a Javamail bug of Multipart not being casteable to MimeMultipart */
	private int getCount() {
		try {
			Method m = getContent().getClass().getDeclaredMethod("getCount");
			return (Integer) m.invoke(getContent());
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return 0;
	}

	/* Hack method to get around a Javamail bug of Multipart not being casteable to MimeMultipart */
	private boolean isMultipart() {
		try {
			return getContent().getClass().getDeclaredMethod("getBodyPart",String.class)!=null;
		} 
		catch (Throwable e) {
			return false;
		}
	}
}
