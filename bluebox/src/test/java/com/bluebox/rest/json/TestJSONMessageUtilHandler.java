package com.bluebox.rest.json;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.rest.MessageResource;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class TestJSONMessageUtilHandler extends BaseServletTest {
	private static final Logger log = LoggerFactory.getLogger(TestJSONMessageUtilHandler.class);


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestUtils.addRandomDirect(StorageFactory.getInstance(), COUNT);
	}


	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}


	public void testGetLinks() throws IOException, Exception {
		List<LiteMessage> list = getInbox().listInboxLite(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true, Locale.getDefault());
		assertEquals("Missing mail",COUNT,list.size());
		for (LiteMessage jo : list) {
			String url = MessageResource.PATH+"/"+MessageResource.LINKS+jo.getIdentifier();
			JSONObject js = getRestJSON(url);
			JSONArray ja = js.getJSONArray(MessageResource.LINKS);
			for (int i = 0; i < ja.length();i++) {
				assertNotNull("Link text not found",ja.getJSONObject(i).getString("text"));
				assertNotNull("Link href not found",ja.getJSONObject(i).getString("href"));
			}
			log.info(js.toString(3));
		}
	}

}
