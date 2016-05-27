package com.bluebox.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.server.internal.servlet.RestServlet;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BlueBoxServlet;
import com.bluebox.TestUtils;
import com.bluebox.feed.FeedServlet;
import com.bluebox.smtp.Inbox;

import junit.framework.TestCase;

public abstract class BaseServletTest extends TestCase {

	private Server server;
	private String baseURL;
	private String contextPath = "/";
	public ServletHolder bbs, feeds, jaxrs;
	private int retryCount=10;
	private static final Logger log = LoggerFactory.getLogger(BaseServletTest.class);
	public static final int COUNT = 5;
	public static final String RECIPIENT = "user@there.com";

	@Override
	protected void setUp() throws Exception {
		server = new Server(8080);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);
		context.setResourceBase("WebContent");
		server.setHandler(context);

		bbs = context.addServlet(BlueBoxServlet.class, "/rest/*");
		feeds = context.addServlet(FeedServlet.class, "/feed/*");
		jaxrs = context.addServlet(RestServlet.class, "/jaxrs/*");
		jaxrs.setInitParameter(RestServlet.APPLICATION_INIT_PARAM, "com.bluebox.rest.RestApi");

		baseURL = "http://0.0.0.0:8080";
		log.info("Starting servlets at "+baseURL);
		server.start();
		while((!feeds.isRunning()||!bbs.isRunning()||!jaxrs.isRunning())&&(retryCount-->0)) {
			log.info("Waiting for servlets to start feeds:{} bbs:{} root:{}",feeds.isRunning(),bbs.isRunning(),jaxrs.isRunning());
			Thread.sleep(750);
		}

		//TestUtils.addRandomNoThread(Inbox.getInstance(), 10);
	}

	public void clearMail() throws IOException, Exception {
		Inbox.getInstance().deleteAll();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.info("Shutting down servlets");
		// clear mailboxes
		clearMail();

		log.debug("Stopping servlet");

		try {
			server.stop();
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
		try {
			jaxrs.doStop();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		while(feeds.isRunning()||bbs.isRunning()||jaxrs.isRunning()) {
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

	public JSONObject getRestJSON(String url) throws IOException, Exception {
		ClientResponse response = getResponse("/jaxrs",url);
		String js = response.getEntity(String.class);
		JSONObject jo = new JSONObject(js);
		return jo;
	}
	
	public JSONArray getRestJSONArray(String url) throws IOException, Exception {
		ClientResponse response = getResponse("/jaxrs",url);
		String js = response.getEntity(String.class);
		JSONArray jo = new JSONArray(js);
		return jo;
	}

	public JSONObject getRestJSONHttpClient(String url) throws IOException, Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		RequestBuilder builder = RequestBuilder.get().setUri(baseURL+url)
				.setHeader(HttpHeaders.ACCEPT,  MediaType.APPLICATION_JSON)
				.setHeader(HttpHeaders.CONTENT_TYPE,  MediaType.APPLICATION_FORM_URLENCODED);

//		if ((args!=null)&&(args.length>1)) {
//			for (int i = 0; i < args.length; i+=2) {
//				builder.addParameter(args[i], args[i+1]);
//			}
//		}

		HttpUriRequest request = builder.build();

		log.info("Calling {}",request.getRequestLine());
		CloseableHttpResponse response = httpclient.execute(request);
		log.info("Server status: {} with reason : {}",response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase());
		String resp = IOUtils.toString(response.getEntity().getContent(),"UTF-8");
		httpclient.close();

		JSONObject jo = new JSONObject(resp);
		return jo;
	}

	public ClientResponse getResponse(String base, String url) {
		return getResponse(base,url,MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON);
	}
	
	public ClientResponse getResponse(String base, String url, String acceptType, String mediaType) {
		ClientConfig clientConfig = new ClientConfig();
		RestClient client = new RestClient(clientConfig);
		Resource resource = client.resource(baseURL+base+url);
		log.info("Calling endpoint {}",baseURL+base+url);
		ClientResponse response = resource.contentType(acceptType).accept(mediaType).get();
		assertEquals(200,response.getStatusCode());
		return response;
	}

	public String getContextPath() {
		return contextPath;
	}

	public String getBaseURL() {
		return baseURL;
	}

}
