package com.bluebox.smtp.storage;

import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.mail.internet.InternetAddress;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.search.SearchIndexer;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;

public class StorageTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();
	private int SIZE = 20;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// this triggers the storage to be created too, and started
		Inbox.getInstance();
		log.info("Cleaning up messages to start tests");
		StorageFactory.getInstance().logErrorClear();
		try {
			StorageFactory.getInstance().deleteAll();
		}
		catch (Throwable t) {
			log.warning("Tables not created");
		}
		Inbox.getInstance().rebuildSearchIndexes();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		log.info("Cleaning up messages after tests");
		try {
			StorageFactory.getInstance().deleteAll();
		}
		catch (Throwable t) {
			log.warning("Tables not created");
		}
		
		Inbox.getInstance().stop();
	}

	@Test
	public void testAutoComplete2() throws Exception {
		String email = "\"First Name\" <stephen.johnson@mail.com>";
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(null,
				Utils.getRandomAddress(), 
				new InternetAddress[]{new InternetAddress(email)}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr", 
				true);
		InboxAddress ia = new InboxAddress(email);
		BlueboxMessage m1 = StorageFactory.getInstance().store(ia, email, message);
		SearchIndexer.getInstance().indexMail(m1);

		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("First Name*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("ste*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("ste*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("Ste*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("joh*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("Nam*", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete("stephen.johnson*", 0, 10).length());
		// Lucene cannot search this for some reason
//		assertEquals("Autocomplete not working as expected",1,Inbox.getInstance().autoComplete(ia.getAddress(), 0, 10).length());
	}

	public void testState() throws Exception {
		BlueboxMessage original = TestUtils.addRandom(StorageFactory.getInstance());
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		StorageFactory.getInstance().setState(original.getIdentifier(),BlueboxMessage.State.DELETED);
		assertEquals("Expected to find our mail in trash",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail in trash",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		StorageFactory.getInstance().setState(original.getIdentifier(),BlueboxMessage.State.NORMAL);
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		List<BlueboxMessage> list = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.ANY, 0, 12, BlueboxMessage.RECEIVED, true);
		assertEquals("Expected to find our mail added",1,list.size());
		for (BlueboxMessage m : list) {
			try {
				log.info(m.toJSON(true));
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public void testAddAndRetrieve() throws Exception {
		InboxAddress inbox = new InboxAddress("Stephen johnson <steve@test.com>");
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(Utils.getSession(),
				Utils.getRandomAddress(), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				Utils.getRandomAddresses(1), 
				"subjStr",
				"bodyStr", 
				true);
		BlueboxMessage bbm = StorageFactory.getInstance().store(inbox, inbox.getAddress(), message);
		BlueboxMessage stored = StorageFactory.getInstance().retrieve(bbm.getIdentifier());
		assertEquals("Identifiers did not match",bbm.getIdentifier(),stored.getIdentifier());
		MimeMessageWrapper storedMM = stored.getBlueBoxMimeMessage();

		assertEquals("MimeMessage subjects did not match",message.getSubject(),storedMM.getSubject());
		assertEquals("Inbox address did not match",inbox.getFullAddress(),stored.getInbox().getFullAddress());
		assertEquals("Received time did not match",bbm.getLongProperty(BlueboxMessage.RECEIVED),stored.getLongProperty(BlueboxMessage.RECEIVED));
		assertEquals("Subjects did not match",bbm.getProperty(BlueboxMessage.SUBJECT),stored.getProperty(BlueboxMessage.SUBJECT));
		assertEquals("From did not match",bbm.getProperty(BlueboxMessage.FROM),stored.getProperty(BlueboxMessage.FROM));
		log.info(bbm.toJSON(true));
	}
	
	public void testInboxAndFullName() throws Exception {
		InboxAddress inbox = new InboxAddress("Stephen johnson <steve@test.com>");
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(Utils.getSession(),
				Utils.getRandomAddress(), 
				new InternetAddress[]{new InternetAddress(inbox.getFullAddress())}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr", 
				true);
		BlueboxMessage bbm = StorageFactory.getInstance().store(inbox, inbox.getAddress(), message);
		BlueboxMessage stored = StorageFactory.getInstance().retrieve(bbm.getIdentifier());
		assertEquals("Stored recipient did not match original",inbox.getFullAddress(),stored.getInbox().getFullAddress());
		assertEquals("Stored recipient did not match original",inbox.getAddress(),stored.getInbox().getAddress());
	}

	public void testRetrieve() throws Exception {
		for (int i = 0; i < 20; i++) {
			BlueboxMessage original = TestUtils.addRandom(StorageFactory.getInstance());
			try {
				BlueboxMessage saved = StorageFactory.getInstance().retrieve(original.getIdentifier());
				assertEquals("The uid of the retrieved object did not match the one we saved",original.getIdentifier(),saved.getIdentifier());
			}
			catch (Exception re) {
				fail("Failed to retrieve the stored message :"+re.getMessage());
			}
		}
		try {
			StorageFactory.getInstance().retrieve("cafebabe-1fad-4b08-8ddd-f30d7bf1e53d");
			fail("Should not have found the specified mail");
		}
		catch (Throwable t) {
			// passed
		}
	}

	public void testMailCount() throws Exception {
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.DELETED));
		assertEquals("Expected to find no mail",0,StorageFactory.getInstance().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.ANY));

		BlueboxMessage original = TestUtils.addRandom(StorageFactory.getInstance());
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(original.getInbox(),BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",0,StorageFactory.getInstance().getMailCount(original.getInbox(),BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(original.getInbox(),BlueboxMessage.State.ANY));
		StorageFactory.getInstance().setState(original.getIdentifier(),BlueboxMessage.State.DELETED);
		assertEquals("Expected to find our mail in trash",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail in trash",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail in trash",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		StorageFactory.getInstance().setState(original.getIdentifier(),BlueboxMessage.State.NORMAL);
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		assertEquals("Expected to find our mail added",0,StorageFactory.getInstance().getMailCount(original.getInbox(),BlueboxMessage.State.DELETED));

	}

	public void testAddAndDelete() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance());

		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,StorageFactory.getInstance().getMailCount(new InboxAddress("steve@here.com"),BlueboxMessage.State.NORMAL));
		StorageFactory.getInstance().deleteAll();
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		assertEquals("Expected to find our mails added",count,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
	}

	public void testListEmail() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		InboxAddress inbox = new InboxAddress("steve@here.com");
		List<BlueboxMessage> mails = StorageFactory.getInstance().listMail(inbox, BlueboxMessage.State.NORMAL, 0,SIZE,BlueboxMessage.RECEIVED, true);
		assertEquals("Expected to find our mails added",count,StorageFactory.getInstance().getMailCount(inbox, BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mails added",count,mails.size());

		// try listing a non-existent email
		InboxAddress inbox2 = new InboxAddress("idontexist@nowhere.com");
		mails = StorageFactory.getInstance().listMail(inbox2, BlueboxMessage.State.NORMAL, 0,-1,BlueboxMessage.RECEIVED, true);
		assertEquals("Should not find any mails for "+inbox2,0,mails.size());
	}

	public void testListInbox() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		InboxAddress inbox = new InboxAddress("steve@here.com");
		StringWriter sw = new StringWriter();
		StorageFactory.getInstance().listInbox(inbox, BlueboxMessage.State.NORMAL, sw, 0,SIZE,BlueboxMessage.RECEIVED, true, Locale.getDefault());
		JSONArray inboxList = new JSONArray(sw.toString());
		assertEquals("Expected to find our mails added",count,inboxList.length());
	}

	public void testJSONDetail() throws Exception {
		int count = 2;
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.ANY));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		InboxAddress email = new InboxAddress("steve@here.com");
		List<BlueboxMessage> mail = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.ANY, 0,SIZE,BlueboxMessage.RECEIVED, true);

		for (BlueboxMessage message : mail) {
			log.info(message.toJSON(true));
		}
	}

	public void testPaging() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		List<BlueboxMessage> results;
		results = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.NORMAL, 0, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",10,results.size());
		results = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.NORMAL, 10, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",10,results.size());
		results = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.NORMAL, SIZE*3, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",0,results.size());
	}

	public void testPagingWithEmail() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		List<BlueboxMessage> results;
		int pageSize = 5;
		results = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, 0, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",pageSize,results.size());
		results = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, pageSize, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",pageSize,results.size());
		results = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, SIZE, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",0,results.size());
	}

	public void testOrderByDate() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		List<BlueboxMessage> results = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, 0, -1,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",SIZE,results.size());
		BlueboxMessage prev = null;
		for (BlueboxMessage curr : results) {
			Date mDate, pDate;
			//			log.info(curr.toString());
			if (prev!=null) {
				mDate = new Date(Long.parseLong(curr.getProperty(BlueboxMessage.RECEIVED)));
				pDate = new Date(Long.parseLong(prev.getProperty(BlueboxMessage.RECEIVED)));
				assertTrue("Mail order was not correct",((mDate.after(pDate))||(mDate.equals(pDate))));
			}
			prev = curr;
		}
	}

	public void testOrderBySize() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandom(StorageFactory.getInstance(),count);
		List<BlueboxMessage> results = StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, 0, -1,BlueboxMessage.SIZE, true);
		assertEquals("Paging did not return correct amount of results",SIZE,results.size());
		long currSize=0, prevSize=-1;
		for (BlueboxMessage curr : results) {
			currSize = curr.getLongProperty(BlueboxMessage.SIZE);
			assertTrue("Mail order was not sorted by size "+currSize+" > "+prevSize,prevSize<=currSize);
			prevSize = currSize;
		}
	}

	public void testAutoComplete() throws Exception {
		String name = "Monica Smith";
		InboxAddress email = new InboxAddress(name+" <monica.smith@test.com>");

		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(null,
				Utils.getRandomAddress(), 
				new InternetAddress[]{new InternetAddress(email.getFullAddress())}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr", 
				true);
		BlueboxMessage m1 = StorageFactory.getInstance().store(email, email.getAddress(), message);
		BlueboxMessage m2 = StorageFactory.getInstance().store(email, email.getAddress(), message);
		BlueboxMessage m3 = StorageFactory.getInstance().store(email, email.getAddress(), message);
		SearchIndexer.getInstance().indexMail(m1);
		SearchIndexer.getInstance().indexMail(m2);
		SearchIndexer.getInstance().indexMail(m3);
		
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("mon*", 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("monica*", 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("monica.smith*", 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("Mon*", 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("smi*", 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("Smi*", 0, 10).length());
		//		assertEquals("Message not found",1,Inbox.getInstance().autoComplete(inbox, 0, 10).length());
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete("ith*", 0, 10).length());
		System.out.println(Inbox.getInstance().autoComplete(name+"*", 0, 10).toString(3));
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete(name+"*", 0, 10).length());

		// test for search of name
		assertEquals("Message not found",1,Inbox.getInstance().autoComplete(name+"*", 0, 10).length());

		// check we get the full name in the result
		JSONArray ja = Inbox.getInstance().autoComplete(name+"*", 0, 10);
		//		log.info(ja.toString(3));
		//		log.info(name.toLowerCase());
		assertTrue("Did not return full name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for partial firstname
		ja = Inbox.getInstance().autoComplete("Monica*", 0, 10);
		log.info(ja.toString(3));
		assertTrue("Did not return search on first name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for partial secondname
		ja = Inbox.getInstance().autoComplete("Smith*", 0, 10);
		log.info(ja.toString(3));
		assertTrue("Did not return search on last name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for empty string
		ja = Inbox.getInstance().autoComplete("*", 0, 10);
		log.info(ja.toString(3));
		assertEquals("Did not return results",1,ja.length());
	}

	public void testAutoCompleteEmpty() throws Exception {
		InboxAddress inbox = new InboxAddress("monica.smith@test.com");
		String name = "Another Name";
		String email = name+" <"+inbox.getAddress()+">";
		InternetAddress to = new InternetAddress(email);
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(Utils.getSession(),
				Utils.getRandomAddress(), 
				new InternetAddress[]{to}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr", 
				true);
		SearchIndexer.getInstance().indexMail(StorageFactory.getInstance().store(inbox, inbox.getAddress(), message));
		SearchIndexer.getInstance().indexMail(StorageFactory.getInstance().store(inbox, inbox.getAddress(), message));
		SearchIndexer.getInstance().indexMail(StorageFactory.getInstance().store(inbox, inbox.getAddress(), message));


		// check for empty string
		JSONArray ja = Inbox.getInstance().autoComplete("", 0, 10);
		log.info(ja.toString(3));
		assertEquals("Did not return results",1,ja.length());
	}

	public void testPerf() throws Exception {
		int MAX = 50;
		int STEP = 5;
		Date now;
		InboxAddress email = new InboxAddress("steve@there.com");
		String prefix="";//new Date().getTime()+"-";
		Writer writer = new FileWriter(prefix+"list-perf.csv");
		Writer writer2 = new FileWriter(prefix+"listEmail-perf.csv");
		writer.write("Count,Time\r\n");
		writer2.write("Count,Time\r\n");
		long count=0,time;
		for (int i = 0; i < MAX; i++) {
			log.info("Perf at "+count+" of "+(MAX*STEP));
			TestUtils.addRandom(StorageFactory.getInstance(),STEP);
			count += STEP;
			now = new Date();
			StorageFactory.getInstance().getMailCount(email,BlueboxMessage.State.NORMAL);
			time=(new Date().getTime()-now.getTime());
			log.fine("NORMAL Count="+count+"Query took "+time);
			StorageFactory.getInstance().getMailCount(email,BlueboxMessage.State.ANY);
			time=(new Date().getTime()-now.getTime());
			log.fine("ANY Count="+count+"Query took "+time);
			StorageFactory.getInstance().getMailCount(email,BlueboxMessage.State.DELETED);
			time=(new Date().getTime()-now.getTime());
			log.fine("DELETED Count="+count+"Query took "+time);
			writer.write(count+","+time+"\r\n");

			now = new Date();
			StorageFactory.getInstance().listMail(email, BlueboxMessage.State.NORMAL, 0, STEP,BlueboxMessage.RECEIVED, true);
			time=(new Date().getTime()-now.getTime());
			log.fine("List="+count+"Query took "+time);
			writer2.write(count+","+time+"\r\n");
		}
		writer.close();
		writer2.close();
	}

	public void testErrorStorage() throws JSONException {
		StorageFactory.getInstance().logErrorClear();
		assertEquals("Error log should be empty",1,StorageFactory.getInstance().logErrorCount());
		StorageFactory.getInstance().logError("This is a test error", Utils.convertStringToStream("This is a blob"));
		assertEquals("Error log should not be empty",2,StorageFactory.getInstance().logErrorCount());
		JSONArray errors = StorageFactory.getInstance().logErrorList(0,10);
		assertEquals("Missing logs",errors.length(),2);
		log.info(errors.toString(3));
		assertEquals("Incorrect title","This is a test error",((JSONObject)errors.get(1)).get("title"));
		String id = ((JSONObject)errors.get(1)).getString("id");
		log.info("Looking for id "+id);
		assertEquals("Incorrect content","This is a blob",StorageFactory.getInstance().logErrorContent(id));
	}
}
