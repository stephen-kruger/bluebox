package com.bluebox;

import junit.framework.TestCase;

import com.bluebox.search.SolrIndexer;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public abstract class BaseTestCase extends TestCase {
	private StorageIf jr;
	private Inbox inbox;
	private BlueBoxSMTPServer smtpServer;
	private BlueboxMessageHandlerFactory bbmhf;
	private SolrIndexer si;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		jr = StorageFactory.getInstance();
		si = SolrIndexer.getInstance();
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
		return jr;
	}

	public BlueboxMessageHandlerFactory getBlueboxMessageHandlerFactory() {
		return bbmhf;	
	}
	
	public SolrIndexer getSearchIndexer() {
		return si;
	}
}
