package com.bluebox.rest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class TestAttachmentResource extends TestCase {
	private static final Logger log = LoggerFactory.getLogger(TestAttachmentResource.class);

	//	public void testGetEmail() {
	//		String uri = JSONAttachmentHandler.JSON_ROOT+"/26e3a411-f456-4c5f-a531-3b73f43ecf7f/1";
	//		// Form 1
	//		assertEquals("Uid was not properly extracted","26e3a411-f456-4c5f-a531-3b73f43ecf7f",AbstractHandler.extractFragment(uri,JSONAttachmentHandler.JSON_ROOT,0));
	//		assertEquals("Index was not properly extracted","1",AbstractHandler.extractFragment(uri,"/bluebox/rest/json/inbox/attachment",1));
	//		// Form 2
	//		uri = JSONAttachmentHandler.JSON_ROOT+"/26e3a411-f456-4c5f-a531-3b73f43ecf7f";
	//		assertEquals("Uid was not properly extracted","26e3a411-f456-4c5f-a531-3b73f43ecf7f",AbstractHandler.extractFragment(uri,JSONAttachmentHandler.JSON_ROOT,0));
	//	}

	//	public void testExtractFragment() {
	//		String uri = "xxx/aaa/bbb/ccc/ddd";
	//		assertEquals("Fragment did not match","ddd",AbstractHandler.extractFragment(uri,"xxx",3));
	//		assertEquals("Fragment did not match","ccc",AbstractHandler.extractFragment(uri,"xxx",2));
	//		assertEquals("Fragment did not match","bbb",AbstractHandler.extractFragment(uri,"xxx",1));
	//		assertEquals("Fragment did not match","aaa",AbstractHandler.extractFragment(uri,"xxx",0));
	//	}

	@Test
	public void testImplementMe() {
		log.info("Not implemented");
	}

}
