package com.bluebox.rest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class TestFolderResource extends BaseServletTest {
	private static final Logger log = LoggerFactory.getLogger(BaseServletTest.class);

	@Test
	public void testGetEmail() throws UnsupportedEncodingException {
		log.info("Deprecated");
//		String uri;
//
//		uri = JSONFolderHandler.JSON_ROOT+"/bluemail%20team%20%3Cbluemail@us.xxx.com%3E";
//		assertEquals("Email was not properly extracted","bluemail@us.xxx.com",JSONFolderHandler.extractEmail(JSONFolderHandler.extractFragment(uri,JSONFolderHandler.JSON_ROOT,0)));
//
//		uri = JSONFolderHandler.JSON_ROOT+"/"+URLEncoder.encode("Stephen_Johnson/Iceland/MyDomain",Utils.UTF8);
//
//		assertEquals("Email was not properly extracted","Stephen_Johnson@Iceland.MyDomain",JSONFolderHandler.extractEmail(JSONFolderHandler.extractFragment(uri,JSONFolderHandler.JSON_ROOT,0)));
//
//		uri = "/bluebox/"+JSONFolderHandler.JSON_ROOT+"/Sally%20Johnson%20%3Csally.johnson@somewhere.com%3E/";	
//		assertEquals("Email was not properly extracted","sally.johnson@somewhere.com",JSONFolderHandler.extractEmail(JSONFolderHandler.extractFragment(uri,JSONFolderHandler.JSON_ROOT,0)));
	}

	@Test
	public void testFolderCount() throws IOException, Exception {
		TestUtils.addRandomDirect(StorageFactory.getInstance(), COUNT);
		List<LiteMessage> list = Inbox.getInstance().listInboxLite(null, BlueboxMessage.State.ANY, 0, 100, BlueboxMessage.RECEIVED, true, Locale.getDefault());
		assertEquals("Missing mails",COUNT,list.size());
		String url = FolderResource.PATH+"/counts/";
		JSONObject js = getRestJSON(url);
		assertEquals("Incorrect All count",list.size(),js.getJSONObject(BlueboxMessage.State.ANY.name()).getInt("count"));
		assertEquals("Incorrect Normal count",list.size(),js.getJSONObject(BlueboxMessage.State.NORMAL.name()).getInt("count"));
		assertEquals("Incorrect Deleted count",0,js.getJSONObject(BlueboxMessage.State.DELETED.name()).getInt("count"));

		// now delete 1 mail
		Inbox.getInstance().softDelete(list.get(0).getIdentifier());
		js = getRestJSON(url);
		assertEquals("Incorrect All count",list.size(),js.getJSONObject(BlueboxMessage.State.ANY.name()).getInt("count"));
		assertEquals("Incorrect Normal count",list.size()-1,js.getJSONObject(BlueboxMessage.State.NORMAL.name()).getInt("count"));
		assertEquals("Incorrect Deleted count",1,js.getJSONObject(BlueboxMessage.State.DELETED.name()).getInt("count"));
		log.debug(js.toString(3));
	}

}
