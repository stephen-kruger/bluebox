package com.bluebox.rest.json;

import com.bluebox.rest.json.AbstractHandler;

import junit.framework.TestCase;

public class TestAbstractHandler extends TestCase {


	public void testURIExtraction() {
		String uri = "/aaa/bbb/ccc/ddd";
		assertEquals("Field was not properly extracted","ddd",AbstractHandler.extractFragment(uri, 0));
		assertEquals("Field was not properly extracted","ccc",AbstractHandler.extractFragment(uri, 1));
		assertEquals("Field was not properly extracted","bbb",AbstractHandler.extractFragment(uri, 2));
		assertEquals("Field was not properly extracted","aaa",AbstractHandler.extractFragment(uri, 3));

		uri = "/aaa/bbb/ccc/ddd/";
		assertEquals("Field was not properly extracted","",AbstractHandler.extractFragment(uri, 0));
		assertEquals("Field was not properly extracted","ddd",AbstractHandler.extractFragment(uri, 1));
		assertEquals("Field was not properly extracted","ccc",AbstractHandler.extractFragment(uri, 2));
		assertEquals("Field was not properly extracted","bbb",AbstractHandler.extractFragment(uri, 3));
		assertEquals("Field was not properly extracted","aaa",AbstractHandler.extractFragment(uri, 4));
	}

}
