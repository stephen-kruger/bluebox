package com.bluebox.smtp;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONObject;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.TestUtils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchIndexer;
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
		//		inbox.deleteAll();
		smtpServer = new BlueBoxSMTPServer(new SimpleMessageListenerAdapter(inbox));
		smtpServer.start();
		//		int max = 10;
		//		do {
		//			// give thread time to start up
		//			Thread.sleep(500);
		//		} while ((max-- > 0)&&(!smtpServer.isRunning()));
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		smtpServer.stop();
		inbox.deleteAll();
		inbox.stop();
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
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email1), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email2), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email3), null, null, "subject", "body");

		TestUtils.waitFor(3);
		JSONObject json = inbox.getStatsRecent();
		log.info(json.toString(3));
		assertEquals("Incorrectly reported recent stats recipient",new InboxAddress(email3).getAddress(),json.getString(BlueboxMessage.INBOX));
		assertEquals("Incorrectly reported recent stats sender","from@from.com",json.getString(BlueboxMessage.FROM));
		assertNotNull("Unset UID",json.getString(BlueboxMessage.UID));
	}

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
		inbox.updateStats(null, "", true);
		JSONObject jo = inbox.getStatsActiveInbox();
		assertEquals("Incorrectly reported most active inbox",new InboxAddress(email1).getFullAddress(),jo.getString(Inbox.EMAIL));

		assertEquals("Incorrectly reported most active recipient",new InboxAddress(email1).getDisplayName(),jo.getString(BlueboxMessage.RECIPIENT));
		assertEquals("Incorrectly reported most active inbox count",10,jo.getInt(BlueboxMessage.COUNT));

		jo = inbox.getStatsActiveSender();
		assertEquals("Incorrectly reported most active sender",new InboxAddress(email1).getAddress(),new InboxAddress(jo.getString(BlueboxMessage.FROM)).getAddress());
		assertEquals("Incorrectly reported most active sender count",10,jo.getInt(BlueboxMessage.COUNT));
	}

	public void testBackup() throws Exception {
		Inbox.getInstance().deleteAll();
		assertEquals(0,Inbox.getInstance().getMailCount(BlueboxMessage.State.ANY));
		String email1 = "aaading@kkddf.com";
		String email2 = "aaading@kkddf.com";
		String email3 = "aaading@kkddf.com";
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email1), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email2), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress("from@from.com"), new InternetAddress(email3), null, null, "subject", "body");
		assertEquals(3,Inbox.getInstance().getMailCount(BlueboxMessage.State.ANY));
		File dir = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
		dir.mkdirs();
		WorkerThread wt = Inbox.getInstance().backup(dir);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(250);
		Inbox.getInstance().deleteAll();
		assertEquals(0,Inbox.getInstance().getMailCount(BlueboxMessage.State.ANY));
		wt = Inbox.getInstance().restore(dir);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(250);
		assertEquals(3,Inbox.getInstance().getMailCount(BlueboxMessage.State.ANY));
	}

	public void testSoftDelete() throws Exception {
		inbox.deleteAll();
		String email1 = "aaading@kkddf.com";
		String from="from@from.com";
		TestUtils.sendMailSMTP(new InternetAddress(from), new InternetAddress(email1), null, null, "subject", "body");
		TestUtils.sendMailSMTP(new InternetAddress(from), new InternetAddress(email1), null, null, "subject", "body");
		Inbox inbox = Inbox.getInstance();
		assertEquals("Missing mail",2,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		List<BlueboxMessage> mail = inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.NORMAL, 0, 100, BlueboxMessage.SIZE, true);
		assertEquals("Missing mail",2,mail.size());
		SearchIndexer si = SearchIndexer.getInstance();
		assertEquals("Missing search data",2,si.search("from@from.com", SearchIndexer.SearchFields.FROM, 0, 10, SearchIndexer.SearchFields.FROM, true).length);
		inbox.spam(mail.get(0).getIdentifier());
		assertEquals("Missing deleted mail",2,inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.DELETED, 0, 100, BlueboxMessage.SIZE, true).size());
		assertEquals("Found deleted mail",0,inbox.listInbox(new InboxAddress(email1), BlueboxMessage.State.NORMAL, 0, 100, BlueboxMessage.SIZE, true).size());
		assertEquals("Unexpected search data",0,si.search(from, SearchIndexer.SearchFields.FROM, 0, 10, SearchIndexer.SearchFields.FROM, true).length);
	}

}
