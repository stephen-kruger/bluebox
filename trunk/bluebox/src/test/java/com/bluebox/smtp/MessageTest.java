package com.bluebox.smtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.util.MimeMessageUtils;

import com.bluebox.MimeMessageParser;
import com.bluebox.Utils;
import com.bluebox.smtp.storage.BlueboxMessage;

public class MessageTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Override
	public void setUp() {
	}

	@Override
	protected void tearDown() throws Exception {
	}

	public void testProperties() throws AddressException, MessagingException, IOException, EmailException {
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

	public void testBlueBoxMessage() throws AddressException, MessagingException, IOException, EmailException {
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

	public void testAttachmentParsing() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/m0017.txt"));
		MimeMessage message = Utils.loadEML(is);
		MimeMessageParser p = new MimeMessageParser(message);
		p.parse();
		assertNotNull("Attachment not loaded correctly",p.getDataSourceForCid("938014623@17052000-0f9b"));
	}

	//	public void testInlineAttachmentParsing() throws IOException, MessagingException, JSONException {
	//		InputStream is = new FileInputStream(new File("src/test/resources/test-data/inlineattachments.eml"));
	//		MimeMessage message = Utils.loadEML(is);
	//		BlueboxMessage bbm = new BlueboxMessage("svxcvkkxcvsxl");
	//		bbm.setBlueBoxMimeMessage("test@here.com", bbmm);
	//		bbm.
	//		BodyPart bp = message.loadInlineAttachment("ISM Open Tickets Report - 03-13-2012-DOW.zip");
	//		assertNotNull("Attachment not loaded correctly",bp);
	//
	//		InputStream attachmentStream = message.getAttachment(0, null);
	//		assertNotNull("Attachment not loaded correctly",attachmentStream);
	//		attachmentStream.close();
	//	}

	//	public void testCharsetDetection() {
	//		String contentType = "text/html; charset=GB2312";
	//		assertEquals("Incorrect charset detected","GB2312",MimeMessageWrapper.getCharset(contentType));
	//	}

	public void testNoRecipient() throws MessagingException, IOException {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/no-recipient.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();

		InternetAddress add = BlueboxMessage.getRecipient(new InboxAddress("xxx@xxx.com"), message);
		assertEquals("Should not have any recipient fields",add.getAddress(),"xxx@xxx.com");
	}
	
	public void testEncodedContent() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/bodybreaker.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();
		MimeMessageParser parser = new MimeMessageParser(message);
		parser.parse();
		log.info(message.getSubject());
		log.info(parser.getSubject());
//		log.info(parser.getPlainContent());
//		log.info(parser.getHtmlContent());
	}

	public List<String> listCids(MimeMessage mm) throws MessagingException {
		List<String> cids = new ArrayList<String>();
		MimeMultipart mmp = new MimeMultipart(mm.getDataHandler().getDataSource());
		log.info(mm.getContentID());
		for (int i = 0; i < mmp.getCount(); i++) {
			BodyPart bp = mmp.getBodyPart(i);
			if (bp instanceof MimeBodyPart) {
				MimeBodyPart mbp = (MimeBodyPart)bp;
				log.info("1:"+mbp.getContentID());
				log.info("2:"+mbp.getFileName());
				log.info("3:"+mbp.getDisposition());
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
			log.info(ds.getName()+">>>>>>>>>>"+cid);	
		}
		//		listCids(mm);
		//		String html = parser.getHtmlContent();
		//		log.info(BlueboxMessage.convertCidLinks("ii_hxqkskb21_147462ce25a92ebf", html));
		//		log.info(mm.getClass().getName());
		//		List<DataSource> attachments = parser.getAttachmentList();
		//		for (DataSource ds : attachments) {
		//			log.info(ds.getClass().getName()+">>>>>"+ds.getName()+" "+ds.getContentType());
		//			//			html = convertCidLinks(ds.getName(),html);
		//		}	
		//		log.info(parser.findAttachmentByName("cid:ii_hxqkskb21_147462ce25a92ebf"));

	}
	
	public void testScratch() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/nobodyshown.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();
		MimeMessageParser parser = new MimeMessageParser(message);
		parser.parse();
		log.info(parser.getPlainContent());
	}
}
