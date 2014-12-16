package com.bluebox.rest.json;

import com.bluebox.rest.json.AbstractHandler;

import junit.framework.TestCase;

public class TestAbstractHandler extends TestCase {


	public void testURIExtraction() {
		String JSON_ROOT="zzzz";
		String uri = JSON_ROOT+"/aaa/bbb/ccc/ddd";
		assertEquals("Field was not properly extracted","ddd",AbstractHandler.extractFragment(uri, JSON_ROOT, 3));
		assertEquals("Field was not properly extracted","ccc",AbstractHandler.extractFragment(uri, JSON_ROOT, 2));
		assertEquals("Field was not properly extracted","bbb",AbstractHandler.extractFragment(uri, JSON_ROOT, 1));
		assertEquals("Field was not properly extracted","aaa",AbstractHandler.extractFragment(uri, JSON_ROOT, 0));

		uri = JSON_ROOT+"/aaa/bbb/ccc/ddd/";
		assertEquals("Field was not properly extracted","",AbstractHandler.extractFragment(uri, JSON_ROOT, 4));
		assertEquals("Field was not properly extracted","ddd",AbstractHandler.extractFragment(uri, JSON_ROOT, 3));
		assertEquals("Field was not properly extracted","ccc",AbstractHandler.extractFragment(uri, JSON_ROOT, 2));
		assertEquals("Field was not properly extracted","bbb",AbstractHandler.extractFragment(uri, JSON_ROOT, 1));
		assertEquals("Field was not properly extracted","aaa",AbstractHandler.extractFragment(uri, JSON_ROOT, 0));
		
		uri = JSON_ROOT+"/rest/json/stats/stats_mph//";
		assertEquals("Field was not properly extracted","stats_mph",AbstractHandler.extractFragment(uri, JSON_ROOT, 3));
		assertEquals("Field was not properly extracted","",AbstractHandler.extractFragment(uri, JSON_ROOT, 5));
		
		uri = JSON_ROOT+"/rest/json/stats/stats_mph/bob@test.com";
		assertEquals("Field was not properly extracted","stats_mph",AbstractHandler.extractFragment(uri, JSON_ROOT, 3));
		assertEquals("Field was not properly extracted","bob@test.com",AbstractHandler.extractFragment(uri, JSON_ROOT, 4));

		uri = JSON_ROOT+"/aaa/bbb//ddd";
		assertEquals("Field was not properly extracted","",AbstractHandler.extractFragment(uri, JSON_ROOT, 2));
}

}
