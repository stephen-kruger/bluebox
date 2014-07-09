package com.bluebox.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.ParseException;
import org.apache.abdera.parser.Parser;
import org.codehaus.jettison.json.JSONException;

import com.bluebox.Utils;

public class AtomTest extends BaseServletTest {

	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAtom() throws JSONException, ParseException, IOException, AddressException, MessagingException, InterruptedException {
		String atomURL = getBaseURL()+"/atom/inbox?email=";
		//		Client client = Client.create();
		log.info("Checking URL:"+atomURL);

		URL url = new URL(atomURL);
		Parser parser = new Abdera().getParser();
		InputStream is = url.openStream();
		Document<Feed> doc = parser.parse(is, url.toString());
		Feed feed = doc.getRoot();
		//WebResource webResource = client.resource(atomURL);
		//MultivaluedMap<String, String> queryMap = new MultivaluedMapImpl();
		//String s = webResource.queryParams(queryMap).get(String.class);
		assertNotNull("No feed title detected",feed.getTitle());
		assertEquals("Missing feed items",COUNT,feed.getEntries().size());
		List<Entry> entries = feed.getEntries();
		for (Entry entry : entries) {
			log.info("Found entry :"+entry.getTitle()+" "+entry.getId().toASCIIString());
		}

	}

	public void testAtomUpdate() throws JSONException, ParseException, IOException, AddressException, MessagingException, InterruptedException {

		// now send a new mail
		InternetAddress[] blank =  new InternetAddress[]{};
		String to = "junit@junit.com";
		Utils.sendMessage(new InternetAddress("bob@test.com"), "atom test", "body", new InternetAddress[]{new InternetAddress(to)}, blank, blank, false);
		Thread.sleep(1000);

		String atomURL = getBaseURL()+"/atom/inbox?email="+to;
		log.info("Checking URL:"+atomURL);

		URL url = new URL(atomURL);
		Parser parser = new Abdera().getParser();
		InputStream is = url.openStream();
		Document<Feed> doc = parser.parse(is, url.toString());
		Feed feed = doc.getRoot();

		assertNotNull("No feed title detected",feed.getTitle());
		assertEquals("Missing feed items",1,feed.getEntries().size());
		List<Entry> entries = feed.getEntries();
		for (Entry entry : entries) {
			log.info("Found entry :"+entry.getTitle()+" "+entry.getId().toASCIIString());
			assertEquals("Unexpected subject","atom test",entry.getTitle());
		}

	}


}
