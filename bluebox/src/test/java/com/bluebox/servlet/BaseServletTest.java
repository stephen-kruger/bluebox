package com.bluebox.servlet;

import java.io.IOException;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BlueBoxServlet;
import com.bluebox.feed.FeedServlet;
import com.bluebox.rest.json.JSONAdminHandler;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public abstract class BaseServletTest extends TestCase {

	private ServletTester tester;
	private String baseURL;
	private String contextPath = "/";
	public ServletHolder bbs, feeds;
//	private int retryCount=5;
	private static final Logger log = LoggerFactory.getLogger(BaseServletTest.class);
	public static final int COUNT = 5;
	public static final String RECIPIENT = "user@there.com";

	@Override
	protected void setUp() throws Exception {
		tester = new ServletTester();
		tester.setContextPath(contextPath);
		//ServletHolder jsp = tester.addServlet(org.apache.jasper.servlet.JspServlet.class, "*.jsp");
		tester.setResourceBase("WebContent");
		bbs = tester.addServlet(BlueBoxServlet.class, "/rest/*");
		feeds = tester.addServlet(FeedServlet.class, "/feed/*");

		baseURL = tester.createSocketConnector(false);

		log.debug("Starting servlets at "+baseURL);
		tester.start();

		while(!feeds.isRunning()||!bbs.isRunning()) {
			log.info("Waiting for servlets to start");
			Thread.sleep(750);
		}
				
		// clear mailboxes
		clearMail();
	}

	public void clearMail() throws IOException, Exception {
		getURL("/"+JSONAdminHandler.JSON_ROOT+"/clear");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.info("Shutting down servlets");
		// clear mailboxes
		getURL("/"+JSONAdminHandler.JSON_ROOT+"/clear");

		log.debug("Stopping servlet");

		try {
			tester.stop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		try {
			bbs.doStop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		try {
			feeds.doStop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		while(feeds.isRunning()||bbs.isRunning()) {
			log.info("Waiting for servlets to stop");
			Thread.sleep(750);
		}
	}

	public Inbox getInbox() {
		try {
			return ((BlueBoxServlet)bbs.getServlet()).getInbox() ;
		} 
		catch (ServletException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getMailCount(BlueboxMessage.State state) {
		String url = "/"+JSONFolderHandler.JSON_ROOT;
		try {
			return getRestJSON(url).getJSONObject(state.name()).getInt("count");
		}
		catch (Throwable t) {
			t.printStackTrace();
			return 0;
		}
	}

	public JSONObject getRestJSON(String url) throws IOException, Exception {
		log.info("Invoking {}",url);
		HttpTester request = new HttpTester();
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(url);
		request.setVersion("HTTP/1.0");

		HttpTester response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));

		assertNull(response.getMethod());
//		while ((response.getStatus()!=200)&&(retryCount-- > 0)) {
//			log.info("Server not ready, rechecking url");
//			return getRestJSON(url);
//		}
		assertEquals(200,response.getStatus());
		String js = response.getContent();
		JSONObject jo = new JSONObject(js);
		return jo;

	}

	public String getURL(String url) throws IOException, Exception {
		HttpTester request = new HttpTester();
		request.setMethod("GET");
		request.setHeader("HOST","127.0.0.1");
		request.setURI(url);
		request.setVersion("HTTP/1.0");

		HttpTester response = new HttpTester();
		response.parse(getTester().getResponses(request.generate()));
//		while ((response.getStatus()!=200)&&(retryCount-- > 0)) {
//			log.info("Server not ready, rechecking url");
//			return getURL(url);
//		}
		//assertNull(response.getMethod());
		// delete gives 302 redirect, so don't check for 200
		assertEquals(200,response.getStatus());
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
