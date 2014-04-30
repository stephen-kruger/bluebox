package com.bluebox.rest.json;

import junit.framework.TestCase;

import com.bluebox.Utils;
import com.bluebox.rest.json.AbstractHandler;
import com.bluebox.rest.json.JSONInboxHandler;

public class TestJSONInboxHandler extends TestCase {
	
	
	public void testGetEmail() {
		String uri;
		
		uri = "/bluebox/rest/json/inbox/bluemail%20team%20%3Cbluemail@us.xxx.com%3E/undefined/";
		assertEquals("Email was not properly extracted","bluemail@us.xxx.com",JSONInboxHandler.extractEmail(uri, "rest/json/inbox"));
		
		uri = "/bluebox/rest/json/inbox/Stephen_Johnson/Ireland/xxx%25xxxIE/undefined/";
		assertEquals("Email was not properly extracted","Stephen_Johnson@Ireland.xxx.xxxIE",JSONInboxHandler.extractEmail(uri, "rest/json/inbox"));
		
		uri = "/BlueBox/rest/json/inbox/xxx@xxx.com/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","xxx@xxx.com",AbstractHandler.extractFragment(uri, 2));

		uri = "/BlueBox/rest/json/inbox/xxx/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","xxx@"+Utils.getHostName(),JSONInboxHandler.extractEmail(uri,"rest/json/inbox"));

		uri = "/BlueBox/rest/json/inbox//&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","",JSONInboxHandler.extractEmail(uri,"rest/json/inbox"));
	
		uri = "/bluebox/rest/json/folder/Jack%20Johnson%20%3Cjack.johnson@somewhere.com%3E/&Start=0&Count=-1/";
		assertEquals("Email was not properly extracted","jack.johnson@somewhere.com",JSONInboxHandler.extractEmail(uri,"rest/json/folder"));
	}
	
}
