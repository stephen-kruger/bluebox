package com.bluebox.chart;

import java.util.Calendar;

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
		log.info("Populating chart tests");
		Inbox.getInstance().deleteAll();; // trigger inbox and storage start
		TestUtils.addRandom(StorageFactory.getInstance(), 10);
		Utils.waitFor(10);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Inbox.getInstance().deleteAll();
		Inbox.getInstance().stop();
	}

	public void testCountByDay() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByDay();

		Calendar cal = Calendar.getInstance();
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		for (int i = 1; i < 32;i++) {
			assertNotNull(jo.get(""+i));
			if (i==dayOfMonth) {
				assertEquals("Incorrect status reported for today",10,jo.getInt(""+i));
			}
			else {
				assertEquals("Incorrect status reported for day "+i,1,jo.getInt(""+i));				
			}
		}
	}

	public void testCountByHour() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByHour();
		log.info(jo.toString());
		Calendar cal = Calendar.getInstance();
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		log.info("Current hour is "+hourOfDay);
		for (int i = 0; i < 24;i++) {
			assertNotNull(jo.get(""+i));
			if (i==hourOfDay) {
				assertEquals("Incorrect status reported for hour "+i,10,jo.getInt(""+i));
			}
			else {
				assertEquals("Incorrect status reported for hour "+i,1,jo.getInt(""+i));				
			}
		}
	}

}
