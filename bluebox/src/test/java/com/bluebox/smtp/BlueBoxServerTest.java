package com.bluebox.smtp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.bluebox.Config;
import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;

public class BlueBoxServerTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private SMTPServer smtpServer;
	private Config config = Config.getInstance();

	public BlueBoxServerTest(String s) {
		super(s);
	}

	protected void setUp() throws Exception {
		super.setUp();
		smtpServer = new BlueBoxSMTPServer(new SimpleMessageListenerAdapter(Inbox.getInstance()));
		smtpServer.start();
		log.fine("Test setUp");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		smtpServer.stop();
		Inbox.getInstance().deleteAll();
		Inbox.getInstance().stop();
	}

	public void testCrash() throws Exception {
		assertEquals("Mailbox was not cleared",0,Inbox.getInstance().getMailCount(State.ANY));
		InputStream emlStream = new FileInputStream("src/test/resources"+File.separator+"test-data"+File.separator+"crashfix.eml");
		Utils.uploadEML(emlStream);
		Utils.waitFor(1);
		assertEquals("Mail was not delivered",1,Inbox.getInstance().getMailCount(State.ANY));
	}

	public void testRaw() throws UnknownHostException, IOException {
		String host = smtpServer.getHostName();
		int port = smtpServer.getPort();
		Socket s = new Socket(host,port);
		BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
		BufferedWriter output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		//input.readLine();
		// S: 220 smtp.example.com ESMTP Postfix
		log.info("Opening smtp relay on host "+host+" port "+port);
		output.write("HELO relay.example.org\n");output.flush();
		safeRead(input);
		// S: 250 Hello relay.example.org, I am glad to meet you
		output.write("MAIL FROM:<bob@example.org>\n");output.flush();
		safeRead(input);
		// S: 250 Ok
		output.write("RCPT TO:<alice@example.com>\n");output.flush();
		safeRead(input);
		// S: 250 Ok
		output.write("RCPT TO:<theboss@example.com>\n");output.flush();
		safeRead(input);
		// S: 250 Ok
		output.write("DATA\n");output.flush();
		safeRead(input);
		// S: 354 End data with <CR><LF>.<CR><LF>
		output.write("From: \"Bob Example\" <bob@example.org>\n");
		output.write("To: \"Alice Example\" <alice@example.com>\n");
		output.write("Cc: theboss@example.com\n");
		output.write("Date: Tue, 15 Jan 2008 16:02:43 -0500\n");
		output.write("Subject: Test message\n");
		output.write("Hello Alice.\n");
		output.write("This is a test message with 5 header fields and 4 lines in the message body.\n");
		output.write("Your friend,\n");
		output.write("Bob\n");
		output.write("\r\n.\r\n");output.flush();
		safeRead(input);
		// S: 250 Ok: queued as 12345
		output.write("QUIT\n");output.flush();
		safeRead(input);
		// S: 221 Bye
		output.flush();
		safeRead(input);
		log.info("Closing smtp relay");
		output.close();
		s.close();
	}

	private void safeRead(BufferedReader input) throws IOException {
		if (input.ready()) {
			log.info(input.readLine());
		}
		else {
			log.info("No data!");
		}
	}

	public void testSendWithBadChars() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String testBody = "testSend Test Body";
		String testSubject = "testSend Test Subject";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress().toString(), 
					new InternetAddress("suresh%hserus.net@here.com").toString(),
					null,
					null,
					testSubject, 
					testBody);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		Utils.waitFor(1);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 1)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		assertEquals("Did not find expected results",1,list.size());
		BlueboxMessage email = (BlueboxMessage) list.get(0);
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals(testSubject));
	}

	public void testSend() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String testBody = "testSend Test Body";
		String testSubject = "testSend Test Subject";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), 
					Utils.getRandomAddress(),
					Utils.getRandomAddress(),
					Utils.getRandomAddress(),
					testSubject, 
					testBody);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}

		Utils.waitFor(3);

		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 3);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		assertEquals("Did not find expected results",3,list.size());
		BlueboxMessage email = (BlueboxMessage) list.get(0);
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals(testSubject));
	}

	public void testBlackList() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String testBody = "testSend Test Body";
		String testSubject = "testSend Test Subject";
		try {
			TestUtils.sendMailSMTP(new InternetAddress("badboy@blackdomain.com"), Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), testSubject, testBody);
			fail("The mail should have thrown an exception");
		} 
		catch (Exception e) {
			// we expect this to fail
		}
		assertTrue("Message should  not have been delivered (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 0)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 0);

		try {
			TestUtils.sendMailSMTP(new InternetAddress("test@example.com"),Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), testSubject, testBody);
			fail("The mail should have thrown an exception");
		} 
		catch (Exception e) {
			// we expect this to fail
		}
		assertTrue("Message should  not have been delivered (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 0)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 0);
	}

	public void testToWhiteList() throws Exception {
		Inbox inbox = Inbox.getInstance();
		assertTrue("No whitelist defined, should be accepted",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		inbox.addToWhiteList("gooddomain.com");
		assertFalse("Recipient was not on whitelist, should be refused",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		assertTrue("Recipient was on whitelist, should be accepted",inbox.accept("sender@nowhere.com","goodboy@gooddomain.com"));
	}

	public void testFromWhiteList() throws Exception {
		Inbox inbox = Inbox.getInstance();
		assertTrue("No whitelist defined, should be accepted",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		inbox.addFromWhiteList("gooddomain.com");
		assertFalse("Sender was not on whitelist, should be refused",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		assertTrue("Sender was on whitelist, should be accepted",inbox.accept("goodboy@gooddomain.com","sender@nowhere.com"));
	}



	public void xtestSendMessageWithCarriageReturn() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String bodyWithCR = "\nKeep these pesky\n carriage returns\n";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null,"Test", bodyWithCR);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		Utils.waitFor(1);
		assertTrue("Sent message was not correctly recieved (Got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+")",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		DataHandler dh = mimeMessage.getDataHandler();
		assertEquals("Body did not match",bodyWithCR,dh.getContent().toString());
	}

	// not sure this is a valid test as we never see the receiver@there.com or webhiker@test.com
	public void testSendTwoMsgsWithLogin() throws Exception {
		Inbox inbox = Inbox.getInstance();
		try {
			String server = Config.getInstance().getString("bluebox_host");
			String port = Config.getInstance().getString("bluebox_port");
			String From = "sender@here.com";
			String Subject = "Test";
			String body = "Test Body";

			Properties props = System.getProperties();

			if (server != null) {
				props.put("mail.smtp.host", server);
				props.put("mail.smtp.port", port);
			}

			Session session = Session.getDefaultInstance(props, null);
			Message msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(From));

			//InternetAddress.parse(To, false);
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("ignoreme@nowhere.com", false));
			msg.setSubject(Subject);

			msg.setText(body);
			msg.setHeader("X-Mailer", "musala");
			msg.setSentDate(new Date());
			msg.saveChanges();

			Transport transport = null;

			try {
				transport = session.getTransport("smtp");
				transport.connect(server, config.getInt(Config.BLUEBOX_PORT), "username", "password");
				transport.sendMessage(msg, InternetAddress.parse("receiver@there.com", false));
				transport.sendMessage(msg, InternetAddress.parse("webhiker@test.com", false));

			} 
			catch (javax.mail.MessagingException me) {
				me.printStackTrace();
			} 
			catch (Exception e) {
				e.printStackTrace();
			} 
			finally {
				if (transport != null) {
					transport.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Utils.waitFor(2);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 2)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 2);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals("Test"));
		//		assertTrue(email.getContent().toString().equals("Test Body"));
	}

	public void testSendToBCCAndCC() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String testBody = "testSend Test Body";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), "Test", testBody);

		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		Utils.waitFor(3);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 3);
	}

	public void testBCC() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String testBody = "testSend Test Body";
		try {
			log.info("Sending BCC only message");
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), null, null, Utils.getRandomAddress(), "Test", testBody);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		log.info("Waiting for message to get delivered");
		Utils.waitFor(1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		assertTrue("Did not find the expected message",emailIter.hasNext());
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertNull("No To recipient was expected",mimeMessage.getRecipients(RecipientType.TO));
		assertNull("No CC recipient was expected",mimeMessage.getRecipients(RecipientType.CC));
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
	}

	public void testSubjectEncoding() throws Exception {
		Inbox inbox = Inbox.getInstance();
		String chineseStr = "æ¥·ä¹¦ï¼�æ¥·æ›¸";
		TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null, chineseStr, "This is the body");
		Utils.waitFor(1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		assertTrue("Did not find the expected message",emailIter.hasNext());
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertEquals("The subject was not correctly encoded or decoded",chineseStr, mimeMessage.getSubject());
	}

//	private Properties getMailProperties() {
//		Properties mailProps = new Properties();
//		mailProps.setProperty("mail.smtp.host", Utils.getHostName());
//		mailProps.setProperty("mail.smtp.port", "" + config.getString(Config.BLUEBOX_PORT));
//		mailProps.setProperty("mail.smtp.sendpartial", "true");
//		return mailProps;
//	}


	public void testSendSMTP() throws Exception {
		String subject = "My country is dying";
		String bodyWithCR = "\nKeep these pesky\n carriage returns\n";
		TestUtils.sendMailSMTP("steve@here.com", "bob@zim.com", null, null, subject, bodyWithCR);

		Utils.waitFor(1);


		Inbox inbox = Inbox.getInstance();
		assertEquals("Sent message was not correctly recieved (Got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+")",1,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
//		DataHandler dh = mimeMessage.getDataHandler();
		assertEquals("Subject did not match",subject,mimeMessage.getSubject().toString());
		// TODO - figure out why this does not work
//		assertEquals("Body did not match",bodyWithCR.length(),email.getText().length());
//		assertEquals("Body did not match",bodyWithCR,email.getText());
	}

}
