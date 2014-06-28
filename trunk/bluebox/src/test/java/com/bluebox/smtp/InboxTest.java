package com.bluebox.smtp;

import java.io.IOException;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONObject;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.Utils;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageIf;

public class InboxTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private StorageIf jr;
	private Inbox inbox;
	private BlueBoxSMTPServer smtpServer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		inbox = Inbox.getInstance();
		inbox.deleteAll();
		smtpServer = new BlueBoxSMTPServer(new SimpleMessageListenerAdapter(inbox));
		smtpServer.start();
		int max = 10;
		do {
			// give thread time to start up
			Thread.sleep(500);
		} while ((max-- > 0)&&(!smtpServer.isRunning()));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		inbox.deleteAll();
		smtpServer.stop();
		inbox.stop();
		int max = 10;
		do {
			// give thread time to close down
			Thread.sleep(500);
		}
		while ((max-- > 0)&&(smtpServer.isRunning()));
	}

	public StorageIf getBlueBoxStorageIf() {
		return jr;
	}

	public void setBlueBoxStorageIf(StorageIf si) {
		jr = si;
	}

	public void testGlobalStats() throws AddressException, MessagingException, IOException, Exception {
		inbox.setStatsGlobalCount(987654);
		assertEquals("Incorrectly reported global stats",987654,inbox.getStatsGlobalCount());
		inbox.setStatsGlobalCount(123456);
		assertEquals("Incorrectly reported global stats",123456,inbox.getStatsGlobalCount());
	}

	public void testRecentStats() throws AddressException, MessagingException, IOException, Exception {
		String email1 = "Steve1 <steve1@there.com>";
		String email2 = "Steve2 <steve2@there.com>";
		String email3 = "Steve3 <steve3@there.com>";
		Utils.sendMessage(new InternetAddress("from@from.com"), "subject", "body", new InternetAddress[]{new InternetAddress(email1)}, new InternetAddress[]{}, new InternetAddress[]{}, false);
		Utils.sendMessage(new InternetAddress("from@from.com"), "subject", "body", new InternetAddress[]{new InternetAddress(email2)}, new InternetAddress[]{}, new InternetAddress[]{}, false);
		Utils.sendMessage(new InternetAddress("from@from.com"), "subject", "body", new InternetAddress[]{new InternetAddress(email3)}, new InternetAddress[]{}, new InternetAddress[]{}, false);


		Thread.sleep(2000);
		JSONObject json = inbox.getStatsRecent();
		log.info(json.toString(3));
		assertEquals("Incorrectly reported recent stats recipient",email3,json.getString(BlueboxMessage.TO));
		assertEquals("Incorrectly reported recent stats sender","from@from.com",json.getString(BlueboxMessage.FROM));
	}

	public void testActiveStats() throws AddressException, MessagingException, IOException, Exception {
		String email1 = "Steve1 <steve1@there.com>";
		String email2 = "Steve2 <steve2@there.com>";
		String email3 = "Steve3 <steve3@there.com>";
		for (int i = 0; i <10; i++) {
			Utils.sendMessage(new InternetAddress(email1), "subject", "body", new InternetAddress[]{new InternetAddress(email1)}, new InternetAddress[]{}, new InternetAddress[]{}, false);
		}
		for (int i = 0; i <5; i++) {
			Utils.sendMessage(new InternetAddress(email2), "subject", "body", new InternetAddress[]{new InternetAddress(email2)}, new InternetAddress[]{}, new InternetAddress[]{}, false);
		}
		for (int i = 0; i <2; i++) {
			Utils.sendMessage(new InternetAddress(email3), "subject", "body", new InternetAddress[]{new InternetAddress(email3)}, new InternetAddress[]{}, new InternetAddress[]{}, false);
		}
		JSONObject jo = inbox.getStatsActive();
		assertEquals("Incorrectly reported most active inbox",new InboxAddress(email1).getAddress(),jo.getString(BlueboxMessage.TO));
		assertEquals("Incorrectly reported most active inbox count",10,jo.getInt(BlueboxMessage.COUNT));
	}

}
