package com.bluebox.rest.json;

import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;

public class TestJSONAutoCompleteHandler extends BaseTestCase {
	private static final Logger log = LoggerFactory.getLogger(TestJSONAutoCompleteHandler.class);
	private JSONAutoCompleteHandler handler;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		handler = new JSONAutoCompleteHandler();
	}
	
	@Test
	public void testFullname() throws Exception {
		TestUtils.sendMailDirect(getInbox(),"\"Joe Blow\" <jblow@example.com>", "WinEdt Mailing List <winedt+list@wsg.net>");
		JSONArray result = handler.doAutoComplete(getInbox(), "Joe", "0", "10");
		assertEquals("Missing results",1,result.length());
		assertEquals("Incorrect name set","Joe Blow <jblow@example.com>",result.getJSONObject(0).getString("label"));
	}

	@Test
	public void testAutoComplete() throws Exception {
		log.info("Testing type-ahead");
		for (int i = 0; i < 20; i++) {
			TestUtils.sendMailDirect(getInbox(),"\"Joe Blow\" <jblow@example.com>", "WinEdt Mailing List <winedt+list@wsg.net>");
		}

		assertEquals("Should not trigger for one character", 0, handler.doAutoComplete(getInbox(), "j", "0", "10").length());
		assertTrue("No results found", handler.doAutoComplete(getInbox(), "Joe", "0", "10").length()>0);
		log.debug(handler.doAutoComplete(getInbox(), "Joe", "0", "10").toString(3));
		assertTrue("Unexpected results found", handler.doAutoComplete(getInbox(), "Joe", "0", "10").getJSONObject(0).getString("label").toLowerCase().contains("joe"));
		assertTrue("Case sensitivity problem", handler.doAutoComplete(getInbox(), "joe", "0", "10").length()>0);
		assertEquals("Uniqueness problem", 1, handler.doAutoComplete(getInbox(), "joe", "0", "10").length());
		assertEquals("Should not find anything", 0, handler.doAutoComplete(getInbox(), "xyz", "0", "10").length());
		assertTrue("Could not search on email", handler.doAutoComplete(getInbox(), "jblow", "0", "10").length()>0);
		assertEquals("Could not search on email", 1, handler.doAutoComplete(getInbox(), "jblow", "0", "10").length());
		assertTrue("Could not search on case sensistive name", handler.doAutoComplete(getInbox(), "Blow", "0", "10").length()>0);
		assertTrue("Should not find results for non-existing names", handler.doAutoComplete(getInbox(), "Steve", "0", "10").length()==0);
	}

	@Test
	public void testAutoCompleteDuplicates() throws Exception {
		// add 10 instances of 10 emails. Then check we recieved 10 auto-complete results
		String to = "bob-the-tester@nowhere.com";
		String from = "bob-the-sender@nowhere.com";
		int count = 5;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < count; j++) {
				TestUtils.sendMailDirect(getInbox(),to, i+from+j);
			}
		}

		// check we recieve 10 non-identical results
		assertEquals("Did not receive expected number of results", 1, handler.doAutoComplete(getInbox(), "bob", "0", Integer.toString(count)).length());
		assertEquals("Did not recieve expected number of results", 1, handler.doAutoComplete(getInbox(), "*", "0", Integer.toString(count)).length());
	}
	
	@Test
	public void testAutoCompleteWildcard() throws Exception {
		// add 10 emails. Then check we recieved 10 auto-complete results
		String to = "bob-the-tester@nowhere.com";
		String from = "bob-the-sender@nowhere.com";
		int count = 10;
		for (int i = 0; i < count; i++) {
			TestUtils.sendMailDirect(getInbox(),i+to, from);
		}

		// check we recieve 10 non-identical results
		assertEquals("Did not receive expected number of results", count, handler.doAutoComplete(getInbox(), "*", "0", "Infinity").length());
		assertEquals("Did not receive expected number of results", count/2, handler.doAutoComplete(getInbox(), "*", "0", ""+count/2).length());
		assertEquals("Did not receive expected number of results", count, handler.doAutoComplete(getInbox(), "*", "0", Integer.toString(count)).length());
	}

}
