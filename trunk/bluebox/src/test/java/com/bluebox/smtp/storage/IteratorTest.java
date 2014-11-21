package com.bluebox.smtp.storage;

import java.util.logging.Logger;

import junit.framework.TestCase;

import com.bluebox.TestUtils;
import com.bluebox.smtp.Inbox;

public class IteratorTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private int SIZE = 1100;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Inbox.getInstance().deleteAll();;
		TestUtils.addRandom(StorageFactory.getInstance(), SIZE);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.fine("Cleaning up messages after tests");
		Inbox.getInstance().deleteAll();
		Inbox.getInstance().stop();
	}

	public void testStepThroughAll() throws Exception {
		MessageIterator mi = new MessageIterator();
		assertTrue(mi.hasNext());
		int count = 0;
		while (mi.hasNext()) {
			count++;
			mi.next();
		}
		assertEquals("Missing items in iterator",SIZE,count);
		
		mi = new MessageIterator(null,BlueboxMessage.State.DELETED);
		assertFalse(mi.hasNext());
	}
}
