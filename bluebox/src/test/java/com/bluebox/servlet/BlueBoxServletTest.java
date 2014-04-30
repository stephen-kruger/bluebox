package com.bluebox.servlet;

import java.io.IOException;

import javax.mail.SendFailedException;
import javax.mail.internet.InternetAddress;

import org.junit.Test;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;

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
			Utils.sendMessage(new InternetAddress("test@here.com"), 
					"subject", "body", 
					new InternetAddress[]{new InternetAddress("steve@blackdomain.com")}, 
					new InternetAddress[]{}, 
					new InternetAddress[]{}, false);
			fail("Mail to not blacklisted");
		} 
		catch (SendFailedException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail to not blacklisted");
		} 

		Inbox inbox = Inbox.getInstance();
		inbox.addToBlacklist("qwerty.com");

		try {
			Utils.sendMessage(new InternetAddress("test@here.com"), 
					"subject", "body", 
					new InternetAddress[]{new InternetAddress("steve@qwerty.com")}, 
					new InternetAddress[]{}, 
					new InternetAddress[]{}, false);
			fail("Mail to not blacklisted");
		} 
		catch (SendFailedException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail to not blacklisted");
		}

	}

	@Test
	public void testBlacklistFrom() throws IOException, Exception {
		try {
			Utils.sendMessage(new InternetAddress("test@here.com"), 
					"subject", "body", 
					new InternetAddress[]{new InternetAddress("steve@blackdomain.com")}, 
					new InternetAddress[]{}, 
					new InternetAddress[]{}, false);
			fail("Mail to not blacklisted");
		} 
		catch (SendFailedException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail to not blacklisted");
		} 

		Inbox inbox = Inbox.getInstance();
		inbox.addFromBlacklist("qwerty.com");

		try {
			Utils.sendMessage(new InternetAddress("test@qwerty.com"), 
					"subject", "body", 
					new InternetAddress[]{new InternetAddress("steve@here.com")}, 
					new InternetAddress[]{}, 
					new InternetAddress[]{}, false);
			fail("Mail from not blacklisted");
		} 
		catch (SendFailedException e) {
			// expected
		}
		catch (Exception e) {
			fail("Mail from not blacklisted");
		}
	}

	@Test
	public void testToWhitelist() throws IOException, Exception {

		Inbox inbox = Inbox.getInstance();
		inbox.addToWhiteList("qwerty.com");
		try {
			Utils.sendMessage(new InternetAddress("test@here.com"), 
					"subject", "body", 
					new InternetAddress[]{new InternetAddress("steve@here.com")}, 
					new InternetAddress[]{}, 
					new InternetAddress[]{}, false);
			fail("Mail to not whitelisted");
		} 
		catch (SendFailedException e) {
			// expected
		}
		catch (Exception e) {
			fail("accepted Mail to not whitelisted");
		} 

		Utils.sendMessage(new InternetAddress("test@qwerty.com"), 
				"subject", "body", 
				new InternetAddress[]{new InternetAddress("steve@qwerty.com")}, 
				new InternetAddress[]{}, 
				new InternetAddress[]{}, false);

	}


}
