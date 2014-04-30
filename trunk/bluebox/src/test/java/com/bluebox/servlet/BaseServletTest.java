package com.bluebox.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONObject;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

import com.bluebox.Utils;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.smtp.Inbox;

public abstract class BaseServletTest extends TestCase {

	private ServletTester tester;
	private String baseURL;
	private String contextPath = "/";
	//	private BlueBoxSMTPServer smtpServer;
	public ServletHolder bbs;
	public static final Logger log = Logger.getAnonymousLogger();
	public static final int COUNT = 5;
	public static final String RECIPIENT = "user@there.com";

	@Override
	protected void setUp() throws Exception {
		tester = new ServletTester();
		tester.setContextPath(contextPath);
		//ServletHolder jsp = tester.addServlet(org.apache.jasper.servlet.JspServlet.class, "*.jsp");
		bbs = tester.addServlet(com.bluebox.BlueBoxServlet.class, "/rest/*");
		ServletHolder atom = tester.addServlet(com.sun.jersey.spi.container.servlet.ServletContainer.class, "/atom/*");
		atom.setInitParameter("javax.ws.rs.Application", "com.bluebox.feed.ATOMApplication");

		tester.setResourceBase("WebContent");
		//		tester.addServlet(DefaultServlet.class, "/");
		baseURL = tester.createSocketConnector(false);

		log.info("Starting servlets at "+baseURL);
		tester.start();

		// this triggers the servlet to actually start
		getRestJSON("/"+JSONFolderHandler.JSON_ROOT);

		// send some test messages
		Utils.sendSingleMessage(COUNT);

		getRestJSON("/"+JSONFolderHandler.JSON_ROOT);

		//		Thread.sleep(20000);


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
		JSONObject jo = new JSONObject(response.getContent());
		return jo;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.info("Stopping servlet");
		tester.stop();
		Inbox.getInstance().deleteAll();
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
