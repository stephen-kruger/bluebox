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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.bluebox.BaseTestCase;
import com.bluebox.Config;
import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils;
import com.bluebox.search.SolrIndexer;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.BlueboxMessage.State;

public class InboxTest extends BaseTestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Test
	public void testGlobalStats() throws AddressException, MessagingException, IOException, Exception {
		Inbox inbox = getInbox();
		inbox.setStatsGlobalCount(987654);
		assertEquals("Incorrectly reported global stats",987654,inbox.getStatsGlobalCount());
		inbox.setStatsGlobalCount(123456);
		assertEquals("Incorrectly reported global stats",123456,inbox.getStatsGlobalCount());
	}

	@Test
	public void testRecentStats() throws AddressException, MessagingException, IOException, Exception {
		Inbox inbox = getInbox();
		String email1 = "Steve1 <steve1@there.com>";
		String email2 = "Steve2 <steve2@there.com>";
		String email3 = "Steve3 <steve3@there.com>";
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email1), null, null, "subject", "body");
		log.info(inbox.getStatsRecent().toString(3));
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email2), null, null, "subject", "body");
		log.info(inbox.getStatsRecent().toString(3));
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email3), null, null, "subject", "body");
		log.info(inbox.getStatsRecent().toString(3));
		TestUtils.waitFor(getInbox(),3);
		JSONObject json = inbox.getStatsRecent();
		log.info(json.toString());
		assertEquals("Incorrectly reported recent stats recipient",new InboxAddress(email3).getAddress(),json.getString(BlueboxMessage.INBOX));
		assertEquals("Incorrectly reported recent stats sender","from@from.com",json.getString(BlueboxMessage.FROM));
		assertNotNull("Unset UID",json.getString(BlueboxMessage.UID));
	}

	@Test
	public void testActiveStats() throws AddressException, MessagingException, IOException, Exception {
		String email1 = "Steve1 <steve1@there.com>";
		String email2 = "Steve2 <steve2@there.com>";
		String email3 = "Steve3 <steve3@there.com>";
		String email4 = "Steve4 <steve4@there.com>";
		String email5 = "Steve5 <steve5@there.com>";
		for (int i = 0; i <10; i++) {
			TestUtils.sendMailSMTP(new InternetAddress(email1), new InternetAddress(email1), null, null, "subject", "body");
		}
		for (int i = 0; i <5; i++) {
			TestUtils.sendMailSMTP(new InternetAddress(email2), new InternetAddress(email2), null, null, "subject", "body");
		}
		for (int i = 0; i <2; i++) {
			TestUtils.sendMailSMTP(new InternetAddress(email3), new InternetAddress(email3), null, null, "subject", "body");
		}
		for (int i = 0; i <8; i++) {
			TestUtils.sendMailSMTP(new InternetAddress(email4), new InternetAddress(email4), null, null, "subject", "body");
		}
		for (int i = 0; i <6; i++) {
			TestUtils.sendMailSMTP(new InternetAddress(email5), new InternetAddress(email5), null, null, "subject", "body");
		}
		Inbox inbox = getInbox();
		inbox.updateStats(null, "", true);
		JSONObject jo = inbox.getStatsActiveInbox();
		assertEquals("Incorrectly reported most active inbox",new InboxAddress(email1).getFullAddress(),jo.getString(Inbox.EMAIL));

		assertEquals("Incorrectly reported most active recipient",new InboxAddress(email1).getDisplayName(),jo.getString(BlueboxMessage.RECIPIENT));
		assertEquals("Incorrectly reported most active inbox count",10,jo.getInt(BlueboxMessage.COUNT));

		jo = inbox.getStatsActiveSender();
		assertEquals("Incorrectly reported most active sender",new InboxAddress(email1).getAddress(),new InboxAddress(jo.getString(BlueboxMessage.FROM)).getAddress());
		assertEquals("Incorrectly reported most active sender count",10,jo.getInt(BlueboxMessage.COUNT));
	}

	@Test
	public void testBackup() throws Exception {
		getInbox().deleteAll();
		assertEquals(0,getInbox().getMailCount(BlueboxMessage.State.ANY));
		String email1 = "aaading@kkddf.com";
		String email2 = "aaading@kkddf.com";
		String email3 = "aaading@kkddf.com";
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email1), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email2), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email3), null, null, "subject", "body");
		assertEquals(3,getInbox().getMailCount(BlueboxMessage.State.ANY));
		File dir = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
		dir.mkdirs();
		WorkerThread wt = getInbox().backup(dir);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(250);
		getInbox().deleteAll();
		assertEquals(0,getInbox().getMailCount(BlueboxMessage.State.ANY));
		wt = getInbox().restore(dir);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(250);
		assertEquals(3,getInbox().getMailCount(BlueboxMessage.State.ANY));
	}

	@Test
	public void testSoftDelete() throws Exception {
		String email1 = "aaading@kkddf.com";
		String from="<from@from.com>";
		TestUtils.sendMailSMTP(new InternetAddress("user one "+from), new InternetAddress(email1), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("user two "+from), new InternetAddress(email1), null, null, "subject", "body");
		Inbox inbox = getInbox();
		assertEquals("Missing mail",2,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		List<BlueboxMessage> mail = inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.NORMAL, 0, 100, BlueboxMessage.SIZE, true);
		assertEquals("Missing mail",2,mail.size());
		SolrIndexer si = SolrIndexer.getInstance();
		si.commit(true);
		assertEquals("Missing search data",2,si.search("from@from.com", SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SearchFields.FROM, true).length);
		inbox.softDelete(mail.get(0).getIdentifier());

		assertEquals("Missing deleted mail",1,inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.DELETED, 0, 100, BlueboxMessage.SIZE, true).size());
		assertEquals("Did not find deleted mail",1,inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.NORMAL, 0, 100, BlueboxMessage.SIZE, true).size());
		assertEquals("Unexpected search data",1,si.search(from, SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SearchFields.FROM, true).length);		
	}

	@Test
	public void testCrash() throws Exception {
		assertEquals("Mailbox was not cleared",0,getInbox().getMailCount(State.ANY));
		InputStream emlStream = new FileInputStream("src/test/resources"+File.separator+"test-data"+File.separator+"crashfix.eml");
		Utils.uploadEML(getInbox(),emlStream);
		TestUtils.waitFor(getInbox(),1);
		assertEquals("Mail was not delivered",1,getInbox().getMailCount(State.ANY));
	}

	@Test
	public void testRaw() throws Exception {
		String smtp = "relay.junittest.org";
		String to1 = "<alice@junittest.com>";
		String to2 = "<theboss@junittest.com>";
		String from = "<bob@junittest.org>";
		assertEquals(0,getInbox().getMailCount(BlueboxMessage.State.ANY));

		String host = getSMTPServer().getHostName();
		int port = getSMTPServer().getPort();
		Socket s = new Socket(host,port);
		assertTrue(s.isConnected());
		BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
		BufferedWriter output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		//input.readLine();
		// S: 220 smtp.junittest.com ESMTP Postfix
		log.info("Opening smtp relay on host "+host+" port "+port);
		safeWrite(output,"HELO "+smtp);
		safeRead(input);
		// S: 250 Hello relay.junittest.org, I am glad to meet you
		safeWrite(output,"MAIL FROM:"+from);
		safeRead(input);
		// S: 250 Ok
		safeWrite(output,"RCPT TO:"+to1);
		safeRead(input);
		// S: 250 Ok
		safeWrite(output,"RCPT TO:"+to2);
		safeRead(input);
		// S: 250 Ok
		safeWrite(output,"DATA");
		safeRead(input);
		// S: 354 End data with <CR><LF>.<CR><LF>
		safeWrite(output,"From: "+from);
		safeWrite(output,"To: "+to1);
		safeWrite(output,"Cc: "+to2);
		safeWrite(output,"Date: Tue, 15 Jan 2008 16:02:43 -0500");
		safeWrite(output,"Subject: Test message");
		safeWrite(output,"Hello Alice.");
		safeWrite(output,"This is a test message with 5 header fields and 4 lines in the message body.");
		safeWrite(output,"Your friend,");
		safeWrite(output,"Bob");
		safeWrite(output,"\r\n.\r\n");

		safeRead(input);
		// S: 250 Ok: queued as 12345
		safeWrite(output,"QUIT");

		safeRead(input);
		// S: 221 Bye

		log.info("Closing smtp relay");
		output.close();
		s.close();

		TestUtils.waitFor(getInbox(), 1);

		// check the mail was received
		assertEquals(2,getInbox().getMailCount(BlueboxMessage.State.ANY));
	}

	private void safeRead(BufferedReader input) throws IOException, InterruptedException {
		int count=0;
		while((count++<10)&&(!input.ready())) {
			Thread.sleep(150);
		} 
		if (input.ready()) {
			String s = input.readLine();
			log.info(s);
		}
		else {
			log.severe("No data!");
		}
	}

	private void safeWrite(BufferedWriter output,String s) {
		try {
			output.write(s+"\r\n");
			output.flush();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSendWithBadChars() throws Exception {
		Inbox inbox = getInbox();
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
		TestUtils.waitFor(getInbox(),1);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 1)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		assertEquals("Did not find expected results",1,list.size());
		BlueboxMessage email = (BlueboxMessage) list.get(0);
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals(testSubject));
	}

	/*
	 * Not really a test, but verify the logs contain message indicating temp file was deleted.
	 */
	public void testFileHandlesLimit() throws Exception {
		String testBody = "testSend Test Body";
		String testSubject = "testSend Test Subject";
		for (int i = 0; i < 30; i++) {
			TestUtils.sendMailSMTP(Utils.getRandomAddress().toString(), 
					new InternetAddress("suresh%hserus.net@here.com").toString(),
					null,
					null,
					testSubject, 
					testBody);
		}
		TestUtils.waitFor(getInbox(), 30);
	}

	@Test
	public void testSend() throws Exception {
		Inbox inbox = getInbox();
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

		TestUtils.waitFor(getInbox(),3);

		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 3);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		assertEquals("Did not find expected results",3,list.size());
		BlueboxMessage email = (BlueboxMessage) list.get(0);
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals(testSubject));
	}

	@Test
	public void testBlackList() throws Exception {
		Inbox inbox = getInbox();
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
			TestUtils.sendMailSMTP(new InternetAddress("test@wallstreetads.org"),Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), testSubject, testBody);
			fail("The mail should have thrown an exception");
		} 
		catch (Exception e) {
			// we expect this to fail
		}
		assertTrue("Message should  not have been delivered (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 0)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 0);
	}

	@Test
	public void testToWhiteList() throws Exception {
		Inbox inbox = getInbox();
		assertTrue("No whitelist defined, should be accepted",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		inbox.addToWhiteList("gooddomain.com");
		assertFalse("Recipient was not on whitelist, should be refused",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		assertTrue("Recipient was on whitelist, should be accepted",inbox.accept("sender@nowhere.com","goodboy@gooddomain.com"));
	}

	@Test
	public void testFromWhiteList() throws Exception {
		Inbox inbox = getInbox();
		assertTrue("No whitelist defined, should be accepted",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		inbox.addFromWhiteList("gooddomain.com");
		assertFalse("Sender was not on whitelist, should be refused",inbox.accept("sender@nowhere.com", "badboy@notinvited.com"));
		assertTrue("Sender was on whitelist, should be accepted",inbox.accept("goodboy@gooddomain.com","sender@nowhere.com"));
	}

	@Test
	public void xtestSendMessageWithCarriageReturn() throws Exception {
		Inbox inbox = getInbox();
		String bodyWithCR = "\nKeep these pesky\n carriage returns\n";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null,"Test", bodyWithCR);
		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		TestUtils.waitFor(getInbox(),1);
		assertTrue("Sent message was not correctly recieved (Got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+")",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		DataHandler dh = mimeMessage.getDataHandler();
		assertEquals("Body did not match",bodyWithCR,dh.getContent().toString());
	}

	@Test
	public void testSendTwoMsgsWithLogin() throws Exception {
		Inbox inbox = getInbox();
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
			msg.setSentDate(StorageFactory.getInstance().getUTCTime());
			msg.saveChanges();

			Transport transport = null;

			try {
				transport = session.getTransport("smtp");
				transport.connect(server, Config.getInstance().getInt(Config.BLUEBOX_PORT), "username", "password");
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
		TestUtils.waitFor(getInbox(),2);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 2)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 2);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertTrue(mimeMessage.getSubject().equals("Test"));
		//		assertTrue(email.getContent().toString().equals("Test Body"));
	}

	@Test
	public void testSendToBCCAndCC() throws Exception {
		Inbox inbox = getInbox();
		String testBody = "testSend Test Body";
		try {
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), Utils.getRandomAddress(), "Test", testBody);

		} 
		catch (Exception e) {
			e.printStackTrace();
			fail("Unexpected exception: " + e);
		}
		TestUtils.waitFor(getInbox(),3);
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 3);
	}

	@Test
	public void testBCC() throws Exception {
		Inbox inbox = getInbox();
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
		TestUtils.waitFor(getInbox(),1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		assertTrue("Did not find the expected message",emailIter.hasNext());
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertNull("No To recipient was expected",mimeMessage.getRecipients(RecipientType.TO));
		assertNull("No CC recipient was expected",mimeMessage.getRecipients(RecipientType.CC));
		assertTrue("Did not find expected number of recieved emails (got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+" instead of 3)",inbox.getMailCount(BlueboxMessage.State.NORMAL) == 1);
	}

	@Test
	public void testSubjectEncoding() throws Exception {
		Inbox inbox = getInbox();
		String chineseStr = "æ¥·ä¹¦ï¼�æ¥·æ›¸";
		TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null, chineseStr, "This is the body");
		TestUtils.waitFor(getInbox(),1);
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		assertTrue("Did not find the expected message",emailIter.hasNext());
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertEquals("The subject was not correctly encoded or decoded",chineseStr, mimeMessage.getSubject());
	}

	@Test
	public void testSendSMTP() throws Exception {
		String subject = "My country is dying";
		String bodyWithCR = "\nKeep these pesky\n carriage returns\n";
		TestUtils.sendMailSMTP("steve@here.com", "bob@zim.com", null, null, subject, bodyWithCR);

		TestUtils.waitFor(getInbox(),1);


		Inbox inbox = getInbox();
		assertEquals("Sent message was not correctly recieved (Got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+")",1,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		List<BlueboxMessage> list = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertEquals("Subject did not match",subject,mimeMessage.getSubject().toString());
		// TODO - figure out why this does not work
//				assertEquals("Body did not match",bodyWithCR.length(),email.getText().length());
		//		assertEquals("Body did not match",bodyWithCR,email.getText());
	}

	@Test
	public void testSendHugeSMTP() throws Exception {
		String subject = "My country is dying";
		String bodyWithCR = "\nKeep these pesky\n carriage returns\n";
		List<File> attachments = new ArrayList<File>();
		File attachment = new File("./src/test/resources/test-data/inlineattachments.eml");
		// figure out how many attachments to add taking into account encoding etc
		// without hitting maximum mail size
		int count = (int)(Inbox.MAX_MAIL_BYTES/attachment.length()*7/10);
		log.info("Sending mail with size="+(count*attachment.length()));
		for (int i = 0; i < count; i++)
			attachments.add(attachment);
		TestUtils.sendMailSMTP("steve@here.com", "bob@zim.com", null, null, subject, bodyWithCR, attachments);
		log.info("Waiting for delivery");
		TestUtils.waitFor(getInbox(),1);


		Inbox inbox = getInbox();
		assertEquals("Sent message was not correctly recieved (Got "+inbox.getMailCount(BlueboxMessage.State.NORMAL)+")",1,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		List<BlueboxMessage> list = inbox.listInbox(new InboxAddress("bob@zim.com"), BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Iterator<BlueboxMessage> emailIter = list.iterator();
		BlueboxMessage email = (BlueboxMessage) emailIter.next();
		MimeMessage mimeMessage = email.getBlueBoxMimeMessage();
		assertEquals("Subject did not match",subject,mimeMessage.getSubject().toString());
		// TODO - figure out why this does not work
		//		assertEquals("Body did not match",bodyWithCR.length(),email.getText().length());
		//		assertEquals("Body did not match",bodyWithCR,email.getText());
	}
}
