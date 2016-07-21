package com.bluebox.smtp.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;

import junit.framework.TestCase;

public class BlueboxParserTest extends TestCase {
	private static final Logger log = LoggerFactory.getLogger(BlueboxMessage.class);

	@Test
	public void testBody() throws Exception {
		log.info("Test html parsing");
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/community.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();

		BlueBoxParser parser = new BlueBoxParser(message);
		assertNotNull("Could not retrieve html part",parser.getHtmlContent());
	}
	
	@Test
	public void testInline() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/inline-example.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();

		BlueBoxParser parser = new BlueBoxParser(message);
		assertNotNull("Could not retrieve inline attachment",parser.getInlineAttachments().get("BannerImg@banner.XXX.com"));
	}
	
	@Test
	public void testInline2() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/attachments.eml"));
		MimeMessage message =Utils.loadEML(is);
		is.close();

		BlueBoxParser parser = new BlueBoxParser(message);
		assertNotNull("Could not retrieve inline attachment",parser.findAttachmentByCid("ii_hxqkskb21_147462ce25a92ebf"));
		assertNotNull("Could not retrieve inline attachment",parser.findAttachmentByName("cv-template-Marketing-Manager.doc"));
		assertNotNull("Could not retrieve inline attachment",parser.findAttachmentByName("DSC_3968.JPG"));
	}

}
