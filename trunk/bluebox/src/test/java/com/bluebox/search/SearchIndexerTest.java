package com.bluebox.search;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import com.bluebox.TestUtils;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class SearchIndexerTest extends TestCase {
	private SearchIndexer si;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		//		Config config = Config.getInstance();
		//		config.setString(Config.BLUEBOX_STORAGE,"com.bluebox.smtp.storage.mongodb.StorageImpl");
		StorageFactory.getInstance().start();
		StorageFactory.getInstance().deleteAll();
		
		si = SearchIndexer.getInstance();
		si.deleteIndexes();
		String recipients = "receiever1@here.com, receiever2@here.com";
		si.addDoc("193398817","receiever1@here.com","sender@there.com","Subject in action","Lucene in Action","<b>Lucene in Action</b>", recipients,"23423","6346543");
		si.addDoc("55320055Z","receiever1@here.com","sender@there.com","Subject for dummies","Lucene for Dummies","<b>Lucene for dummies</b>",  "55320055Z","235324","6346543");
		si.addDoc("55063554A","receiever1@here.com","sender@there.com","Subject for gigabytes", "Managing Gigabytes","<b>stephen</b><i>johnson</i>",  "55063554A","7646","6346543");
		si.addDoc("9900333X","receiever1@here.com","sender@there.com","Subject for Computer Science","The Art of Computer Science","<b>Lucene for Computer Science</b>",  "9900333X","543","6346543");
	}
	
	@Override
	protected void tearDown() throws Exception {
		StorageFactory.getInstance().stop();
	}
	
	public void testHtmlSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",4,si.search("sender",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
	}
	
	public void testSubjectSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",1,si.search("action",SearchIndexer.SearchFields.SUBJECT,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
	}
	
	public void testFromSearch() throws IOException, ParseException {
		assertEquals("Missing expected search results",1,si.search("johnson",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",1,si.search("stephen",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",3,si.search("Lucene in Action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
	}
	
	public void testMailIndexing() throws Exception {
		BlueboxMessage msg = TestUtils.addRandom(StorageFactory.getInstance());
		si.indexMail(msg);
		assertEquals("Missing expected search results",1,si.search(msg.getProperty(BlueboxMessage.SUBJECT),SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",1,si.search("steve",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
	}
	
	public void testDelete() throws IOException, ParseException {
		si.deleteDoc("55063554A");
		assertEquals("Missing expected search results",0,si.search("johnson",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",0,si.search("stephen",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
		assertEquals("Missing expected search results",3,si.search("Lucene in Action",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name()).length);
	}
	
	public void testTextSearch() throws IOException, ParseException {
		Document[] hits = si.search("lucene",SearchIndexer.SearchFields.ANY,0,10,SearchIndexer.SearchFields.SUBJECT.name());
		assertEquals("Missing expected search results",3,hits.length);
//		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
			System.out.println((i + 1) + ". " + hits[i].get(SearchIndexer.SearchFields.UID.name()));
		}
	}
	
	public void testHtmlConvert() throws IOException {
		String htmlStr = "<html><title>title text</title><body>this is the body</body></html>";
		String textStr = SearchIndexer.htmlToString(htmlStr);
		assertEquals("Html to text conversion failed","title text this is the body",textStr);
	}
}
