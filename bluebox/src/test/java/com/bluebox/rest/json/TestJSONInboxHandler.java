package com.bluebox.rest.json;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import junit.framework.TestCase;

import com.bluebox.Utils;

public class TestJSONInboxHandler extends TestCase {
	
	
	public void testGetEmail() throws UnsupportedEncodingException {
		String uri;
		
		uri = "/bluebox/rest/json/inbox/bluemail%20team%20%3Cbluemail@us.xxx.com%3E/undefined/";
		assertEquals("Email was not properly extracted","bluemail@us.xxx.com",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri, JSONInboxHandler.JSON_ROOT,0)));
		
		uri = "/bluebox/rest/json/inbox/"+URLEncoder.encode("Stephen_Johnson/Ireland/xxx",Utils.UTF8)+"/undefined/";
		assertEquals("Email was not properly extracted","Stephen_Johnson@Ireland.xxx",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri, JSONInboxHandler.JSON_ROOT,0)));
		
		uri = "/BlueBox/rest/json/inbox/xxx@xxx.com/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","xxx@xxx.com",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri, JSONInboxHandler.JSON_ROOT,0)));

		uri = "/BlueBox/rest/json/inbox/xxx/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","xxx@"+Utils.getHostName(),JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,JSONInboxHandler.JSON_ROOT,0)));

		uri = "/BlueBox/rest/json/inbox//&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,JSONInboxHandler.JSON_ROOT,0)));
	
		uri = "/bluebox/rest/json/folder/Jack%20Johnson%20%3Cjack.johnson@somewhere.com%3E/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","jack.johnson@somewhere.com",JSONInboxHandler.extractEmail(JSONInboxHandler.extractFragment(uri,"rest/json/folder",0)));
	}
	
}
