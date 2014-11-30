package com.bluebox.chart;

import java.util.Calendar;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BaseTestCase;
import com.bluebox.TestUtils;
import com.bluebox.rest.json.JSONChartHandler;
import com.bluebox.smtp.storage.StorageFactory;

public class ChartTest extends BaseTestCase {
	private static final Logger log = LoggerFactory.getLogger(ChartTest.class);
	private static int COUNT = 10;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		log.info("Populating chart tests");
		TestUtils.addRandomDirect(StorageFactory.getInstance(), COUNT);
		TestUtils.waitFor(getInbox(),COUNT);
	}

	@Test
	public void testCountByDay() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByDay();

		Calendar cal = Calendar.getInstance();
		int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
		for (int i = 1; i < 32;i++) {
			assertNotNull(jo.get(""+i));
			if (i==dayOfMonth) {
				assertEquals("Incorrect status reported for today",COUNT,jo.getInt(""+i));
			}
			else {
				assertEquals("Incorrect status reported for day "+i,0,jo.getInt(""+i));				
			}
		}
	}

	@Test
	public void testCountByHour() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByHour();
		log.info(jo.toString());
		Calendar cal = Calendar.getInstance();
		int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
		log.info("Current hour is "+hourOfDay);
		for (int i = 0; i < 24;i++) {
			assertNotNull(jo.get(""+i));
			if (i==hourOfDay) {
				assertEquals("Incorrect status reported for hour "+i,COUNT,jo.getInt(""+i));
			}
			else {
				assertEquals("Incorrect status reported for hour "+i,0,jo.getInt(""+i));				
			}
		}
	}

	@Test
	public void testCountByDayOfWeek() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByDayOfWeek();
		log.info(jo.toString());
		Calendar cal = Calendar.getInstance();
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int repeat = 10;
		do {
			log.debug("Current day is {}",dayOfWeek);
			for (int i = 1; i < 8;i++) {
				assertNotNull(jo.get(""+i));
				if (i==dayOfWeek) {
					assertEquals("Incorrect status reported for day "+i,COUNT,jo.getInt(""+i));
				}
				else {
					assertEquals("Incorrect status reported for day "+i,0,jo.getInt(""+i));				
				}
			}
		} while ((repeat--)>0);
	}

	@Test
	public void testScratch() throws JSONException {
		JSONObject jo = StorageFactory.getInstance().getCountByDayOfWeek();
		log.info(jo.toString(3));		
		log.info(JSONChartHandler.convertToArrayPie(jo).toString(3));		
	}
}
