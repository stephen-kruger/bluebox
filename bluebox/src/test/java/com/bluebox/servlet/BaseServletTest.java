package com.bluebox.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public abstract class BaseServletTest extends TestCase {

	private ServletTester tester;
	private String baseURL;
	private String contextPath = "/";
	public ServletHolder bbs, feeds;
	public static final Logger log = Logger.getAnonymousLogger();
	public static final int COUNT = 5;
	public static final String RECIPIENT = "user@there.com";

	@Override
	protected void setUp() throws Exception {

		tester = new ServletTester();
		tester.setContextPath(contextPath);
		//ServletHolder jsp = tester.addServlet(org.apache.jasper.servlet.JspServlet.class, "*.jsp");
		bbs = tester.addServlet(com.bluebox.BlueBoxServlet.class, "/rest/*");
		feeds = tester.addServlet(com.bluebox.feed.FeedServlet.class, "/feed/*");

		tester.setResourceBase("WebContent");
		//		tester.addServlet(DefaultServlet.class, "/");
		baseURL = tester.createSocketConnector(false);

		log.info("Starting servlets at "+baseURL);
		tester.start();

		// this triggers the servlet to actually start
		getRestJSON("/"+JSONFolderHandler.JSON_ROOT);

		// clear mailboxes
		getURL("/rest/admin/clear");

		// send some test messages
		//Utils.sendSingleMessage(COUNT);
		for (int i = 0; i < COUNT; i++)
			TestUtils.sendMailSMTP(Utils.getRandomAddress(), Utils.getRandomAddress(), null, null, "subject", "body");
		getRestJSON("/"+JSONFolderHandler.JSON_ROOT);

//				Thread.sleep(5000);


		//		Inbox inbox = Inbox.getInstance();
		//		InputStream is = new FileInputStream(new File("src/test/resources/test-data/inlineattachments.eml"));
		//		inbox.deliver("sender@here.com", "user@there.com", is);
		//
		//		is = new FileInputStream(new File("src/test/resources/test-data/crash.eml"));
		//		inbox.deliver("sender@here.com", RECIPIENT, is);
		//
		//		is = new FileInputStream(new File("src/test/resources/test-data/m0017.txt"));
		//		inbox.deliver("sender@here.com", RECIPIENT, is);
		//		log.fine("Test setUp");

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.info("Stopping servlet");

		try {
			tester.stop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		try {
			bbs.stop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		try {
			Inbox inbox = Inbox.getInstance();
			inbox.deleteAll();
			inbox.stop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}

	}

	public int getMailCount(BlueboxMessage.State state) {
		String url = "/"+JSONFolderHandler.JSON_ROOT;
		try {
			JSONObject js = getRestJSON(url);
			JSONArray children = js.getJSONArray("items").getJSONObject(0).getJSONArray("children");
			for (int i = 0; i < children.length();i++) {
				JSONObject stateC = children.getJSONObject(i);
				if (BlueboxMessage.State.valueOf(stateC.getString("state"))==state) {
					return stateC.getInt("count");
				}
			}
			return 0;
		}
		catch (Throwable t) {
			t.printStackTrace();
			return 0;
		}
	}

	public JSONObject getRestJSON(String url) throws IOException, Exception {
		HttpTester request = new HttpTester();
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(url);
		request.setVersion("HTTP/1.0");

		HttpTester response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));

		assertNull(response.getMethod());
		assertEquals(200,response.getStatus());
		String js = response.getContent();
		try {
			JSONObject jo = new JSONObject(js);
			return jo;
		}
		catch (Throwable t) {
			log.info(js);
			throw t;
		}
	}

	public String getURL(String url) throws IOException, Exception {
		HttpTester request = new HttpTester();
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(url);
		request.setVersion("HTTP/1.0");

		HttpTester response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));

		assertNull(response.getMethod());
		// delete gives 302 redirect, so don't check for 200
		//		assertEquals(200,response.getStatus());
		String js = response.getContent();
		return js;
	}



	public ServletTester getTester() {
		return tester;
	}

	public String getContextPath() {
		return contextPath;
	}

	public String getBaseURL() {
		return baseURL;
	}

}
