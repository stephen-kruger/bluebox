package com.bluebox.load;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.databene.contiperf.junit.ParallelRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.bluebox.Utils;
import com.bluebox.servlet.BaseServletTest;
import com.bluebox.smtp.Inbox;



@RunWith(ParallelRunner.class)
public class ServerLoadTest extends BaseServletTest {

	@Rule
	public ContiPerfRule i = new ContiPerfRule();
	
	@Before
	public void setUp() throws Exception {
	  super.setUp();
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	@Test
	@PerfTest(invocations = 10, threads = 2)
	public void testLoad() throws Exception {
		// send some mail
		Utils.generateNoThread(null, Inbox.getInstance(), 10);
		// get some stats
		System.out.println(getResponse("/jaxrs","/stats/global"));
		System.out.println(getResponse("/jaxrs","/stats/recent"));
		System.out.println(getResponse("/jaxrs","/stats/active"));
		System.out.println(getResponse("/jaxrs","/stats/sender"));

		// autocomplete
//		Inbox.getInstance().autoComplete("*", 0, 100);
		// list
//		Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 100, BlueboxMessage.RECEIVED, true);
//		Log.info("Mailcount:"+Inbox.getInstance().getMailCount(BlueboxMessage.State.ANY));
	}
}
