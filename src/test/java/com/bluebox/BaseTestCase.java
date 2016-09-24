package com.bluebox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.search.SearchFactory;
import com.bluebox.search.SearchIf;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

import junit.framework.TestCase;

public abstract class BaseTestCase extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(BaseTestCase.class);
    private Inbox inbox;
    private BlueBoxSMTPServer smtpServer;
    private BlueboxMessageHandlerFactory bbmhf;

	@Override
    protected void setUp() {
	log.info(" ------------------");
	log.info("| Setting up suite |");
	log.info(" ------------------");
	Config config = Config.getInstance();
	config.setProperty(Config.BLUEBOX_PORT, 2500);
	inbox = Inbox.getInstance();
	smtpServer = BlueBoxSMTPServer.getInstance(bbmhf = new BlueboxMessageHandlerFactory(inbox));
	smtpServer.start();
	inbox.deleteAll();
    }

	@Override
    protected void tearDown() {
	log.info(" -------------------");
	log.info("| Cleaning up suite |");
	log.info(" -------------------");
	inbox.deleteAll();
	inbox.stop();
	smtpServer.stop();
	while (smtpServer.isRunning())
	    log.info("Waiting for smtp server to stop");
    }

    public Inbox getInbox() {
	return inbox;
    }

    public BlueBoxSMTPServer getSMTPServer() {
	return smtpServer;
    }

    public StorageIf getBlueBoxStorageIf() {
	return StorageFactory.getInstance();
    }

    public BlueboxMessageHandlerFactory getBlueboxMessageHandlerFactory() {
	return bbmhf;	
    }

    public SearchIf getSearchIndexer() throws Exception {
	return SearchFactory.getInstance();
    }
}
