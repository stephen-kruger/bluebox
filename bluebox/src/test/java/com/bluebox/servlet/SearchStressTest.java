package com.bluebox.servlet;

import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.search.SearchFactory;

public class SearchStressTest extends BaseServletTest {
	private static final Logger log = LoggerFactory.getLogger(SearchStressTest.class);
	private int STRESS_LEVEL = 500;

	public void setUp() throws Exception {
		super.setUp();
		TestUtils.addRandomNoThread(getInbox(), COUNT);
		TestUtils.waitFor(getInbox(), COUNT);
		SearchFactory.getInstance().commit(true);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testStressSearchHandler() throws IOException, Exception {
		String url = "/"+JSONAutoCompleteHandler.JSON_ROOT+"?"+JSONAutoCompleteHandler.NAME+"=&start=0&count="+COUNT;
		log.info("Stressing to "+STRESS_LEVEL);
		for (int i = 0; i < STRESS_LEVEL;i++) {
			JSONObject js = getRestJSON(url);
			assertEquals("Autocomplete did not find expected items",COUNT,js.getJSONArray("items").length());
		}
		log.info("Done search stressing to "+STRESS_LEVEL);
	}

}
