package com.bluebox.search;

import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class SearchIndexerTest extends BaseTestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getSearchIndexer().deleteIndexes();
		getSearchIndexer().addDoc("193398817","receiever1@here.com","sender1@there.com","Subject in action","Lucene in Action","<b>Lucene in Action</b>", "receiever1@here.com",23423,6346543,false);
		getSearchIndexer().addDoc("55320055Z","receiever2@here.com","sender2@there.com","Subject for dummies","Lucene for Dummies","<b>Lucene for dummies</b>",  "receiever2@here.com",235324,6346543,false);
		getSearchIndexer().addDoc("55063554A","receiever3@here.com","sender3@there.com","Subject for gigabytes", "Managing Gigabytes","<b>stephen</b><i>johnson</i>",  "receiever3@here.com",7646,6346543,false);
		getSearchIndexer().addDoc("9900333X","receiever4@here.com","sender4@there.com","Subject for Computer Science","The Art of Computer Science","<b>Lucene for Computer Science</b>",  "receiever4@here.com",543,6346543,false);
		getSearchIndexer().commit();
	}

	@Test
	public void testHtmlSearch() throws IOException, Exception {
		assertEquals("Missing expected search results",4,getSearchIndexer().search("sender",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.RECEIVED,false).length);
	}

	@Test
	public void testMultiWord() throws IOException, Exception {
		assertEquals("Missing expected search results",1,getSearchIndexer().search("Art of Computer",SearchUtils.SearchFields.BODY,0,10,SearchUtils.SearchFields.BODY,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("for dummies",SearchUtils.SearchFields.SUBJECT,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("in action",SearchUtils.SearchFields.SUBJECT,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("Subject in action",SearchUtils.SearchFields.SUBJECT,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",0,getSearchIndexer().search("Subject in action",SearchUtils.SearchFields.BODY,0,10,SearchUtils.SearchFields.BODY,false).length);
	}

	@Test
	public void testSubjectSearch() throws IOException, Exception {
		assertEquals("Missing expected search results",1,getSearchIndexer().search("action",SearchUtils.SearchFields.SUBJECT,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("action",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
	}

	@Test
	public void testFromSearch() throws IOException, Exception {
		assertEquals("Missing expected search results",1,getSearchIndexer().search("johnson",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("stephen",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("Lucene in Action",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
	}

	@Test
	public void testRecipientSearch() throws IOException, Exception {
		assertEquals("Missing expected search results",1,getSearchIndexer().search("receiever1",SearchUtils.SearchFields.RECIPIENTS,0,10,SearchUtils.SearchFields.RECEIVED,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("receiever1@here.com",SearchUtils.SearchFields.RECIPIENTS,0,10,SearchUtils.SearchFields.RECEIVED,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("receiever2",SearchUtils.SearchFields.RECIPIENTS,0,10,SearchUtils.SearchFields.RECEIVED,false).length);
		//		assertEquals("Missing expected search results",4,getSearchIndexer().search("receiever",SearchUtils.SearchFields.RECIPIENTS,0,10,SearchUtils.SearchFields.RECEIVED,false).length);
	}

	@Test
	public void testMailIndexing() throws Exception {
		BlueboxMessage msg = TestUtils.addRandomDirect(StorageFactory.getInstance());
		getSearchIndexer().indexMail(msg,true);
		assertEquals("Missing expected search results",1,getSearchIndexer().search(msg.getSubject(),SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("steve",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
	}

	@Test
	public void testDelete() throws IOException, Exception, InterruptedException, SolrServerException {
		getSearchIndexer().deleteDoc("193398817");
		assertEquals("Missing expected search results",1,getSearchIndexer().search("johnson",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("stephen",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",0,getSearchIndexer().search("Lucene in Action",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",0,getSearchIndexer().search("sender1@there.com",SearchUtils.SearchFields.FROM,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		assertEquals("Missing expected search results",1,getSearchIndexer().search("sender2@there.com",SearchUtils.SearchFields.FROM,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
		getSearchIndexer().deleteDoc("55320055Z");
		assertEquals("Missing expected search results",0,getSearchIndexer().search("sender2@there.com",SearchUtils.SearchFields.FROM,0,10,SearchUtils.SearchFields.SUBJECT,false).length);
	}

	@Test
	public void testTextSearch() throws IOException, Exception {
		Object[] hits = getSearchIndexer().search("lucene",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SearchFields.SUBJECT,false);
		assertEquals("Missing expected search results",3,hits.length);
//		for(int i=0;i<hits.length;++i) {
//			log.info((i + 1) + ". " + hits[i].get(SearchUtils.SearchFields.UID.name()));
//		}
	}

	@Test
	public void testHtmlConvert() throws IOException {
		String htmlStr = "<html><title>title text</title><body>this is the body</body></html>";
		String textStr = SearchUtils.htmlToString(htmlStr);
		assertEquals("Html to text conversion failed","title text this is the body",textStr);
	}

	@Test
	public void testTypeAhead() throws Exception, IOException {
		Object[] results = getSearchIndexer().search("receiever", SearchUtils.SearchFields.RECIPIENTS, 0, 199, SearchUtils.SearchFields.RECEIVED,false);
		assertTrue("Missing autocomplete results",results.length==4);
		results = getSearchIndexer().search("receiever1", SearchUtils.SearchFields.RECIPIENTS, 0, 199, SearchUtils.SearchFields.RECEIVED,false);
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
		getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.FROM, SearchUtils.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		log.info(ja.toString(3));
		assertTrue("No 'Subject' found in search results",ja.length()>0);

		// test search in subject
		sw = new StringWriter();
		searchString = "Subject for gigabytes";
		getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		assertTrue("Missing search results",ja.length()>0);
		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),"Subject for gigabytes");

		// search for last few chars of subject
		sw = new StringWriter();
		searchString = "gigabytes";
		getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		log.info(searchString+"="+ja.toString(3));
		assertTrue("Missing search results",ja.length()>0);
		assertEquals("Subject for gigabytes",ja.getJSONObject(0).get(BlueboxMessage.SUBJECT));

		// search for first few chars of subject
		sw = new StringWriter();
		searchString = "Subject in";
		getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SearchFields.RECEIVED, true);
		ja = new JSONArray(sw.toString());
		log.info(searchString+"="+ja.toString(3));
		assertTrue("Missing search results",ja.length()>0);
		assertEquals("Subject in action",ja.getJSONObject(0).get(BlueboxMessage.SUBJECT));

		// test search To:
		//		sw = new StringWriter();
		//		getSearchIndexer().searchInboxes(original.getProperty(BlueboxMessage.FROM), sw, 0, 50, SearchUtils.SearchFields.FROM,SearchUtils.SearchFields.FROM,true);
		//		ja = new JSONArray(sw.toString());
		//		log.info(ja.toString(3));
		//		assertTrue("No 'From' search results",ja.length()>0);
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),original.getSubject());
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.FROM),original.getProperty(BlueboxMessage.FROM));
		//
		//		// test substring search
		//		sw = new StringWriter();
		//		getSearchIndexer().searchInboxes("steve", sw, 0, 50, SearchUtils.SearchFields.FROM, null, true);
		//		ja = new JSONArray(sw.toString());
		//		log.info(ja.toString(3));
		//		assertTrue("No substring search results",ja.length()>0);
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT),original.getSubject());
		//		assertEquals(ja.getJSONObject(0).get(BlueboxMessage.FROM),original.getProperty(BlueboxMessage.FROM));
	}
}
