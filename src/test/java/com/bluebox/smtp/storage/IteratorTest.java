package com.bluebox.smtp.storage;

import java.util.logging.Logger;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;

public class IteratorTest extends BaseTestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private int SIZE = 1100;


	@Override
	protected void setUp() {
		super.setUp();
		log.info("Populating testdata");
		try {
		    TestUtils.addRandomDirect(StorageFactory.getInstance(), SIZE);
		} 
		catch (Exception e) {
		    e.printStackTrace();
		}
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
	
	public void testStepThroughAllLite() throws Exception {
		LiteMessageIterator mi = new LiteMessageIterator();
		assertTrue(mi.hasNext());
		int count = 0;
		while (mi.hasNext()) {
			count++;
			mi.next();
		}
		assertEquals("Missing items in iterator",SIZE,count);
		
		mi = new LiteMessageIterator(null,BlueboxMessage.State.DELETED);
		assertFalse(mi.hasNext());
	}
}
