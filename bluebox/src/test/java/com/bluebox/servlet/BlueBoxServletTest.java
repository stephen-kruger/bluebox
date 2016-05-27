package com.bluebox.servlet;

import java.io.IOException;

import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.EmailException;
import org.junit.Test;

import com.bluebox.TestUtils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;

public class BlueBoxServletTest extends BaseServletTest {

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	@Test
	public void testBlacklistTo() {
		try {
			TestUtils.sendMailSMTP(new InternetAddress("test@here.com"), new InternetAddress("steve@blackdomain.com"), null, null, "subject", "body");
			fail("Mail to not blacklisted");
		} 
		catch (EmailException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail to not blacklisted");
		} 

		Inbox inbox = Inbox.getInstance();
		inbox.addToBlacklist("qwerty.com");

		try {
			TestUtils.sendMailSMTP(new InternetAddress("test@here.com"), new InternetAddress("steve@qwerty.com"), null, null, "subject", "body");

			fail("Mail to not blacklisted");
		} 
		catch (EmailException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail to not blacklisted");
		}

	}

	@Test
	public void testBlacklistFrom() throws IOException, Exception {
		Inbox inbox = Inbox.getInstance();
		inbox.addFromBlacklist("qwerty.com");
		assertTrue(inbox.isFromBlackListed(new InboxAddress("test@qwerty.com")));
		assertFalse(inbox.isToBlackListed(new InboxAddress("test@qwerty.com")));
		assertFalse(inbox.accept("from@qwerty.com", "to@here.com"));
		assertTrue(inbox.accept("from@test.com", "to@qwerty.com"));
	}

	@Test
	public void testToWhitelist() throws IOException, Exception {
		Inbox inbox = Inbox.getInstance();
		inbox.addToWhiteList("qwerty.com");
		assertTrue(inbox.isToWhiteListed(new InboxAddress("test@qwerty.com")));
		assertFalse(inbox.isFromWhiteListed(new InboxAddress("test@qwerty.com")));
		assertFalse(inbox.accept("test@here.com", "steve@here.com"));
		assertTrue(inbox.accept("test@qwerty.com", "steve@qwerty.com"));
	}


}
