package com.bluebox;

import java.io.IOException;

import junit.framework.TestCase;

import com.bluebox.search.SearchFactory;
import com.bluebox.search.SearchIf;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public abstract class BaseTestCase extends TestCase {
	private Inbox inbox;
	private BlueBoxSMTPServer smtpServer;
	private BlueboxMessageHandlerFactory bbmhf;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		inbox = new Inbox();
		inbox.start();
		smtpServer = new BlueBoxSMTPServer(bbmhf = new BlueboxMessageHandlerFactory(inbox));
		smtpServer.start();
		inbox.deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		smtpServer.stop();
		inbox.deleteAll();
		inbox.stop();
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
	
	public SearchIf getSearchIndexer() throws IOException {
		return SearchFactory.getInstance();
	}
}
