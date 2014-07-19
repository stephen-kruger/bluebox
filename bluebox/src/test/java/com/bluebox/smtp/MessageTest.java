package com.bluebox.smtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;



import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.util.MimeMessageUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.MimeMessageParser;
import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;
import com.bluebox.smtp.storage.BlueboxMessage;

public class MessageTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Override
	public void setUp() {
	}

	@Override
	protected void tearDown() throws Exception {
	}

	public void testProperties() throws AddressException, MessagingException, IOException {
		log.info("Doing nothing");
		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				"subjStr",
				"bodyStr", 
				false);
		message.addHeader("name", "value");
		assertEquals("Header was not set","value",message.getHeader("name")[0]);
		message.addHeader("name", "value2");
		assertEquals("Header was not set","value2",message.getHeader("name")[1]);
		message.addHeader("name", "value");
		assertTrue("Header was missing values",message.getHeader("name").length==3);
	}

	public void testBlueBoxMessage() throws AddressException, MessagingException, IOException {
		String bodyStr = "\nThis is a body\n with line feeds";
		String subjStr = "This is the subject";
		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				subjStr,
				bodyStr, 
				false);

		assertEquals("Body did not get stored and retrieved correctly",bodyStr,message.getContent());
		message.addHeader("test", "value");
		message.addHeader("test", "value");
		assertEquals("Header value was not correctly set",message.getHeader("test")[0],"value");
		assertEquals("Header value was not correctly set",message.getHeader("test")[1],"value");

	}

	public void testJSON() throws AddressException, MessagingException, IOException, JSONException {
		String bodyStr = "\nThis is a body\n with line feeds";
		String subjStr = "This is the subject";
		//		String uidStr = "1234567890";

		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				subjStr,
				bodyStr, 
				false);

		JSONObject json = new MimeMessageWrapper(message).toJSON("xx", Locale.ENGLISH);
		log.info(json.toString(3));
	}

	public void testAttachmentParsing() throws IOException, MessagingException {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/m0017.txt"));
		MimeMessageWrapper message = new MimeMessageWrapper(Utils.loadEML(is));
		assertNotNull("Attachment not loaded correctly",message.loadInlineAttachment("938014623@17052000-0f9b"));
	}

	public void testBodyEncoding() throws IOException, MessagingException, JSONException {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/body_encoding_issue.eml"));
		MimeMessageWrapper message = new MimeMessageWrapper(Utils.loadEML(is));
		message.toJSON("12345", Locale.getDefault()).getString(MimeMessageWrapper.TEXT_BODY);
	}

	public void testInlineAttachmentParsing() throws IOException, MessagingException, JSONException {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/inlineattachments.eml"));
		MimeMessageWrapper message = new MimeMessageWrapper(Utils.loadEML(is));

		BodyPart bp = message.loadInlineAttachment("ISM Open Tickets Report - 03-13-2012-DOW.zip");
		assertNotNull("Attachment not loaded correctly",bp);

		InputStream attachmentStream = message.getAttachment(0, null);
		assertNotNull("Attachment not loaded correctly",attachmentStream);
		attachmentStream.close();
	}

	public void testCharsetDetection() {
		String contentType = "text/html; charset=GB2312";
		assertEquals("Incorrect charset detected","GB2312",MimeMessageWrapper.getCharset(contentType));
	}

	public void testNoRecipient() throws MessagingException, IOException {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/no-recipient.eml"));
		MimeMessageWrapper message = new MimeMessageWrapper(Utils.loadEML(is));
		is.close();

		InternetAddress add = BlueboxMessage.getRecipient(new InboxAddress("xxx@xxx.com"), message);
		assertEquals("Should not have any recipient fields",add.getAddress(),"xxx@xxx.com");
	}

	public void testPersistAndLoad() throws MessagingException, IOException, JSONException {
		MimeMessageWrapper mmw = TestUtils.createBlueBoxMimeMessage(null, 
				new InternetAddress("sender@test.com"), 
				new InternetAddress[]{new InternetAddress("user@recipient.com")}, 
				new InternetAddress[]{}, 
				new InternetAddress[]{}, 
				"subject", "body", false);
		MimeMessageWrapper fromStream = new MimeMessageWrapper(null,mmw.getInputStream());
		assertEquals("Mismatched subject",mmw.getSubject(),fromStream.getSubject());
		assertEquals("Mismatched sender",mmw.getFrom()[0].toString(),fromStream.getFrom()[0].toString());
		assertEquals("Mismatched body",mmw.getContent().toString(),fromStream.getContent().toString());
	}

	public List<String> listCids(MimeMessage mm) throws MessagingException {
		List<String> cids = new ArrayList<String>();
		MimeMultipart mmp = new MimeMultipart(mm.getDataHandler().getDataSource());
		System.out.println(mm.getContentID());
		for (int i = 0; i < mmp.getCount(); i++) {
			BodyPart bp = mmp.getBodyPart(i);
			if (bp instanceof MimeBodyPart) {
				MimeBodyPart mbp = (MimeBodyPart)bp;
				System.out.println("1:"+mbp.getContentID());
				System.out.println("2:"+mbp.getFileName());
				System.out.println("3:"+mbp.getDisposition());
			}
		}
		return cids;
	}

	public void testCommonsAttachmentParsing() throws Exception {
		MimeMessage mm = MimeMessageUtils.createMimeMessage(null, new File("src/test/resources/test-data/attachments.eml"));
		MimeMessageParser parser = new MimeMessageParser(mm);
		parser.parse();
		for (String cid : parser.getCids()) {
			DataSource ds = parser.getDataSourceForCid(cid);
			assertNotNull(ds);
			System.out.println(ds.getName()+">>>>>>>>>>"+cid);	
		}
		//		listCids(mm);
		//		String html = parser.getHtmlContent();
		//		System.out.println(BlueboxMessage.convertCidLinks("ii_hxqkskb21_147462ce25a92ebf", html));
		//		System.out.println(mm.getClass().getName());
		//		List<DataSource> attachments = parser.getAttachmentList();
		//		for (DataSource ds : attachments) {
		//			System.out.println(ds.getClass().getName()+">>>>>"+ds.getName()+" "+ds.getContentType());
		//			//			html = convertCidLinks(ds.getName(),html);
		//		}	
		//		System.out.println(parser.findAttachmentByName("cid:ii_hxqkskb21_147462ce25a92ebf"));

	}
}
