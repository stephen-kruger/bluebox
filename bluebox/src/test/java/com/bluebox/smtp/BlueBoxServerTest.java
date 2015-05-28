package com.bluebox.smtp;

import java.util.logging.Logger;

import org.junit.Test;

import junit.framework.TestCase;

public class BlueBoxServerTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	//	private SMTPServer smtpServer;
	//	private Config config = Config.getInstance();

	public BlueBoxServerTest(String s) {
		super(s);
	}

	protected void setUp() throws Exception {
		super.setUp();
		//		smtpServer = new BlueBoxSMTPServer(BlueboxMessageHandlerFactory.getInstance());
		//		smtpServer.start();
		//		log.fine("Test setUp");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		//		smtpServer.stop();
		//		Inbox.getInstance().deleteAll();
		//		Inbox.getInstance().stop();
	}

	@Test
	public void testDummy() {
		log.info("not yet implemented");
	}

}
