package com.bluebox.smtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import com.bluebox.MimeMessageParser;
import com.bluebox.Utils;

public class SpamTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Override
	public void setUp() {
	}

	@Override
	protected void tearDown() throws Exception {
	}

	public void testFromVsSender() throws Exception {
		log.info("Checking if spam delivery path is detected");
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/spam.eml"));
		MimeMessage message = Utils.loadEML(is);
		MimeMessageParser p = new MimeMessageParser(message);
		p.parse();
		assertTrue("The sender should be rejected in the deliver phase",Inbox.getInstance().isReceivedBlackListedDomain(message));
	}

	
}
