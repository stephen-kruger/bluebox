package com.bluebox.servlet;

import java.io.IOException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.smtp.storage.BlueboxMessage;

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
		int count = 0;
		while ((count++<20)&&(getMailCount(BlueboxMessage.State.NORMAL)<COUNT)) {
			System.out.println("Waiting for mail delivery :"+getMailCount(BlueboxMessage.State.NORMAL));
			Thread.sleep(1000);
		}
		assertEquals("Missing mails",COUNT,getMailCount(BlueboxMessage.State.NORMAL));

		// now hit the REST web service
		String inboxURL = "/"+JSONFolderHandler.JSON_ROOT;
		log.info("Checking URL:"+inboxURL);
		JSONObject js = getRestJSON(inboxURL);
		JSONArray items = js.getJSONArray("items");
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			assertTrue(item.has("id"));
			assertTrue(item.has("name"));
			assertTrue(item.has("type"));
			assertTrue(item.has("style"));
			JSONArray children = item.getJSONArray("children");
			for (int j = 0; j < children.length();j++) {
				JSONObject child = children.getJSONObject(j);
				assertTrue(child.has("id"));
				assertTrue(child.has("name"));
				assertTrue(child.has("style"));
				assertTrue(child.has("count"));
				assertTrue(child.has("state"));
				assertTrue(child.has("email"));
				if ((child.get("id").equals("All"))||(child.get("id").equals("Inbox"))) {
					assertEquals("Missing mails",COUNT,child.getInt("count"));
				}
				else {
					assertEquals("Missing mails",0,child.getInt("count"));					
				}
			}
		}
		log.info(js.toString(3));

		//		{
		//			   "identifier": "id",
		//			   "label": "name",
		//			   "items": [{
		//			      "id": "Overview",
		//			      "name": "Inbox for \/NORMAL@XHOSA",
		//			      "type": "folder",
		//			      "style": "rootFolder",
		//			      "children": [
		//			         {
		//			            "id": "Inbox",
		//			            "name": "Inbox (0)",
		//			            "count": 0,
		//			            "email": "\/NORMAL@XHOSA",
		//			            "state": "NORMAL",
		//			            "style": "inboxFolder"
		//			         },
		//			         {
		//			            "id": "Trash",
		//			            "name": "Trash (0)",
		//			            "count": 0,
		//			            "email": "\/NORMAL@XHOSA",
		//			            "state": "DELETED",
		//			            "style": "trashFolder"
		//			         },
		//			         {
		//			            "id": "All",
		//			            "name": "All documents (0)",
		//			            "count": 0,
		//			            "email": "\/NORMAL@XHOSA",
		//			            "state": "ANY",
		//			            "style": "allFolder"
		//			         }
		//			      ]
		//			   }]
		//			}		
	}


}
