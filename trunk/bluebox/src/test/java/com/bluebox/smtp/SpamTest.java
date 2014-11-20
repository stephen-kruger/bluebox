package com.bluebox.smtp;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import junit.framework.TestCase;

import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SpamTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private Inbox inbox;

	@Override
	public void setUp() {
		inbox = Inbox.getInstance();
		inbox.deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		inbox.deleteAll();
		inbox.stop();
	}

	public void testSpamToggle() throws Exception {
		log.info("Checking if spam delivery path is detected");
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/spam.eml"));
		Utils.uploadEML(is);
		List<BlueboxMessage> mails = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, 5, BlueboxMessage.RECEIVED, true);
		log.info("Marking spam :"+mails.get(0).getSMTPSender());
		List<String> uids = new ArrayList<String>();
		uids.add(mails.get(0).getIdentifier());
		// toggle spam on
		WorkerThread wt = inbox.toggleSpam(uids);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(500);
		assertEquals("The sender should be rejected in the deliver phase",0,inbox.getMailCount(BlueboxMessage.State.NORMAL));
		// toggle spam off
		wt = inbox.toggleSpam(uids);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(500);
		assertEquals("The sender should be rejected in the deliver phase",1,inbox.getMailCount(BlueboxMessage.State.NORMAL));
	}

	public void testSMTPBlackList() {
		BlueboxMessageHandlerFactory mhf = new BlueboxMessageHandlerFactory(inbox);
		BlueBoxSMTPServer smtpServer = new BlueBoxSMTPServer(mhf);
		smtpServer.start();
		String domain = "qwerty.com";
		assertFalse(mhf.isBlackListed(domain));
		mhf.addSMTPBlackList(domain);
		assertTrue(mhf.isBlackListed(domain));
		mhf.removeSMTPBlackList(domain);
		assertFalse(mhf.isBlackListed(domain));
		smtpServer.stop();
	}

}
