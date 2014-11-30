package com.bluebox.load;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.databene.contiperf.junit.ParallelRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mortbay.log.Log;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;
import com.bluebox.smtp.storage.BlueboxMessage;



@RunWith(ParallelRunner.class)
public class LoadTest extends BaseTestCase {

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
	@PerfTest(invocations = 50, threads = 2)
	public void testLoad() throws Exception {
		// send some mail
		TestUtils.addRandomNoThread(getInbox(), 10);
		// get some stats
		getInbox().getStatsActiveSender();
		getInbox().getStatsActiveInbox();
		getInbox().getStatsRecent();
		getInbox().getStatsGlobalCount();
		// autocomplete
		getInbox().autoComplete("*", 0, 100);
		// list
		getInbox().listInbox(null, BlueboxMessage.State.ANY, 0, 100, BlueboxMessage.RECEIVED, true);
		Log.info("Mailcount:"+getInbox().getMailCount(BlueboxMessage.State.ANY));
	}
}
