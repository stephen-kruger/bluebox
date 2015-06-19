package com.bluebox.smtp.storage.derby;

import java.util.List;

import junit.framework.TestCase;

import com.bluebox.TestUtils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.derby.StorageImpl;

public class StorageTest extends TestCase {
	private StorageImpl si;
//	private byte[] blob = new  String("123456").getBytes();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		si = new StorageImpl();
		si.start();
		si.clear();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		si.stop();
	}
	
	public void testProperties() {
		si.setProperty("test","result");
		assertEquals("Incorrect property reported","result",si.getProperty("test", "xxx"));
		si.setProperty("test","newresult");
		assertEquals("Incorrect property reported","newresult",si.getProperty("test", "xxx"));
	}

	public void testListMail() throws Exception {
		BlueboxMessage message = TestUtils.addRandomDirect(si);
//		String id = si.add(UUID.randomUUID().toString(), "test@nowhere.com", "Test Name <test@nowhere.com>", "send@here.com", "this is a subject", Utils.getUTCTime(), State.NORMAL, 45,  new ByteArrayInputStream(blob));

//		Log.info("Added mail "+message.getInbox());
		List<BlueboxMessage> messages;
		messages = si.listMail(message.getInbox(), State.NORMAL, 0, 10, BlueboxMessage.RECEIVED, true);
		assertEquals("Message was missing",1,messages.size());
		assertEquals("Message was missing subject",message.getSubject(),messages.get(0).getSubject());
		assertEquals("Message reported incorrect sender",message.getFrom(),messages.get(0).getFrom());
		
		messages = si.listMail(message.getInbox(), State.ANY, 0, 10, BlueboxMessage.RECEIVED, true);
		assertEquals("Message was missing",1,messages.size());
		
		messages = si.listMail(new InboxAddress("test@nowhere.com"), State.DELETED, 0, 10, BlueboxMessage.RECEIVED, true);
		assertEquals("Message was not supposed to be deleted",0,messages.size());
		
		messages = si.listMail(null, State.ANY, 0, 10, BlueboxMessage.RECEIVED, true);
		assertEquals("Message was not found",1,messages.size());
		
		messages = si.listMail(null, State.ANY, 0, 10, BlueboxMessage.RECEIVED, true);
		assertEquals("Message was not found",1,messages.size());
	}

	public void testAddAndRemove() throws Exception {
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.NORMAL));
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.DELETED));
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.ANY));

//		String id = si.add(UUID.randomUUID().toString(), "test@nowhere.com", "Test Name <test@nowhere.com>", "send@here.com", "this is a subject", Utils.getUTCTime(), State.NORMAL, 45,  new ByteArrayInputStream(blob));
		BlueboxMessage message = TestUtils.addRandomDirect(si);

		assertEquals("Mailbox should not be empty",1,si.getMailCount(State.ANY));
		assertEquals("Mailbox should not be empty",1,si.getMailCount(State.NORMAL));
		assertEquals("Mailbox should not be empty",0,si.getMailCount(State.DELETED));

		assertEquals("Mailbox should not be empty",1,si.getMailCount(message.getInbox(),State.ANY));
		assertEquals("Mailbox should not be empty",1,si.getMailCount(message.getInbox(),State.NORMAL));
		assertEquals("Mailbox should not be empty",0,si.getMailCount(message.getInbox(),State.DELETED));

		si.delete(message.getIdentifier(),message.getRawUid());

		assertEquals("Mailbox should be empty",0,si.getMailCount(State.NORMAL));
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.DELETED));
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.ANY));
	}

	public void testUpdateState() throws Exception {
//		String id = si.add(UUID.randomUUID().toString(), "test@nowhere.com", "Test Name <test@nowhere.com>", "send@here.com", "this is a subject", Utils.getUTCTime(), State.NORMAL, 45, new ByteArrayInputStream(blob));
		BlueboxMessage message = TestUtils.addRandomDirect(si);
		assertEquals("Mailbox should be empty",1,si.getMailCount(State.NORMAL));
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.DELETED));
		assertEquals("Mailbox should be empty",1,si.getMailCount(State.ANY));
		si.setState(message.getIdentifier(), State.DELETED);		
		assertEquals("Mailbox should be empty",0,si.getMailCount(State.NORMAL));
		assertEquals("Mailbox should be empty",1,si.getMailCount(State.DELETED));
		assertEquals("Mailbox should be empty",1,si.getMailCount(State.ANY));
	}
}
