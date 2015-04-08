package com.bluebox.load;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.databene.contiperf.junit.ParallelRunner;
import org.databene.contiperf.timer.ConstantTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;



@RunWith(ParallelRunner.class)
public class LoadTest {
	private static final Logger log = Logger.getAnonymousLogger();

	@Rule
	public ContiPerfRule i = new ContiPerfRule();
	
	@Before
	public void setUp() throws Exception {
	  log.info("Setup");
	}
	
	@After
	public void tearDown() throws Exception {
		  log.info("Teardown");
	}
	
	@Test
	@PerfTest(duration = 2000, threads = 3, timer = ConstantTimer.class, timerParams = { 1200 })
	public void testMulti() throws Exception {
		// todo - this code is never run
		log.info("Starting load test");
		fail("xxx");
	}
	
//	@Test
//	@PerfTest(invocations = 100, threads = 2)
//	public void testLoad() throws Exception {
//		log.info("Starting load test");
//		// send some mail
//		TestUtils.addRandomNoThread(getInbox(), 10);
//		// get some stats
//		getInbox().getStatsActiveSender();
//		getInbox().getStatsActiveInbox();
//		getInbox().getStatsRecent();
//		getInbox().getStatsGlobalCount();
//		// autocomplete
//		getInbox().autoComplete("*", 0, 100);
//		// list
//		getInbox().listInbox(null, BlueboxMessage.State.ANY, 0, 100, BlueboxMessage.RECEIVED, true);
//		Log.info("Mailcount:"+getInbox().getMailCount(BlueboxMessage.State.ANY));
//		assertEquals("Missing mails",10,getInbox().errorCount());
//	}
}
