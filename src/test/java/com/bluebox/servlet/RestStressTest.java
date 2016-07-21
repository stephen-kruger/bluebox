package com.bluebox.servlet;

import java.io.IOException;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.smtp.Inbox;

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
		String url = "/stats/global";
		clearMail();
		TestUtils.sendMailDirect(Inbox.getInstance(), "bob@bob.com", "joe@joe.com");
		log.info("Stressing to "+STRESS_LEVEl);
		for (int i = 0; i < STRESS_LEVEl;i++) {
			JSONObject js = getRestJSON(url);
			assertNotNull(js.get("countAll"));
			assertEquals("Missing emails",1,js.getInt("countAll"));
			if ((i % 1000)==0)
				log.info("Processed "+i);
		}
		log.info("Done stressing to "+STRESS_LEVEl);
	}

}
