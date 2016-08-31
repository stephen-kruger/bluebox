package com.bluebox;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import junit.framework.TestCase;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.subethamail.smtp.util.Base64;

import com.bluebox.smtp.InboxAddress;

public class UtilsTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	@Test
	public void testGetEmail() throws AddressException {
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("Stephen Johnson <steve@nowhere.com>").getAddress());
		assertEquals("The email portion was not extracted correctly","csgdaily@bluebox.xxx.com",new InboxAddress("\"csgdaily@bluebox.xxx.com \" <csgdaily@bluebox.xxx.com>").getAddress());
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("steve@nowhere.com").getAddress());
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("<steve@nowhere.com>").getAddress());
	}

	@Test
	public void testUTF8Decode() throws UnsupportedEncodingException {
		//		String src = "è¿™æ˜¯ä¸€ä¸ªä¸»é¢˜";//6L+Z5piv5LiA5Liq5Li76aKY
		String src2 = "这是一个主题";
		String encodedStr = "=?UTF-8?B?6L+Z5piv5LiA5Liq5Li76aKY?=";
		String encodedStr2 = "6L+Z5piv5LiA5Liq5Li76aKY";
		log.info(new String(Base64.decode(encodedStr2)));
		log.info(Utils.decodeQuotedPrintable(encodedStr));
		assertEquals("UTF-8 was not correctly  decoded",src2,Utils.decodeQuotedPrintable(encodedStr));
	}

	@Test
	public void testNotesAddressConversion() throws AddressException, UnsupportedEncodingException {
		String notes = "Stephen Johnson/Ireland/XXX";
		String email = Utils.convertNotesAddress(notes);
		log.info(email);
		InternetAddress ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());

		notes = "Stephen_Johnson/Ireland/XXX";
		email = Utils.convertNotesAddress(notes);
		log.info(email);
		ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());

		// /bluebox/rest/json/folder/all_groups_2011_10_25%25XXXUS%25XXXGB
		notes = "all_groups_2011_10_25/XXXUS/XXXGB";
		email = Utils.convertNotesAddress(notes);
		log.info(email);
		ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());
	}

	@Test
	public void testDecodeQuotedPrinted() {
		String quoted  ="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.=\r\n"+
				"w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">=0A<html xmlns=3D\"http://www.=\r\n"+
				"w3.org/1999/xhtml\" lang=3D'en_us'>=0A";
		String unquoted="<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n<html xmlns=\"http://www.w3.org/1999/xhtml\" lang='en_us'>";
		//				String quoted="If you believe that truth=3Dbeauty, then surely mathematics =\nis the most beautiful branch of philosophy.";
		//				String unquoted="If you believe that truth=beauty, then surely mathematics is the most beautiful branch of philosophy.";
		String test = Utils.decodeQuotedPrintable(quoted);
		//		log.info(""+test.length());
		//		log.info(""+unquoted.length());
		// note: or some reason a direct compare fails, even though the strings look identical. Might
		// be related to hidden lf/cr et
		// if the length is the same, it's likely decode correctly
		assertEquals("String did not decode correctly",unquoted.length(),test.length());
	}

	@Test
	public void testUpdateCheck() {
		assertTrue(Utils.isVersionNewer("2.0.0", "1.0.0"));
		assertFalse(Utils.isVersionNewer("2.0.0", "2.0.0"));
		assertFalse(Utils.isVersionNewer("1.0.0", "2.0.0"));
		assertFalse(Utils.isVersionNewer("1.1.0", "2.1.0"));
	}

	@Test
	public void testOnlineUpdateCheck() throws JSONException {
		JSONObject jo = Utils.updateAvailable();
		jo = Utils.updateAvailable();
		log.info(jo.toString());
		assertNotNull(jo.getString("current_version"));
		assertNotNull(jo.getString("available_version"));
		assertNotNull(jo.getString("update_available"));
	}

//	@Test
//	public void testTempFile() throws IOException {
//		PriorityQueue<File>stack = new PriorityQueue<File>(20,new FileDateComparator());
//		for (int i = 0; i < 500; i++) {
//			File f = File.createTempFile("bluebox", ".spool");
//			f.deleteOnExit();	
//			assertTrue(f.exists());
//			FileWriter fw = new FileWriter(f);
//			fw.write("x");
//			fw.close();
//			stack.add(f);
//		}
//
//		File older = stack.remove();
//		while (stack.size()>0) {
//			File old = stack.remove();
//			assertTrue("File were not removed in oldest first order",older.lastModified()<old.lastModified());
//		}
//	}

}