package com.bluebox.servlet;

import com.bluebox.BlueBoxServlet;
import com.bluebox.Config;
import com.bluebox.feed.FeedServlet;
import com.bluebox.rest.RestApi;
import com.bluebox.smtp.Inbox;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
//import org.apache.wink.client.ClientConfig;
//import org.apache.wink.client.ClientResponse;
//import org.apache.wink.client.Resource;
//import org.apache.wink.client.RestClient;
//import org.apache.wink.server.internal.servlet.RestServlet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;

public abstract class BaseServletTest extends TestCase {

    public static final int COUNT = 5;
    public static final String RECIPIENT = "user@there.com";
    private static final Logger log = LoggerFactory.getLogger(BaseServletTest.class);
    public ServletHolder bbs, feeds, jaxrs;
    private Server server;
    private String baseURL;
    private final String contextPath = "/";
    private final String webApp = "/";
    private final int PORT = 8090;
    private int retryCount = 10;

    @Override
    protected void setUp() throws Exception {
        log.info("setUp");

//        try {
//            WebAppContext webAppContext = new WebAppContext();
//            webAppContext.setParentLoaderPriority(true);
//            Config config = Config.getInstance();
//            config.setProperty(Config.BLUEBOX_PORT, 2500);
//            server = new Server(PORT);
//            //ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//            webAppContext.setContextPath(contextPath);
//            //context.setContextPath(contextPath);
//            //context.setResourceBase("WebContent");
//            webAppContext.setResourceBase("WebContent");
//            //server.setHandler(webAppContext);
//
//            //bbs = context.addServlet(BlueBoxServlet.class, "/rest/*");
////			feeds = context.addServlet(FeedServlet.class, "/feed/*");
////			jaxrs = context.addServlet(RestServlet.class, "/jaxrs/*");
//            bbs = webAppContext.addServlet(BlueBoxServlet.class.getCanonicalName(), "/rest/*");
//            feeds = webAppContext.addServlet(FeedServlet.class.getCanonicalName(), "/feed/*");
//            log.info("Before");
//            jaxrs = webAppContext.addServlet(RestServlet.class.getCanonicalName(), "/jaxrs/*");
//            log.info("After");
//
//            jaxrs.setInitParameter(RestServlet.APPLICATION_INIT_PARAM, "com.bluebox.rest.RestApi");
//
            baseURL = "http://localhost:" + PORT;
//            log.info("Starting servlets at " + baseURL);
//            server.start();
//            while ((!feeds.isRunning() || !bbs.isRunning() || !jaxrs.isRunning()) && (retryCount-- > 0)) {
//                log.info("Waiting for servlets to start feeds:{} bbs:{} root:{}", feeds.isRunning(), bbs.isRunning(), jaxrs.isRunning());
//                Thread.sleep(750);
//            }
//
//            //TestUtils.addRandomNoThread(Inbox.getInstance(), 10);
//        } catch (Throwable t) {
//            t.printStackTrace();
//        }
    }

    public void clearMail() throws Exception {
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
            if (server!=null) server.stop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            if (bbs!=null) bbs.doStop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            if (feeds!=null) feeds.doStop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            if (jaxrs!=null) jaxrs.doStop();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            while (feeds.isRunning() || bbs.isRunning() || jaxrs.isRunning()) {
                log.info("Waiting for servlets to stop");
                Thread.sleep(750);
            }
        }
        catch (Throwable t) {
            log.info("Problem waiting for servers to stop");
        }
        Inbox.getInstance().stop();
    }

//    public JSONObject getRestJSON(String url) throws Exception {
//        ClientResponse response = getResponse("/jaxrs", url);
//        String js = response.getEntity(String.class);
//        JSONObject jo = new JSONObject(js);
//        return jo;
//    }
//
//    public JSONArray getRestJSONArray(String url) throws Exception {
//        ClientResponse response = getResponse("/jaxrs", url);
//        String js = response.getEntity(String.class);
//        JSONArray jo = new JSONArray(js);
//        return jo;
//    }

    public JSONObject getRestJSONHttpClient(String url) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        RequestBuilder builder = RequestBuilder.get().setUri(baseURL + url)
                .setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        //		if ((args!=null)&&(args.length>1)) {
        //			for (int i = 0; i < args.length; i+=2) {
        //				builder.addParameter(args[i], args[i+1]);
        //			}
        //		}

        HttpUriRequest request = builder.build();

        log.info("Calling {}", request.getRequestLine());
        CloseableHttpResponse response = httpclient.execute(request);
        log.info("Server status: {} with reason : {}", response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
        String resp = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        httpclient.close();

        JSONObject jo = new JSONObject(resp);
        return jo;
    }

//    public ClientResponse getJaxResponse(String url, String acceptType, String mediaType) {
//        return getResponse(RestApi.APPLICATION_PATH, url, acceptType, mediaType);
//    }
//
//    public ClientResponse getResponse(String base, String url) {
//        return getResponse(base, url, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_JSON);
//    }
//
//    public ClientResponse getResponse(String base, String url, String acceptType, String mediaType) {
//        ClientConfig clientConfig = new ClientConfig();
//        RestClient client = new RestClient(clientConfig);
//        Resource resource = client.resource(baseURL + base + url);
//        log.info("Calling endpoint {}", baseURL + base + url);
//        ClientResponse response = resource.contentType(acceptType).accept(mediaType).get();
//        response.consumeContent();
//        assertEquals(200, response.getStatusCode());
//        return response;
//    }

    public String getContextPath() {
        return contextPath;
    }

    public String getBaseURL() {
        return baseURL;
    }

}
