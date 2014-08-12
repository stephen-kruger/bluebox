package com.bluebox.rest.json;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class TestJSONMessageUtilHandler extends BaseServletTest {


	public void testGetLinks() throws IOException, Exception {
		List<JSONObject> list = Inbox.getInstance().listInboxLite(null, BlueboxMessage.State.ANY, 0, 5, BlueboxMessage.RECEIVED, true, Locale.getDefault());
		for (JSONObject jo : list) {
			String url = "/"+JSONMessageUtilHandler.JSON_ROOT+"/"+jo.getString(BlueboxMessage.UID)+"/"+JSONMessageUtilHandler.LINKS;
			JSONObject js = getRestJSON(url);
			JSONArray ja = js.getJSONArray(JSONMessageUtilHandler.LINKS);
			for (int i = 0; i < ja.length();i++) {
				assertNotNull("Link text not found",ja.getJSONObject(i).getString("text"));
				assertNotNull("Link href not found",ja.getJSONObject(i).getString("href"));
			}
			log.info(js.toString(3));
		}
	}

}
