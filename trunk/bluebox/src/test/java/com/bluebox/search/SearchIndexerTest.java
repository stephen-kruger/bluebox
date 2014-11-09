package com.bluebox.search;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.bluebox.TestUtils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class SearchIndexerTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private SearchIndexer si;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		si = SearchIndexer.getInstance();
		si.deleteIndexes();
		si.addDoc("193398817","receiever1@here.com","sender@there.com","Subject in action","Lucene in Action","<b>Lucene in Action</b>", "receiever1@here.com",23423,6346543);
		si.addDoc("55320055Z","receiever2@here.com","sender@there.com","Subject for dummies","Lucene for Dummies","<b>Lucene for dummies</b>",  "receiever2@here.com",235324,6346543);
		si.addDoc("55063554A","receiever3@here.com","sender@there.com","Subject for gigabytes", "Managing Gigabytes","<b>stephen</b><i>johnson</i>",  "receiever3@here.com",7646,6346543);
		si.addDoc("9900333X","receiever4@here.com","sender@there.com","Subject for Computer Science","The Art of Computer Science","<b>Lucene for Computer Science</b>",  "receiever4@here.com",543,6346543);
	}

	@Override
	protected void tearDown() throws Exception {
		si.stop();
	}

	public void testHtmlSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",4,si.search("sender",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
	}

	public void testMultiWord() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("Art of Computer",SearchIndexer.SearchFields.BODY,0,10,SearchIndexer.SearchFields.BODY,false).length);
		assertEquals("Missing expected search results",1,si.search("for dummies",SearchIndexer.SearchFields.SUBJECT,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("in action",SearchIndexer.SearchFields.SUBJECT,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("Subject in action",SearchIndexer.SearchFields.SUBJECT,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",0,si.search("Subject in action",SearchIndexer.SearchFields.BODY,0,10,SearchIndexer.SearchFields.BODY,false).length);
	}

	public void testSubjectSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("action",SearchIndexer.SearchFields.SUBJECT,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
	}

	public void testFromSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("johnson",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("stephen",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("Lucene in Action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
	}

	public void testRecipientSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("receiever1",SearchIndexer.SearchFields.RECIPIENTS,0,10,SearchIndexer.SearchFields.RECEIVED,false).length);
		assertEquals("Missing expected search results",1,si.search("receiever1@here.com",SearchIndexer.SearchFields.RECIPIENTS,0,10,SearchIndexer.SearchFields.RECEIVED,false).length);
		assertEquals("Missing expected search results",1,si.search("receiever2",SearchIndexer.SearchFields.RECIPIENTS,0,10,SearchIndexer.SearchFields.RECEIVED,false).length);
		//		assertEquals("Missing expected search results",4,si.search("receiever",SearchIndexer.SearchFields.RECIPIENTS,0,10,SearchIndexer.SearchFields.RECEIVED,false).length);
	}

	public void testMailIndexing() throws Exception {
		Inbox.getInstance();
		BlueboxMessage msg = TestUtils.addRandom(StorageFactory.getInstance());
		si.indexMail(msg);
		assertEquals("Missing expected search results",1,si.search(msg.getSubject(),SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("steve",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		Inbox.getInstance().stop();
	}

	public void testDelete() throws IOException, ParseException, InterruptedException {
		si.deleteDoc("55063554A");
		assertEquals("Missing expected search results",0,si.search("johnson",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",0,si.search("stephen",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,si.search("Lucene in Action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false).length);
	}

	public void testTextSearch() throws IOException, ParseException {
		Document[] hits = si.search("lucene",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT,false);
		assertEquals("Missing expected search results",3,hits.length);
		for(int i=0;i<hits.length;++i) {
			log.info((i + 1) + ". " + hits[i].get(SearchIndexer.SearchFields.UID.name()));
		}
	}

	public void testHtmlConvert() throws IOException {
		String htmlStr = "<html><title>title text</title><body>this is the body</body></html>";
		String textStr = SearchIndexer.htmlToString(htmlStr);
		assertEquals("Html to text conversion failed","title text this is the body",textStr);
	}

	public void testTypeAhead() throws ParseException, IOException {
		Document[] results = si.search("receiever*", SearchIndexer.SearchFields.RECIPIENTS, 0, 199, SearchIndexer.SearchFields.RECEIVED,false);
		assertTrue("Missing autocomplete results",results.length==4);
		results = si.search("receiever1*", SearchIndexer.SearchFields.RECIPIENTS, 0, 199, SearchIndexer.SearchFields.RECEIVED,false);
		assertTrue("Missing autocomplete results",results.length>0);
	}

	@Test
	public void testSearch() throws Exception {
		StringWriter sw;
		JSONArray ja;

		// test search in from field
		sw = new StringWriter();
		String searchString = "sender";
		log.info("Looking for sender "+searchString);
		SearchIndexer.getInstance().searchInboxes(searchString, sw, 0, 50, SearchIndexer.SearchFields.FROM, SearchIndexer.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		assertTrue("No 'Subject' found in search results",ja.length()>0);

		// test search in subject
		sw = new StringWriter();
		searchString = "Subject for gigabytes";
		SearchIndexer.getInstance().searchInboxes(searchString, sw, 0, 50, SearchIndexer.SearchFields.SUBJECT, SearchIndexer.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		assertTrue("Missing search results",ja.length()>0);
		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),"Subject for gigabytes");

		// search for last few chars of subject
		sw = new StringWriter();
		searchString = "gigabytes";
		SearchIndexer.getInstance().searchInboxes(searchString, sw, 0, 50, SearchIndexer.SearchFields.SUBJECT, SearchIndexer.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		log.info(searchString+"="+ja.toString(3));
		assertTrue("Missing search results",ja.length()>0);
		assertEquals("Subject for gigabytes",ja.getJSONObject(0).get(BlueboxMessage.SUBJECT));

		// search for first few chars of subject
		sw = new StringWriter();
		searchString = "Subject in";
		SearchIndexer.getInstance().searchInboxes(searchString, sw, 0, 50, SearchIndexer.SearchFields.SUBJECT, SearchIndexer.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		log.info(searchString+"="+ja.toString(3));
		assertTrue("Missing search results",ja.length()>0);
		assertEquals("Subject in action",ja.getJSONObject(0).get(BlueboxMessage.SUBJECT));

		// test search To:
		//		sw = new StringWriter();
		//		SearchIndexer.getInstance().searchInboxes(original.getProperty(BlueboxMessage.FROM), sw, 0, 50, SearchIndexer.SearchFields.FROM,SearchIndexer.SearchFields.FROM,true);
		//		ja = new JSONArray(sw.toString());
		//		log.info(ja.toString(3));
		//		assertTrue("No 'From' search results",ja.length()>0);
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),original.getSubject());
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.FROM),original.getProperty(BlueboxMessage.FROM));
		//
		//		// test substring search
		//		sw = new StringWriter();
		//		SearchIndexer.getInstance().searchInboxes("steve", sw, 0, 50, SearchIndexer.SearchFields.FROM, null, true);
		//		ja = new JSONArray(sw.toString());
		//		log.info(ja.toString(3));
		//		assertTrue("No substring search results",ja.length()>0);
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),original.getSubject());
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.FROM),original.getProperty(BlueboxMessage.FROM));
	}
}
