package com.bluebox.smtp.storage;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.search.SearchFactory;
import com.bluebox.smtp.InboxAddress;

public class StorageTest extends BaseTestCase {
	private static final Logger log = LoggerFactory.getLogger(StorageTest.class);
	private int SIZE = 20;

	@Test
	public void testMPH() throws Exception {
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),100);
		StorageIf si = getBlueBoxStorageIf();
		JSONObject mph = si.getMPH(null);
		assertEquals("MPH was wrong",100,mph.getInt("mph"));
		//		assertEquals("MPH24 was wrong",4,mph.getInt("mph24"));
		log.info(mph.toString());
	}

	@Test
	public void testMPHInboxAddress() throws Exception {
		String addr = "test@here.com";
		StorageIf si = getBlueBoxStorageIf();
		JSONObject mph = si.getMPH(new InboxAddress(addr));
		assertEquals("MPH was wrong",0,mph.getInt("mph"));
		for (int i = 0; i < 10; i++) {
			TestUtils.sendMailDirect(getInbox(), addr, "sender@there.com");
			mph = si.getMPH(new InboxAddress(addr));
			assertEquals("MPH was wrong",i+1,mph.getInt("mph"));
			mph = si.getMPH(new InboxAddress("idontexist@me.com"));
			assertEquals("MPH was wrong",0,mph.getInt("mph"));
		}
	}

	@Test
	public void testAutoComplete2() throws Exception {
		String email = "\"First Name\" <stephen.johnson@mail.com>";
		String from = "\"Jack Jones\" <jack.jones@mail.com>";
		MimeMessage message = Utils.createMessage(null,
				new InternetAddress(from), 
				new InternetAddress[]{new InternetAddress(email)}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr",
				"",
				false);
		InboxAddress ia = new InboxAddress(email);

		String uid = TestUtils.spoolMessage(getBlueBoxStorageIf(),message);
		BlueboxMessage m1 = getBlueBoxStorageIf().store(from, ia, getBlueBoxStorageIf().getUTCTime(), message, uid);
		//		TestUtils.removeSpooledMessage(getBlueBoxStorageIf(),uid);
		SearchFactory.getInstance().indexMail(m1,true);

		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("First Name", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("ste", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("ste", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("Ste", 0, 10).length());
		// todo - fixme, maybe
		//		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("joh", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("Nam", 0, 10).length());
		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete("stephen.johnson", 0, 10).length());
		// Lucene cannot search this for some reason
		//		assertEquals("Autocomplete not working as expected",1,getInbox().autoComplete(ia.getAddress(), 0, 10).length());
	}

	@Test
	public void testState() throws Exception {
		BlueboxMessage original = TestUtils.addRandomDirect(getBlueBoxStorageIf());
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		getBlueBoxStorageIf().setState(original.getIdentifier(),BlueboxMessage.State.DELETED);
		assertEquals("Expected to find our mail in trash",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail in trash",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		getBlueBoxStorageIf().setState(original.getIdentifier(),BlueboxMessage.State.NORMAL);
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		List<BlueboxMessage> list = getBlueBoxStorageIf().listMail(null, BlueboxMessage.State.ANY, 0, 12, BlueboxMessage.RECEIVED, true);
		assertEquals("Expected to find our mail added",1,list.size());
		//		for (BlueboxMessage m : list) {
		//			try {
		//				log.info(m.toJSON());
		//			} 
		//			catch (JSONException e) {
		//				e.printStackTrace();
		//			}
		//		}
	}

	@Test
	public void testAddAndRetrieve() throws Exception {
		getBlueBoxStorageIf().deleteAll();
		InboxAddress inbox = new InboxAddress("Stephen johnson <steve.johnson@test.com>");
		String from = "sender@nowhere.com";
		MimeMessage message = Utils.createMessage(null,
				new InternetAddress(from), 
				new InternetAddress[]{new InternetAddress(inbox.getFullAddress())}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr",
				"",
				false);
		String rawuid = TestUtils.spoolMessage(getBlueBoxStorageIf(),message);
		BlueboxMessage bbm = getBlueBoxStorageIf().store(from, inbox, getBlueBoxStorageIf().getUTCTime(), message, rawuid);
		//		TestUtils.removeSpooledMessage(getBlueBoxStorageIf(),uid);
		BlueboxMessage stored = getBlueBoxStorageIf().retrieve(bbm.getIdentifier());
		assertEquals("Identifiers did not match",bbm.getIdentifier(),stored.getIdentifier());
		MimeMessage storedMM = stored.getBlueBoxMimeMessage();
		assertEquals("MimeMessage subjects did not match",message.getSubject(),storedMM.getSubject());
		assertEquals("Inbox address did not match",inbox.getAddress(),stored.getInbox().getAddress());
		assertEquals("Received time did not match",bbm.getReceived(),stored.getReceived());
		assertEquals("Subjects did not match",bbm.getSubject(),stored.getSubject());
		assertEquals("Subjects did not match",message.getSubject(),storedMM.getSubject());
		//		assertEquals("From did not match",bbm.getProperty(BlueboxMessage.FROM),stored.getProperty(BlueboxMessage.FROM));
		//		log.info(stored.toJSON());
	}

	@Test
	public void testInboxAndFullName() throws Exception {
		InboxAddress inbox = new InboxAddress("Stephen johnson <steve@test.com>");
		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				new InternetAddress[]{new InternetAddress(inbox.getFullAddress())}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr",
				"",
				false);
		String uid = TestUtils.spoolMessage(getBlueBoxStorageIf(),message);

		BlueboxMessage bbm = getBlueBoxStorageIf().store(inbox.getAddress(), inbox, getBlueBoxStorageIf().getUTCTime(), message, uid);
		//		TestUtils.removeSpooledMessage(getBlueBoxStorageIf(),uid);
		BlueboxMessage stored = getBlueBoxStorageIf().retrieve(bbm.getIdentifier());
		//		assertEquals("Stored recipient did not match original",inbox.getFullAddress(),stored.getInbox().getFullAddress());
		assertEquals("Stored recipient did not match original",inbox.getAddress(),stored.getInbox().getAddress());
	}

	@Test
	public void testRetrieve() throws Exception {
		for (int i = 0; i < 20; i++) {
			BlueboxMessage original = TestUtils.addRandomDirect(getBlueBoxStorageIf());
			try {
				BlueboxMessage saved = getBlueBoxStorageIf().retrieve(original.getIdentifier());
				assertEquals("The uid of the retrieved object did not match the one we saved",original.getIdentifier(),saved.getIdentifier());
				assertEquals("The subject of the retrieved object did not match the one we saved",original.getBlueBoxMimeMessage().getSubject(),saved.getBlueBoxMimeMessage().getSubject());
			}
			catch (Exception re) {
				fail("Failed to retrieve the stored message :"+re.getMessage());
			}
		}
		try {
			getBlueBoxStorageIf().retrieve("cafebabe-1fad-4b08-8ddd-f30d7bf1e53d");
			fail("Should not have found the specified mail");
		}
		catch (Throwable t) {
			// passed
		}
	}

	@Test
	public void testMailCount() throws Exception {
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.DELETED));
		assertEquals("Expected to find no mail",0,getBlueBoxStorageIf().getMailCount(new InboxAddress("idontexist@nowhere.com"),BlueboxMessage.State.ANY));

		BlueboxMessage original = TestUtils.addRandomDirect(getBlueBoxStorageIf());
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(original.getInbox(),BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",0,getBlueBoxStorageIf().getMailCount(original.getInbox(),BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(original.getInbox(),BlueboxMessage.State.ANY));
		getBlueBoxStorageIf().setState(original.getIdentifier(),BlueboxMessage.State.DELETED);
		assertEquals("Expected to find our mail in trash",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail in trash",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.DELETED));
		assertEquals("Expected to find our mail in trash",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		getBlueBoxStorageIf().setState(original.getIdentifier(),BlueboxMessage.State.NORMAL);
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		assertEquals("Expected to find our mail added",0,getBlueBoxStorageIf().getMailCount(original.getInbox(),BlueboxMessage.State.DELETED));

	}

	@Test
	public void testAddAndDelete() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf());
		assertEquals("Expected to find our mail added",1,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		getBlueBoxStorageIf().deleteAll();
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		assertEquals("Expected to find our mails added",count,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
	}

	@Test
	public void testListEmail() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		InboxAddress inbox = new InboxAddress("steve@here.com");
		List<BlueboxMessage> mails = getBlueBoxStorageIf().listMail(inbox, BlueboxMessage.State.NORMAL, 0,SIZE,BlueboxMessage.RECEIVED, true);
		assertEquals("Expected to find our mails added",count,getBlueBoxStorageIf().getMailCount(inbox, BlueboxMessage.State.NORMAL));
		assertEquals("Expected to find our mails added",count,mails.size());

		// try listing a non-existent email
		InboxAddress inbox2 = new InboxAddress("idontexist@nowhere.com");
		mails = getBlueBoxStorageIf().listMail(inbox2, BlueboxMessage.State.NORMAL, 0,-1,BlueboxMessage.RECEIVED, true);
		assertEquals("Should not find any mails for "+inbox2,0,mails.size());
	}

	@Test
	public void testListInbox() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		InboxAddress inbox = new InboxAddress("steve@here.com");
		StringWriter sw = new StringWriter();
		getBlueBoxStorageIf().listInbox(inbox, BlueboxMessage.State.NORMAL, sw, 0,SIZE,BlueboxMessage.RECEIVED, true, Locale.getDefault());
		JSONArray inboxList = new JSONArray(sw.toString());
		assertEquals("Expected to find our mails added",count,inboxList.length());
	}

	@Test
	public void testJSONDetail() throws Exception {
		int count = 2;
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.ANY));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		List<BlueboxMessage> mail = getBlueBoxStorageIf().listMail(null, BlueboxMessage.State.ANY, 0,SIZE,BlueboxMessage.RECEIVED, true);

		for (BlueboxMessage message : mail) {
			JSONObject jo = message.toJSON();
			assertTrue(jo.has(BlueboxMessage.UID));
			assertTrue(jo.has(BlueboxMessage.INBOX));
			assertTrue(jo.has(BlueboxMessage.RECEIVED));
			assertTrue(jo.has(BlueboxMessage.SIZE));
		}
	}

	@Test
	public void testPaging() throws Exception {
		int count = SIZE;
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		List<BlueboxMessage> results;
		results = getBlueBoxStorageIf().listMail(null, BlueboxMessage.State.NORMAL, 0, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",10,results.size());
		results = getBlueBoxStorageIf().listMail(null, BlueboxMessage.State.NORMAL, 10, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",10,results.size());
		results = getBlueBoxStorageIf().listMail(null, BlueboxMessage.State.NORMAL, SIZE*3, 10,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",0,results.size());
	}

	@Test
	public void testPagingWithEmail() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		List<BlueboxMessage> results;
		int pageSize = 5;
		results = getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, 0, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",pageSize,results.size());
		results = getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, pageSize, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",pageSize,results.size());
		results = getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, SIZE, pageSize,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",0,results.size());
	}

	@Test
	public void testOrderByDate() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		List<BlueboxMessage> results = getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, 0, -1,BlueboxMessage.RECEIVED, true);
		assertEquals("Paging did not return correct amount of results",SIZE,results.size());
		BlueboxMessage prev = null;
		for (BlueboxMessage curr : results) {
			Date mDate, pDate;
			//			log.info(curr.toString());
			if (prev!=null) {
				mDate = curr.getReceived();
				pDate = prev.getReceived();
				assertTrue("Mail order was not correct",((mDate.after(pDate))||(mDate.equals(pDate))));
			}
			prev = curr;
		}
	}

	@Test
	public void testOrderBySize() throws Exception {
		int count = SIZE;
		InboxAddress email = new InboxAddress("steve@here.com");
		assertEquals("Did not expect to find anything",0,getBlueBoxStorageIf().getMailCount(BlueboxMessage.State.NORMAL));
		TestUtils.addRandomDirect(getBlueBoxStorageIf(),count);
		List<BlueboxMessage> results = getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, 0, -1,BlueboxMessage.SIZE, true);
		assertEquals("Paging did not return correct amount of results",SIZE,results.size());
		long currSize=0, prevSize=-1;
		for (BlueboxMessage curr : results) {
			currSize = curr.getSize();
			assertTrue("Mail order was not sorted by size "+currSize+" > "+prevSize,prevSize<=currSize);
			prevSize = currSize;
		}
	}

	@Test
	public void testAutoComplete() throws Exception {
		String name = "Monica Smith";
		InboxAddress email = new InboxAddress(name+" <monica.smith@test.com>");

		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				new InternetAddress[]{new InternetAddress(email.getFullAddress())}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr",
				"",
				false);
		String uid = TestUtils.spoolMessage(getBlueBoxStorageIf(),message);

		BlueboxMessage m1 = getBlueBoxStorageIf().store(email.getAddress(), email, getBlueBoxStorageIf().getUTCTime(), message, uid);
		BlueboxMessage m2 = getBlueBoxStorageIf().store(email.getAddress(), email, getBlueBoxStorageIf().getUTCTime(), message, uid);
		BlueboxMessage m3 = getBlueBoxStorageIf().store(email.getAddress(), email, getBlueBoxStorageIf().getUTCTime(), message, uid);
		//		TestUtils.removeSpooledMessage(getBlueBoxStorageIf(),uid);
		SearchFactory.getInstance().indexMail(m1,true);
		SearchFactory.getInstance().indexMail(m2,true);
		SearchFactory.getInstance().indexMail(m3,true);

		assertEquals("Message not found",1,getInbox().autoComplete("Monica", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("mon", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("monica", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("monica.smith", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("Mon", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("smi", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete("Smi", 0, 10).length());
		//		assertEquals("Message not found",1,getInbox().autoComplete(inbox, 0, 10).length());
		//		assertEquals("Message not found",1,getInbox().autoComplete("ith", 0, 10).length());
		assertEquals("Message not found",1,getInbox().autoComplete(name, 0, 10).length());

		// test for search of name
		assertEquals("Message not found",1,getInbox().autoComplete(name, 0, 10).length());

		// check we get the full name in the result
		JSONArray ja = getInbox().autoComplete(name+"*", 0, 10);
		//		log.info(ja.toString(3));
		//		log.info(name.toLowerCase());
		assertTrue("Did not return full name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for partial firstname
		ja = getInbox().autoComplete("Monica*", 0, 10);
		log.info(ja.toString(3));
		assertTrue("Did not return search on first name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for partial secondname
		ja = getInbox().autoComplete("Smith*", 0, 10);
		log.info(ja.toString(3));
		assertTrue("Did not return search on last name",ja.toString().toLowerCase().indexOf(name.toLowerCase())>0);

		// check for empty string
		ja = getInbox().autoComplete("", 0, 10);
		log.info(ja.toString(3));
		assertEquals("Did not return results",1,ja.length());
	}

	@Test
	public void testAutoCompleteEmpty() throws Exception {
		InboxAddress inbox = new InboxAddress("monica.smith@test.com");
		String name = "Another Name";
		String email = name+" <"+inbox.getAddress()+">";
		InternetAddress to = new InternetAddress(email);
		MimeMessage message = Utils.createMessage(null,
				Utils.getRandomAddress(), 
				new InternetAddress[]{to}, 
				Utils.getRandomAddresses(0), 
				Utils.getRandomAddresses(0), 
				"subjStr",
				"bodyStr",
				"",
				false);
		String uid = TestUtils.spoolMessage(getBlueBoxStorageIf(),message);

		SearchFactory.getInstance().indexMail(getBlueBoxStorageIf().store(inbox.getAddress(), inbox, getBlueBoxStorageIf().getUTCTime(), message, uid),true);
		SearchFactory.getInstance().indexMail(getBlueBoxStorageIf().store(inbox.getAddress(), inbox, getBlueBoxStorageIf().getUTCTime(), message, uid),true);
		SearchFactory.getInstance().indexMail(getBlueBoxStorageIf().store(inbox.getAddress(), inbox, getBlueBoxStorageIf().getUTCTime(), message, uid),true);
		//		TestUtils.removeSpooledMessage(getBlueBoxStorageIf(),uid);

		// check for empty string
		JSONArray ja = getInbox().autoComplete("", 0, 10);
		log.info(ja.toString(3));
		assertEquals("Did not return results",1,ja.length());
	}

	@Test
	public void testPerf() throws Exception {
		int MAX = 50;
		int STEP = 5;
		Date now;
		InboxAddress email = new InboxAddress("steve@there.com");
		String prefix="";//Utils.getUTCTime().getTime()+"-";
		Writer writer = new FileWriter(prefix+"list-perf.csv");
		Writer writer2 = new FileWriter(prefix+"listEmail-perf.csv");
		writer.write("Count,Time\r\n");
		writer2.write("Count,Time\r\n");
		long count=0,time;
		for (int i = 0; i < MAX; i++) {
			log.info("Perf at "+count+" of "+(MAX*STEP));
			TestUtils.addRandomDirect(getBlueBoxStorageIf(),STEP);
			count += STEP;
			now = getBlueBoxStorageIf().getUTCTime();
			getBlueBoxStorageIf().getMailCount(email,BlueboxMessage.State.NORMAL);
			time=(getBlueBoxStorageIf().getUTCTime().getTime()-now.getTime());
			log.debug("NORMAL Count="+count+"Query took "+time);
			getBlueBoxStorageIf().getMailCount(email,BlueboxMessage.State.ANY);
			time=(getBlueBoxStorageIf().getUTCTime().getTime()-now.getTime());
			log.debug("ANY Count="+count+"Query took "+time);
			getBlueBoxStorageIf().getMailCount(email,BlueboxMessage.State.DELETED);
			time=(getBlueBoxStorageIf().getUTCTime().getTime()-now.getTime());
			log.debug("DELETED Count="+count+"Query took "+time);
			writer.write(count+","+time+"\r\n");

			now = getBlueBoxStorageIf().getUTCTime();
			getBlueBoxStorageIf().listMail(email, BlueboxMessage.State.NORMAL, 0, STEP,BlueboxMessage.RECEIVED, true);
			time=(getBlueBoxStorageIf().getUTCTime().getTime()-now.getTime());
			log.debug("List="+count+"Query took "+time);
			writer2.write(count+","+time+"\r\n");
		}
		writer.close();
		writer2.close();
	}

	@Test
	public void testErrorStorage() throws JSONException {
		getBlueBoxStorageIf().logErrorClear();
		assertEquals("Error log should be empty",0,getBlueBoxStorageIf().logErrorCount());
		getBlueBoxStorageIf().logError("This is a test error", Utils.convertStringToStream("This is a blob"));
		getBlueBoxStorageIf().logError("This is a test error", Utils.convertStringToStream("This is a blob"));
		assertEquals("Error log should not be empty",2,getBlueBoxStorageIf().logErrorCount());
		JSONArray errors = getBlueBoxStorageIf().logErrorList(0,10);
		assertEquals("Missing logs",errors.length(),2);
		log.info(errors.toString(3));
		assertEquals("Incorrect title","This is a test error",((JSONObject)errors.get(1)).get("title"));
		assertNotNull("No date found",((JSONObject)errors.get(1)).get("date"));
		String id = ((JSONObject)errors.get(1)).getString("id");
		log.info("Looking for id "+id);
		assertEquals("Incorrect content","This is a blob",getBlueBoxStorageIf().logErrorContent(id));
	}

	@Test
	public void testProperties() {
		StorageIf si = getBlueBoxStorageIf();
		String key = "test.property";
		si.setProperty(key, "12345");
		assertEquals("Property not correctly set","12345",si.getProperty(key));
		si.setProperty(key, "abcdef");
		assertEquals("Property not correctly set","abcdef",si.getProperty(key));
		assertEquals("Default value not correctly returned","qwerty",si.getProperty("somthingrandom","qwerty"));		
		si.setLongProperty(key, 143);
		assertEquals("Property not correctly set",(long)143,si.getLongProperty(key));
	}

	@Test
	public void testSpool() throws Exception {
		String data = "abcdefghijklmnopqrstuvwxyz";
		StorageIf si = StorageFactory.getInstance();
		String uid = si.spoolStream(new ByteArrayInputStream(data.getBytes("UTF-8")));
		assertNotNull("UID not allocated correctly",uid);
		assertEquals("Incorrect blob size reported",26,si.getSpooledStreamSize(uid));
		si.removeSpooledStream(uid);
	}

	@Test
	public void testOrphanedSpools() throws Exception {
		StorageIf si = StorageFactory.getInstance();
		List<String> uids = new ArrayList<String>();
		assertEquals("Did not expect to find spools",0,si.getSpoolCount());
		for (int i = 0; i < 50; i++) {
			log.debug("Spooling to uid {}",si.spoolStream(new ByteArrayInputStream((i+"xxxxxxxxxxxxxxxxxxxxx").getBytes())));
		}
		assertEquals("Unexpected spool count found",50,si.getSpoolCount());
		
		for (int i = 0; i < 50; i++) {
			log.debug("Spooling to uid {}",si.spoolStream(new ByteArrayInputStream(("xxxxxxxxxxxxxxxxxxxxx").getBytes())));
		}
		assertEquals("Unexpected spool count found",51,si.getSpoolCount());

		for (String uid : uids) {
			assertTrue("Spool was not found",si.contains(uid));
			assertTrue(si.getSpooledStreamSize(uid)>0);
		}
		
		si.cleanOrphans().run();
		assertEquals("Did not expect to find spools",0,si.getSpoolCount());
	}
	
	public void testFrom() throws Exception {
		StorageIf si = StorageFactory.getInstance();
		BlueboxMessage message = TestUtils.addRandomDirect(si);
		BlueboxMessage saved = si.retrieve(message.getIdentifier());

		assertEquals("Message reported incorrect sender",message.getFrom().get(0),saved.getFrom().get(0));
	}

}
