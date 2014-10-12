package com.bluebox.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mortbay.jetty.testing.HttpTester;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.rest.json.JSONMessageHandler;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;

public class RestTest extends BaseServletTest {

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAutocomplete() throws IOException, Exception {
		String url = "/"+JSONAutoCompleteHandler.JSON_ROOT+"?start=0&count=10&name=*";
		JSONObject js = getRestJSON(url);
		JSONArray ja = js.getJSONArray("items");
		for (int i = 0; i < ja.length();i++) {
			assertNotSame("Full name not found",ja.getJSONObject(i).getString("name"),ja.getJSONObject(i).getString("label"));
		}
		log.info(js.toString(3));
	}

	public void testInbox() throws Exception {


		// first we check directly
		TestUtils.waitFor(COUNT);

		assertEquals("Missing mails",COUNT,getMailCount(BlueboxMessage.State.NORMAL));

		// now hit the REST web service
		String inboxURL = "/"+JSONFolderHandler.JSON_ROOT;
		log.info("Checking URL:"+inboxURL);
		JSONObject js = getRestJSON(inboxURL);
		
		JSONObject child = js.getJSONObject(BlueboxMessage.State.ANY.name());
		assertEquals("Missing mails",COUNT,child.getInt("count"));
		assertTrue(child.has("id"));
		assertTrue(child.has("name"));
		assertTrue(child.has("style"));
		assertTrue(child.has("count"));
		assertTrue(child.has("state"));
		assertTrue(child.has("email"));
		
		child = js.getJSONObject(BlueboxMessage.State.NORMAL.name());
		assertEquals("Missing mails",COUNT,child.getInt("count"));
		assertTrue(child.has("id"));
		assertTrue(child.has("name"));
		assertTrue(child.has("style"));
		assertTrue(child.has("count"));
		assertTrue(child.has("state"));
		assertTrue(child.has("email"));
		
		child = js.getJSONObject(BlueboxMessage.State.DELETED.name());
		assertEquals("Missing mails",0,child.getInt("count"));
		assertTrue(child.has("id"));
		assertTrue(child.has("name"));
		assertTrue(child.has("style"));
		assertTrue(child.has("count"));
		assertTrue(child.has("state"));
		assertTrue(child.has("email"));
	}

	public void testInlineHandler() throws Exception {
		Inbox.getInstance().deleteAll();
		TestUtils.waitFor(0);
		InputStream emlStream = new FileInputStream("src/test/resources"+File.separator+"test-data"+File.separator+"attachments.eml");
		Utils.uploadEML(emlStream);
		TestUtils.waitFor(1);
		assertEquals("Mail was not delivered",1,Inbox.getInstance().getMailCount(State.ANY));

		List<BlueboxMessage> messages = Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true);
		BlueboxMessage msg = messages.get(0);
		HttpTester request = new HttpTester();
		// now retrieve the atachment by name
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(getBaseURL()+"/"+JSONInlineHandler.JSON_ROOT+"/"+msg.getIdentifier()+"/DSC_3968.JPG");
		request.setVersion("HTTP/1.0");

		HttpTester response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));

		assertEquals(200,response.getStatus());

		// now retrieve the atachment by uid
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(getBaseURL()+"/"+JSONInlineHandler.JSON_ROOT+"/"+msg.getIdentifier()+"/ii_hxqkskb21_147462ce25a92ebf");
		request.setVersion("HTTP/1.0");

		response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));

		assertEquals(200,response.getStatus());
	}

	public void testJSONMessageHandler() throws IOException, Exception {
		List<BlueboxMessage> messages = Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true);
		for (BlueboxMessage message : messages) {
			String url = "/"+JSONMessageHandler.JSON_ROOT+"/"+message.getIdentifier();
			JSONObject js = getRestJSON(url);
			
			log.info(js.toString(3));
		}
	}

}
