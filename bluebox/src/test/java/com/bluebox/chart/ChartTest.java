package com.bluebox.chart;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.TestUtils;
import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;

public class ChartTest extends TestCase {
	private static final Logger log = LoggerFactory.getLogger(ChartTest.class);

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Inbox.getInstance().deleteAll();; // trigger inbox and storage start
		TestUtils.addRandom(StorageFactory.getInstance(), 10);
		Utils.waitFor(10);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
//		Inbox.getInstance().deleteAll();;
		Inbox.getInstance().stop();
	}

	@SuppressWarnings("deprecation")
	public void testHistory() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByDay();
		log.info(jo.toString(3));
		assertNotNull(jo.get("1"));
		assertNotNull(jo.get("31"));
		
		assertEquals("Incorrect status reported for today",10,jo.getInt(""+new Date().getDate()));
	}
	
	public void testChart() throws Exception {
		File f = File.createTempFile("blueboxchart", "png");
		f.deleteOnExit();
		Charts c = new Charts();
		c.renderHistoryChart(new FileOutputStream(f),200,200);
	}
}
