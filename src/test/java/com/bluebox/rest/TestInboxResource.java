package com.bluebox.rest;

import com.bluebox.TestUtils;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestInboxResource extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(TestInboxResource.class);


    public void testGetEmail() throws Exception {
        Inbox.getInstance();
        TestUtils.addRandomDirect(StorageFactory.getInstance(), 5);
        String uri;
        uri = InboxResource.PATH + "/list/bluemail%20team%20%3Cbluemail@us.xxx.com%3E/undefined/";
        JSONArray resp = getRestJSONArray(uri);
        log.info("{}", resp.toString());
//		assertEquals("Email was not properly extracted","bluemail@us.xxx.com",InboxResource.extractEmail(InboxResource.extractFragment(uri, InboxResource.PATH,0)));
//		
//		uri = "/bluebox/rest/json/inbox/"+URLEncoder.encode("Stephen_Johnson/Ireland/xxx",Utils.UTF8)+"/undefined/";
//		assertEquals("Email was not properly extracted","Stephen_Johnson@Ireland.xxx",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri, JSONInboxHandler.JSON_ROOT,0)));
//		
//		uri = "/BlueBox/rest/json/inbox/xxx@xxx.com/&Start=0&Count=-1/";
//		assertEquals("Email was not properly extracted","xxx@xxx.com",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri, JSONInboxHandler.JSON_ROOT,0)));
//
//		uri = "/BlueBox/rest/json/inbox/xxx/&Start=0&Count=-1/";
//		assertEquals("Email was not properly extracted","xxx@"+Utils.getHostName(),JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,JSONInboxHandler.JSON_ROOT,0)));
//
//		uri = "/BlueBox/rest/json/inbox//&Start=0&Count=-1/";
//		assertEquals("Email was not properly extracted","",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,JSONInboxHandler.JSON_ROOT,0)));
//	
//		uri = "/bluebox/rest/json/folder/Jack%20Johnson%20%3Cjack.johnson@somewhere.com%3E/&Start=0&Count=-1/";
//		assertEquals("Email was not properly extracted","jack.johnson@somewhere.com",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,"rest/json/folder",0)));
    }

}
