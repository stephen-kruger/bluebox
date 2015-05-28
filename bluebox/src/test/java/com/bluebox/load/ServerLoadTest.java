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
import com.bluebox.rest.json.JSONStatsHandler;
import com.bluebox.servlet.BaseServletTest;



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
		Utils.generateNoThread(null, getInbox(), 10);
		// get some stats
		System.out.println(getURL("/"+JSONStatsHandler.JSON_ROOT+"/"+JSONStatsHandler.GLOBAL_STAT));
		System.out.println(getURL("/"+JSONStatsHandler.JSON_ROOT+"/"+JSONStatsHandler.RECENT_STAT+"/"));
		System.out.println(getURL("/"+JSONStatsHandler.JSON_ROOT+"/"+JSONStatsHandler.ACTIVE_STAT+"/"));
		System.out.println(getURL("/"+JSONStatsHandler.JSON_ROOT+"/"+JSONStatsHandler.SENDER_STAT+"/"));

		// autocomplete
//		getInbox().autoComplete("*", 0, 100);
		// list
//		getInbox().listInbox(null, BlueboxMessage.State.ANY, 0, 100, BlueboxMessage.RECEIVED, true);
//		Log.info("Mailcount:"+getInbox().getMailCount(BlueboxMessage.State.ANY));
	}
}
