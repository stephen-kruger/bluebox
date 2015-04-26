package com.bluebox.smtp.storage;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.apache.tools.ant.filters.StringInputStream;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

public class BasicStorageTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private StorageIf si;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		log.fine("Test setup");
		si = StorageFactory.getInstance();
//		si.start();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		si.stop();
	}

	@Test
	public void testPropsStorage() throws Exception {
		si.setProperty("xxx", "yyy");
		assertEquals("Unexpected value","yyy",si.getProperty("xxx","Not found"));
		assertEquals("Unexpected default value","zzz",si.getProperty("aaa","zzz"));
		
		si.setLongProperty("number", 100);
		assertEquals("Unexpected number value",100,si.getLongProperty("number",0));
	}
	
	@Test
	public void testErrorStorage() throws Exception {
		si.logErrorClear();
		assertEquals(0,si.logErrorCount());
		si.logError("error title", "this is some content");
		assertEquals(1,si.logErrorCount());
		JSONArray ja = si.logErrorList(0, 10);
		assertEquals(1,ja.length());
		assertEquals("error title",ja.getJSONObject(0).get("title"));
		assertEquals("this is some content",si.logErrorContent(ja.getJSONObject(0).get("id").toString()));
	}
	
	@Test
	public void testSpoolStorage() throws Exception {
		si.trimSpools(0);
		assertEquals("Spool count should be 0",0,si.getSpoolCount());
		String s = new String("1234567890");
		for (int i = 0; i < (AbstractStorage.MAX_SPOOL_SIZE*3); i++) {
			si.spoolStream(new StringInputStream(s));
		}
		assertEquals("Spool count should be capped at "+AbstractStorage.MAX_SPOOL_SIZE,AbstractStorage.MAX_SPOOL_SIZE,si.getSpoolCount());
	}
}
