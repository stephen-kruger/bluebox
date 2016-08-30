package com.bluebox.smtp.storage;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;

/**
 * The Class BlueBoxParser is one big hack for the fact that on Liberty we get a mix mess of mail classes.
 * Sometimes it's MimeMultipart sometimes it's Multipart
 * Probably a classpath problem, but behaviour is completely inconsistent between RHEL and Windows.
 * Hack it by using introspection, as both classes have the same methods.
.
 */
public class BlueBoxParser {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxParser.class);

	private MimeMessage msg;
	private Map<String,DataHandler> cidList, fileNameList;
	private List<Object> parts;

	public BlueBoxParser(MimeMessage msg) throws IOException, MessagingException {
		this.msg = msg;
		cidList = new HashMap<String,DataHandler>();
		fileNameList = new HashMap<String,DataHandler>();
		parts = getPartsX(msg.getContent());
	}

	private List<Object> getPartsX(Object content) throws IOException, MessagingException {
		List<Object> parts = new ArrayList<Object>();
		//		Object content = msg.getContent();
		int count = (Integer)invoke(content,"getCount",null,null,0);
		log.debug("Found {} parts",count);
		for (int i = 0; i < count; i++) {
			Object part = invoke(content,"getBodyPart", new Class[]{int.class}, new Object[]{i},null);
			parts.add(part);
			String cid = stripContentId((String)invoke(part,"getContentID", new Class[]{}, new Object[]{},""));
			if ((cid!=null)&&(cid.length()>0)) {
				log.debug("Found attachment with cid={}",cid);
				cidList.put(cid, (DataHandler)invoke(part,"getDataHandler",null,null,null));
			}
			String fileName = (String)invoke(part,"getFileName", new Class[]{}, new Object[]{},"");
			if ((fileName!=null)&&(fileName.length()>0)) {
				log.debug("Found attachment with fileName={}",fileName);
				fileNameList.put(fileName, (DataHandler)invoke(part,"getDataHandler",null,null,null));
			}
			Object subPart = invoke(part,"getContent",null,null,null);
			List<Object> sl = getPartsX(subPart);
			parts.addAll(sl);
		}
		return parts;
	}

	public Map<String,DataHandler> getInlineAttachments() {
		return cidList;
	}

	public List<DataSource> getAttachmentList() throws IOException, MessagingException {
		List<DataSource> attachments = new ArrayList<DataSource>();
		for (Object part : parts) {
			if (Part.ATTACHMENT.equalsIgnoreCase((String)invoke(part,"getDisposition",null,null,null))) {
				DataHandler dh = (DataHandler)invoke(part,"getDataHandler",null,null,null);
				attachments.add(dh.getDataSource());
			}
		}
		return attachments;
	}

	public String getPlainContent() throws MessagingException, IOException {
		log.debug("getPlainContent getContentType={}",msg.getContentType());
		if (msg.isMimeType(MediaType.TEXT_PLAIN)) {
			String encoding = msg.getEncoding();
			if (encoding==null) encoding = "";
			if (encoding.toLowerCase().indexOf("quoted")>=0)
				return Utils.decodeQuotedPrintable(msg.getContent().toString());
			else
				if (encoding.toLowerCase().indexOf("64")>=0) {
					byte[] bytes = msg.getContent().toString().getBytes();
					byte[] bytes64 = Base64.encodeBase64(bytes);
					return Utils.decodeQuotedPrintable(new String(bytes64));
				}
				else {
					return msg.getContent().toString();
				}
		}
		else {
			for (Object part : parts) {
				if ((Boolean)invoke(part,"isMimeType",new Class[]{String.class},new Object[]{MediaType.TEXT_PLAIN},null)) {
					return decodeString((DataHandler)invoke(part,"getDataHandler",null,null,null),StandardCharsets.UTF_8.name());
				}
				else {
					log.debug("getPlainContent:Ignoring part with mime type {}",invoke(part,"getContentType",null,null,null));
				}
			}
		}
		log.debug("No text body found");
		return null;
	}

	public String getHtmlContent() throws IOException, MessagingException {
		log.debug("Looking for html body {}",msg.getContentType());
		if (msg.isMimeType(MediaType.TEXT_PLAIN)) {
			return null;
		}
		else {
			if (msg.isMimeType(MediaType.TEXT_HTML)) {
				log.debug("getHtmlContent returning entire body");
				return decodeString(msg.getDataHandler(),StandardCharsets.UTF_8.name());
			}			
			for (Object part : parts) {
				if ((Boolean)invoke(part,"isMimeType",new Class[]{String.class},new Object[]{MediaType.TEXT_HTML},null)) {
					log.debug("Found html part");
					return decodeString((DataHandler)invoke(part,"getDataHandler",null,null,null),StandardCharsets.UTF_8.name());
				}
				else {
					log.debug("getHtmlContent ignoring part with mime type {} {}",invoke(part,"getContentType",null,null,null),part.getClass().getName());
				}
			}
		}
		log.debug("No html body found");
		return null;
	}

	public Object invoke(Object obj, String methodName, Class<?>[] sig, Object[] params, Object defaultResult) {
		try {
			Method method = obj.getClass().getMethod(methodName, sig);
			return method.invoke(obj, params);
		}
		catch (Throwable t) {
			log.debug("No method {} found on object {}",methodName,obj.getClass().getName());
			return defaultResult;
		}
	}

	private static String decodeString(DataHandler dataHandler, String encoding) throws IOException, MessagingException {
		if (encoding.indexOf("64")>=0) {
			log.debug("Decoding Base64 {}",encoding);
			byte[] bytes = IOUtils.toByteArray(dataHandler.getInputStream());
			byte[] bytes64 = Base64.encodeBase64(bytes);
			return Utils.decodeQuotedPrintable(new String(bytes64));
		}
		else {
			log.debug("Decoding {}",encoding);
			return Utils.decodeQuotedPrintable(IOUtils.toString(dataHandler.getInputStream(),getEncoding(encoding)));
		}
	}

	private static Charset getEncoding(String encoding) {
		try {
			return Charset.forName(encoding);
		}
		catch (Throwable t) {
			log.error("Invalid encoding specified :{}",encoding,t);
			return StandardCharsets.UTF_8;
		}
	}

	private String stripContentId(final String contentId) {
		if (contentId == null) {
			return null;
		}
		return contentId.trim().replaceAll("[\\<\\>]", "");
	}

	public DataHandler findAttachmentByCid(String cid) {
		// TODO Auto-generated method stub
		return cidList.get(cid);
	}

	public DataHandler findAttachmentByName(String name) throws IOException, MessagingException {
		for (DataSource ds : getAttachmentList()) {
			if (name.equalsIgnoreCase(ds.getName())) {
				return new DataHandler(ds);
			}
		}
		return fileNameList.get(name);
	}
}
