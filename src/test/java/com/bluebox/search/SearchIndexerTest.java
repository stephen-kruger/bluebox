package com.bluebox.search;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import junit.framework.TestCase;
import org.codehaus.jettison.json.JSONArray;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public class SearchIndexerTest extends TestCase {
    private static final Logger log = Logger.getAnonymousLogger();
    private String uid1, rawid1;
    private String uid2, rawid2;
    private String uid3, rawid3;
    private String uid4, rawid4;


    @Override
    protected void setUp() throws Exception {
        getSearchIndexer().deleteIndexes();
        MimeMessage mm;
        mm = Utils.createMessage(null, "sender1@there.com", "receiever1@here.com", null, null, "Lucene in action", "Lucene in action", "<b>Lucene in action</b>");
        rawid1 = Utils.spoolStream(StorageFactory.getInstance(), mm);
        uid1 = StorageFactory.getInstance().store("sender1@there.com", new InboxAddress("receiever1@here.com"), new Date(), mm, rawid1).getIdentifier();
        getSearchIndexer().addDoc(uid1, "receiever1@here.com", "[sender1@there.com]", mm.getSubject(), "Lucene in action", "<b>Lucene in action</b>", "receiever1@here.com", 23423, 6346543, false);

        mm = Utils.createMessage(null, "sender2@there.com", "receiever3@here.com", null, null, "Lucene for dummies", "Lucene for dummies", "<b>Lucene for dummies</b>");
        rawid2 = Utils.spoolStream(StorageFactory.getInstance(), mm);
        uid2 = StorageFactory.getInstance().store("sender2@there.com", new InboxAddress("receiever2@here.com"), new Date(), mm, rawid2).getIdentifier();
        getSearchIndexer().addDoc(uid2, "receiever2@here.com", "[sender2@there.com]", mm.getSubject(), "Lucene for dummies", "<b>Lucene for dummies</b>", "receiever2@here.com", 235324, 6346543, false);

        mm = Utils.createMessage(null, "sender3@there.com", "receiever3@here.com", null, null, "Managing Gigabytes", "Managing Gigabytes", "<b>Managing Gigabytes</b>");
        rawid3 = Utils.spoolStream(StorageFactory.getInstance(), mm);
        uid3 = StorageFactory.getInstance().store("sender3@there.com", new InboxAddress("receiever3@here.com"), new Date(), mm, rawid3).getIdentifier();
        getSearchIndexer().addDoc(uid3, "receiever3@here.com", "[sender3@there.com]", mm.getSubject(), "Managing Gigabytes", "<b>Managing Gigabytes</b>", "receiever3@here.com", 7646, 6346543, false);

        mm = Utils.createMessage(null, "sender4@there.com", "receiever4@here.com", null, null, "Subject for Computer Science", "The Art of Computer Science", "<b>The Art of Computer Science</b>");
        rawid4 = Utils.spoolStream(StorageFactory.getInstance(), mm);
        uid4 = StorageFactory.getInstance().store("sender4@there.com", new InboxAddress("receiever4@here.com"), new Date(), mm, rawid4).getIdentifier();
        getSearchIndexer().addDoc(uid4, "receiever4@here.com", "[sender4@there.com]", mm.getSubject(), "The Art of Computer Science", "<b>The Art of Computer Science</b>", "receiever4@here.com", 543, 6346543, false);

        String uid;
        for (int i = 0; i < 50; i++) {
            mm = Utils.createMessage(null, "xxx@xxx.com", "xxx@xxx.com", null, null, "ttttttttttttttttttttttttttt", "tttttttttttttttttttttttttt", "tttttttttttttttttttttttttttt");
            uid = Utils.spoolStream(StorageFactory.getInstance(), mm);
            uid = StorageFactory.getInstance().store("xxx@xxx.com", new InboxAddress("xxx@xxx.com"), new Date(), mm, uid).getIdentifier();

            getSearchIndexer().addDoc(uid, "xxx@xxx.com", "[xxx@xxx.com]", "ttttttttttttttttttttttttttt", "tttttttttttttttttttttttttt", "tttttttttttttttttttttttttttt", "xxx@xxx.com", 543, 6346543, false);
        }
        getSearchIndexer().commit(true);
    }

    @Override
    protected void tearDown() throws Exception {
        getSearchIndexer().deleteIndexes();
        getSearchIndexer().stop();
        StorageFactory.getInstance().deleteAll();
    }

    public SearchIf getSearchIndexer() throws Exception {
        return SearchFactory.getInstance();
    }

    @Test
    public void testHtmlSearch() throws Exception {
        assertEquals("Missing expected search results", 4, getSearchIndexer().search("sender", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testMultiWord() throws Exception {
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Art of Computer", SearchUtils.SearchFields.BODY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("for dummies", SearchUtils.SearchFields.SUBJECT, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("in action", SearchUtils.SearchFields.SUBJECT, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.SUBJECT, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.BODY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testSubjectSearch() throws Exception {
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.SUBJECT, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("action", SearchUtils.SearchFields.SUBJECT, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("action", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testFromSearch() throws Exception {
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("sender1", SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
//		assertEquals("Missing expected search results",1,getSearchIndexer().search("johnson",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SortFields.SORT_RECEIVED,false).length);
//		assertEquals("Missing expected search results",1,getSearchIndexer().search("stephen",SearchUtils.SearchFields.ANY,0,10,SearchUtils.SortFields.SORT_RECEIVED,false).length);
    }

    @Test
    public void testTextBodySearch() throws Exception {
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.BODY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.TEXT_BODY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.HTML_BODY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("Lucene in action", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testRecipientSearch() throws Exception {
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("receiever1", SearchUtils.SearchFields.RECIPIENTS, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("receiever1@here.com", SearchUtils.SearchFields.RECIPIENTS, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("receiever2", SearchUtils.SearchFields.RECIPIENTS, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        //		assertEquals("Missing expected search results",4,getSearchIndexer().search("receiever",SearchUtils.SearchFields.RECIPIENTS,0,10,SearchUtils.SortFields.SORT_RECEIVED,false).length);
    }

    @Test
    public void testMailIndexing() throws Exception {
        getSearchIndexer().deleteIndexes();
        StorageFactory.getInstance().deleteAll();

        BlueboxMessage msg = TestUtils.addRandomDirect(StorageFactory.getInstance());
        getSearchIndexer().indexMail(msg, true);
        //		assertEquals("Missing expected search results",1,getSearchIndexer().search(SearchUtils.substringQuery(msg.getSubject()),SearchUtils.SearchFields.ANY,0,10,SearchUtils.SortFields.SORT_RECEIVED,false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("steve", SearchUtils.SearchFields.INBOX, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search(SearchUtils.plainQuery(msg.getInbox().toString()), SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testDelete() throws Exception {
        getSearchIndexer().deleteDoc(uid1);
        StorageFactory.getInstance().delete(uid1, rawid1);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("sender2", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("sender3", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 0, getSearchIndexer().search("Lucene in Action", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 0, getSearchIndexer().search("sender1@there.com", SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Missing expected search results", 1, getSearchIndexer().search("sender2@there.com", SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        getSearchIndexer().deleteDoc(uid2);
        StorageFactory.getInstance().delete(uid2, rawid2);
        assertEquals("Missing expected search results", 0, getSearchIndexer().search("sender2@there.com", SearchUtils.SearchFields.FROM, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }

    @Test
    public void testTextSearch() throws Exception {
        Object[] hits = getSearchIndexer().search("Lucene", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false);
        assertEquals("Missing expected search results", 2, hits.length);
    }

    @Test
    public void testHtmlConvert() throws IOException {
        String htmlStr = "<html><title>title text</title><body>this is the body</body></html>";
        String textStr = SearchUtils.htmlToString(htmlStr);
        assertEquals("Html to text conversion failed", "title text this is the body", textStr);
    }

    @Test
    public void testTypeAhead() throws Exception {
        Object[] results = getSearchIndexer().search(SearchUtils.autocompleteQuery("receiever"), SearchUtils.SearchFields.RECIPIENTS, 0, 199, SearchUtils.SortFields.SORT_RECEIVED, false);
        assertTrue("Missing autocomplete results", results.length == 4);
        results = getSearchIndexer().search(SearchUtils.autocompleteQuery("receiever1"), SearchUtils.SearchFields.RECIPIENTS, 0, 199, SearchUtils.SortFields.SORT_RECEIVED, false);
        assertTrue("Missing autocomplete results", results.length > 0);
    }

    @Test
    public void testSearch() throws Exception {
        StringWriter sw;
        JSONArray ja;

        // test search in from field
        sw = new StringWriter();
        String searchString = "sender";
        log.info("Looking for sender " + searchString);
        getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.FROM, SearchUtils.SortFields.SORT_RECEIVED, true);
        ja = new JSONArray(sw.toString());
        log.info(ja.toString(3));
        assertTrue("No 'sender' found in search results", ja.length() > 0);

        // test search in subject
        sw = new StringWriter();
        searchString = "Managing Gigabytes";
        getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SortFields.SORT_RECEIVED, true);
        ja = new JSONArray(sw.toString());
        assertTrue("Missing search results", ja.length() > 0);
        assertEquals(ja.getJSONObject(0).get(BlueboxMessage.SUBJECT), searchString);

        // search for last few chars of subject
        sw = new StringWriter();
        searchString = "Gigabytes";
        getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SortFields.SORT_RECEIVED, true);
        ja = new JSONArray(sw.toString());
        log.info(searchString + "=" + ja.toString(3));
        assertTrue("Missing search results", ja.length() > 0);
        assertEquals("Managing Gigabytes", ja.getJSONObject(0).get(BlueboxMessage.SUBJECT));

        // search for first few chars of subject
        sw = new StringWriter();
        searchString = "Managing";
        getSearchIndexer().searchInboxes(searchString, sw, 0, 50, SearchUtils.SearchFields.SUBJECT, SearchUtils.SortFields.SORT_RECEIVED, true);
        ja = new JSONArray(sw.toString());
        log.fine(searchString + "=" + ja.toString(3));
        assertTrue("Missing search results", ja.length() > 0);
        for (int i = 0; i < ja.length(); i++) {
            assertTrue("Inaccurate search result found", ja.getJSONObject(i).get(BlueboxMessage.SUBJECT).toString().contains("Managing"));
        }

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

    @Test
    public void testContains() throws Exception {
        assertTrue("Did not find document by UID", getSearchIndexer().containsUid(uid1));
        assertTrue("Did not find document by UID", getSearchIndexer().containsUid(uid2));
        assertTrue("Did not find document by UID", getSearchIndexer().containsUid(uid3));
        assertTrue("Did not find document by UID", getSearchIndexer().containsUid(uid4));
        assertFalse("Unexpected UID found", getSearchIndexer().containsUid(UUID.randomUUID().toString()));
        getSearchIndexer().deleteDoc(uid1);
        StorageFactory.getInstance().delete(uid1, rawid1);
        assertFalse("Should not find deleted document by UID", getSearchIndexer().containsUid(uid1));
    }

    @Test
    public void testSearchPaging() throws Exception {
        assertEquals("Search did not limit results", 10, getSearchIndexer().search("", SearchUtils.SearchFields.ANY, 0, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Search did not limit results", 10, getSearchIndexer().search("", SearchUtils.SearchFields.ANY, 10, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
        assertEquals("Should not return results here", 0, getSearchIndexer().search("", SearchUtils.SearchFields.ANY, 1000, 10, SearchUtils.SortFields.SORT_RECEIVED, false).length);
    }
}
