package com.bluebox.feed;

import com.bluebox.Config;
import com.bluebox.FakeServletRequest;
import com.bluebox.TestUtils;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import jakarta.servlet.UnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class FeedServletTest extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(FeedServletTest.class);
    private Inbox inbox;
    private BlueBoxSMTPServer smtpServer;
    private BlueboxMessageHandlerFactory bbmhf;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TestUtils.addRandomDirect(StorageFactory.getInstance(), COUNT);
        TestUtils.waitFor(Inbox.getInstance(), COUNT);
        // new
        Config config = Config.getInstance();
        config.setProperty(Config.BLUEBOX_PORT, 2500);
        inbox = Inbox.getInstance();
        smtpServer = BlueBoxSMTPServer.getInstance(bbmhf = new BlueboxMessageHandlerFactory(inbox));
        smtpServer.start();
        inbox.deleteAll();
        // end new
    }

    public void testMockito() throws FeedException, IOException, UnavailableException {

        FeedServlet fs = new FeedServlet();
        FakeServletRequest r =  new FakeServletRequest();
        r.setMethod("GET ");
        r.setContextPath("/feed");
        r.setAttribute("email","steve@here.com");
        r.setParameter("email",new String[]{"steve@here.com"});
        assertNotNull("Attribute not set", r.getAttribute("email"));
        assertNotNull("Parameter not set", r.getParameter("email"));
        fs.getFeed(inbox, r);
    }

    public void testFeed() throws Exception {
        String feedURL = getBaseURL() + "/feed/inbox?email=";
        //		Client client = Client.create();
        log.info("Checking URL:" + feedURL);

        URL url = new URL(feedURL);
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new InputStreamReader(url.openStream()));

//		Parser parser = new Abdera().getParser();
//		InputStream is = url.openStream();
//		Document<Feed> doc = parser.parse(is, url.toString());
//		Feed feed = doc.getRoot();
        //WebResource webResource = client.resource(feedURL);
        //MultivaluedMap<String, String> queryMap = new MultivaluedMapImpl();
        //String s = webResource.queryParams(queryMap).get(String.class);
        assertNotNull("No feed title detected", feed.getTitle());
        assertEquals("Missing feed items", COUNT, feed.getEntries().size());
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            log.info("Found entry :" + entry.getTitle() + " " + entry.getTitle());
        }

    }

    public void testFeedUpdate() throws Exception {
// TODO - fixme
//		// now send a new mail
//		String to = "junit@junit.com";
//		TestUtils.sendMailSMTP(new InternetAddress("bob@test.com"), new InternetAddress(to), null, null, "feed test", "body");
//
//		TestUtils.waitFor(Inbox.getInstance(),1);
//
//		String feedURL = getBaseURL()+"/feed/inbox?email="+to;
//		log.info("Checking URL:"+feedURL);
//
//		URL url = new URL(feedURL);
//		SyndFeedInput input = new SyndFeedInput();
//        SyndFeed feed = input.build(new InputStreamReader(url.openStream()));
//        
//		assertNotNull("No feed title detected",feed.getTitle());
//		assertEquals("Missing feed items",1,feed.getEntries().size());
//		List<SyndEntry> entries = feed.getEntries();
//		for (SyndEntry entry : entries) {
//			log.info("Found entry :"+entry.getTitle());
//			assertEquals("Unexpected subject","feed test",entry.getTitle());
//		}

    }


}
