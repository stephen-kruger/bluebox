package com.bluebox.servlet;

import java.io.IOException;

import javax.mail.internet.InternetAddress;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.rest.json.JSONStatsHandler;

public class RestStressTest extends BaseServletTest {
	private static final Logger log = LoggerFactory.getLogger(RestStressTest.class);
	private int STRESS_LEVEl = 500;

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testStressStatsHandler() throws IOException, Exception {
		TestUtils.sendMailDirect(getInbox(), "bob@bob.com", "joe@joe.com");
		String url = "/"+JSONStatsHandler.JSON_ROOT+"/"+JSONStatsHandler.GLOBAL_STAT+"/";
		log.info("Stressing to "+STRESS_LEVEl);
		for (int i = 0; i < STRESS_LEVEl;i++) {
			JSONObject js = getRestJSON(url);
			assertNotNull(js.get("Count"));
			assertTrue(js.getInt("Count")==1);
			if ((i % 1000)==0)
				log.info("Processed "+i);
		}
		log.info("Done stressing to "+STRESS_LEVEl);
	}

}
