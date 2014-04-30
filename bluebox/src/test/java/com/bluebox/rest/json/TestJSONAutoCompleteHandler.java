package com.bluebox.rest.json;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONArray;

import com.bluebox.TestUtils;
import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class TestJSONAutoCompleteHandler extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private Inbox inbox;
	private JSONAutoCompleteHandler handler;
	private StorageIf jr;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setBlueBoxStorageIf(StorageFactory.getInstance());	
		inbox = Inbox.getInstance();
		handler = new JSONAutoCompleteHandler();
		
		log.fine("Cleaning up messages to start tests");
		getBlueBoxStorageIf().deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		inbox.deleteAll();
		inbox.stop();
	}
	
	public void testFullname() throws Exception {
		TestUtils.sendMail(getBlueBoxStorageIf(),"\"Joe Blow\" <jblow@example.com>", "WinEdt Mailing List <winedt+list@wsg.net>");
		JSONArray result = handler.doAutoComplete(inbox, "Joe", "0", "10");
		assertEquals("Missing results",1,result.length());
		assertEquals("Incorrect name set","Joe Blow <jblow@example.com>",result.getJSONObject(0).getString("label"));
	}

	public void testAutoComplete() throws Exception {
		log.info("Testing type-ahead");
		for (int i = 0; i < 20; i++) {
			TestUtils.sendMail(getBlueBoxStorageIf(),"\"Joe Blow\" <jblow@example.com>", "WinEdt Mailing List <winedt+list@wsg.net>");
		}

		assertEquals("Should not trigger for one character", 0, handler.doAutoComplete(inbox, "j", "0", "10").length());
		assertTrue("No results found", handler.doAutoComplete(inbox, "Joe", "0", "10").length()>0);
		log.info(handler.doAutoComplete(inbox, "Joe", "0", "10").toString(3));
		assertTrue("Unexpected results found", handler.doAutoComplete(inbox, "Joe", "0", "10").getJSONObject(0).getString("label").toLowerCase().contains("joe"));
		assertTrue("Case sensitivity problem", handler.doAutoComplete(inbox, "joe", "0", "10").length()>0);
		assertEquals("Uniqueness problem", 1, handler.doAutoComplete(inbox, "joe", "0", "10").length());
		assertEquals("Should not find anything", 0, handler.doAutoComplete(inbox, "xyz", "0", "10").length());
		assertTrue("Could not search on email", handler.doAutoComplete(inbox, "jblow", "0", "10").length()>0);
		assertEquals("Could not search on email", 1, handler.doAutoComplete(inbox, "jblow", "0", "10").length());
		assertTrue("Could not search on case sensistive name", handler.doAutoComplete(inbox, "Blow", "0", "10").length()>0);
		assertTrue("Should not find results for non-existing names", handler.doAutoComplete(inbox, "Steve", "0", "10").length()==0);
	}

	public void testAutoCompleteDuplicates() throws Exception {
		// add 10 instances of 10 emails. Then check we recieved 10 auto-complete results
		String to = "bob-the-tester@nowhere.com";
		String from = "bob-the-sender@nowhere.com";
		int count = 5;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < count; j++) {
				TestUtils.sendMail(getBlueBoxStorageIf(),to, i+from+j);
			}
		}

		// check we recieve 10 non-identical results
		assertEquals("Did not receive expected number of results", 1, handler.doAutoComplete(inbox, "bob", "0", Integer.toString(count)).length());
		assertEquals("Did not recieve expected number of results", 1, handler.doAutoComplete(inbox, "*", "0", Integer.toString(count)).length());
	}
	
	public void testAutoCompleteWildcard() throws Exception {
		// add 10 emails. Then check we recieved 10 auto-complete results
		String to = "bob-the-tester@nowhere.com";
		String from = "bob-the-sender@nowhere.com";
		int count = 10;
		for (int i = 0; i < count; i++) {
			TestUtils.sendMail(getBlueBoxStorageIf(),i+to, from);
		}

		// check we recieve 10 non-identical results
		assertEquals("Did not receive expected number of results", count, handler.doAutoComplete(inbox, "*", "0", "Infinity").length());
		assertEquals("Did not receive expected number of results", count/2, handler.doAutoComplete(inbox, "*", "0", ""+count/2).length());
		assertEquals("Did not receive expected number of results", count, handler.doAutoComplete(inbox, "*", "0", Integer.toString(count)).length());
	}

	public StorageIf getBlueBoxStorageIf() {
		return jr;
	}

	public void setBlueBoxStorageIf(StorageIf si) {
		jr = si;
	}

}
