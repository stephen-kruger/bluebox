package com.bluebox.servlet;

import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.rest.json.JSONAutoCompleteHandler;

public class SearchStressTest extends BaseServletTest {
	private static final Logger log = LoggerFactory.getLogger(SearchStressTest.class);
	private int STRESS_LEVEl = 500;

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testStressSearchHandler() throws IOException, Exception {
		String url = "/"+JSONAutoCompleteHandler.JSON_ROOT+"?"+JSONAutoCompleteHandler.NAME+"=*&start=0&count=10";
		log.info("Stressing to "+STRESS_LEVEl);
		for (int i = 0; i < STRESS_LEVEl;i++) {
			JSONObject js = getRestJSON(url);
			assertEquals("Autocomplete did not find expected items",5,js.getJSONArray("items").length());
		}
		log.info("Done search stressing to "+STRESS_LEVEl);
	}

}
