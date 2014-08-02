package com.bluebox.servlet;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.mail.internet.InternetAddress;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;

public class FeedServletTest extends BaseServletTest {

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFeed() throws Exception {
		String feedURL = getBaseURL()+"/feed/inbox?email=";
		//		Client client = Client.create();
		log.info("Checking URL:"+feedURL);

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
		assertNotNull("No feed title detected",feed.getTitle());
		assertEquals("Missing feed items",COUNT,feed.getEntries().size());
		List<SyndEntry> entries = feed.getEntries();
		for (SyndEntry entry : entries) {
			log.info("Found entry :"+entry.getTitle()+" "+entry.getTitle());
		}

	}

	public void testFeedUpdate() throws Exception {

		// now send a new mail
		String to = "junit@junit.com";
		TestUtils.sendMailSMTP(new InternetAddress("bob@test.com"), new InternetAddress(to), null, null, "feed test", "body");

		Utils.waitFor(1);

		String feedURL = getBaseURL()+"/feed/inbox?email="+to;
		log.info("Checking URL:"+feedURL);

		URL url = new URL(feedURL);
		SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new InputStreamReader(url.openStream()));
        
		assertNotNull("No feed title detected",feed.getTitle());
		assertEquals("Missing feed items",1,feed.getEntries().size());
		List<SyndEntry> entries = feed.getEntries();
		for (SyndEntry entry : entries) {
			log.info("Found entry :"+entry.getTitle());
			assertEquals("Unexpected subject","feed test",entry.getTitle());
		}

	}


}
