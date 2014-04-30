package com.bluebox.rest.json;

import com.bluebox.rest.json.JSONFolderHandler;

import junit.framework.TestCase;

public class TestJSONFolderHandler extends TestCase {
	
	
	public void testGetEmail() {
		String uri;

		uri = "/bluebox/rest/json/folder/bluemail%20team%20%3Cbluemail@us.xxx.com%3E";
		assertEquals("Email was not properly extracted","bluemail@us.xxx.com",JSONFolderHandler.extractEmail(uri,"rest/json/folder"));

		uri = "/bluebox/rest/json/folder/Stephen_Johnson/Iceland/XXX%25XXXIE";
		assertEquals("Email was not properly extracted","Stephen_Johnson@Iceland.XXX.XXXIE",JSONFolderHandler.extractEmail(uri,"rest/json/folder"));
	
	}
	
}
