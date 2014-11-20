package com.bluebox.smtp;

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

	public void testFromVsSender() throws Exception {
		log.info("Checking if spam delivery path is detected");
		InputStream is = new FileInputStream(new File("src/test/resources/test-data/spam.eml"));
		Utils.uploadEML(is);
		List<BlueboxMessage> mails = inbox.listInbox(null, BlueboxMessage.State.NORMAL, 0, 5, BlueboxMessage.RECEIVED, true);
		log.info("Marking spam :"+mails.get(0).getSMTPSender());
		WorkerThread wt = inbox.toggleSpam(mails.get(0).getIdentifier());
		new Thread(wt).start();
		while (wt.getProgress()<100)
			Thread.sleep(500);
		assertEquals("The sender should be rejected in the deliver phase",0,inbox.getMailCount(BlueboxMessage.State.NORMAL));
	}

	
}
