package com.bluebox.smtp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.bluebox.BaseTestCase;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SpamTest extends BaseTestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	public void testSpamToggle() throws Exception {
		log.info("Checking if spam delivery path is detected");
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/spam.eml"));
		Utils.uploadEML(getInbox(),is);
		List<BlueboxMessage> mails = getInbox().listInbox(null, BlueboxMessage.State.NORMAL, 0, 5, BlueboxMessage.RECEIVED, true);
		log.info("Marking spam :"+mails.get(0).getSMTPSender());
		List<String> uids = new ArrayList<String>();
		uids.add(mails.get(0).getIdentifier());
		// toggle spam on
		WorkerThread wt = getInbox().toggleSpam(uids);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(500);
		assertEquals("The sender should be rejected in the deliver phase",0,getInbox().getMailCount(BlueboxMessage.State.NORMAL));
		// toggle spam off
		wt = getInbox().toggleSpam(uids);
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(500);
		assertEquals("The sender should be rejected in the deliver phase",1,getInbox().getMailCount(BlueboxMessage.State.NORMAL));
	}

	public void testSMTPBlackList() {
		String domain = "qwerty.com";
		assertFalse(getBlueboxMessageHandlerFactory().isBlackListed(domain,domain));
		getBlueboxMessageHandlerFactory().addSMTPBlackList(domain);
		assertTrue(getBlueboxMessageHandlerFactory().isBlackListed(domain,domain));
		getBlueboxMessageHandlerFactory().removeSMTPBlackList(domain);
		assertFalse(getBlueboxMessageHandlerFactory().isBlackListed(domain,domain));
	}
	
	public void testHostRetrieval() throws Exception {
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/spam2.eml"));
		Utils.uploadEML(getInbox(),is);
		List<BlueboxMessage> mails = getInbox().listInbox(null, BlueboxMessage.State.NORMAL, 0, 5, BlueboxMessage.RECEIVED, true);
		log.info("Marking spam :"+mails.get(0).getSMTPSender());
	}

}
