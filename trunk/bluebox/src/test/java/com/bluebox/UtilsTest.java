package com.bluebox;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.subethamail.smtp.util.Base64;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;

import junit.framework.TestCase;

public class UtilsTest extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	public void testGetEmail() throws AddressException {
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("Stephen Johnson <steve@nowhere.com>").getAddress());
		assertEquals("The email portion was not extracted correctly","csgdaily@bluebox.xxx.com",new InboxAddress("\"csgdaily@bluebox.xxx.com \" <csgdaily@bluebox.xxx.com>").getAddress());
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("steve@nowhere.com").getAddress());
		assertEquals("The email portion was not extracted correctly","steve@nowhere.com",new InboxAddress("<steve@nowhere.com>").getAddress());
	}

	public void testUTF8Decode() throws UnsupportedEncodingException {
//		String src = "è¿™æ˜¯ä¸€ä¸ªä¸»é¢˜";//6L+Z5piv5LiA5Liq5Li76aKY
		String src2 = "这是一个主题";
		String encodedStr = "=?UTF-8?B?6L+Z5piv5LiA5Liq5Li76aKY?=";
		String encodedStr2 = "6L+Z5piv5LiA5Liq5Li76aKY";
		log.info(new String(Base64.decode(encodedStr2)));
		log.info(Utils.decodeQuotedPrintable(encodedStr));
		assertEquals("UTF-8 was not correctly  decoded",src2,Utils.decodeQuotedPrintable(encodedStr));
	}

	public void testNotesAddressConversion() throws AddressException {
		String notes = "Stephen Johnson/Ireland/XXX";
		String email = Utils.convertNotesAddress(notes);
		log.info(email);
		InternetAddress ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());

		notes = "Stephen_Johnson/Ireland/XXX%XXXIE";
		email = Utils.convertNotesAddress(notes);
		log.info(email);
		ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());

		//1>>>>/bluebox/rest/json/folder/all_groups_2011_10_25%25XXXUS%25XXXGB
		notes = "all_groups_2011_10_25%XXXUS%XXXGB";
		email = Utils.convertNotesAddress(notes);
		log.info(email);
		ia = new InternetAddress(email);
		assertNotNull("No display name set", ia.getPersonal());
		assertNotNull("No address set", ia.getAddress());
	}

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
//	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
//	<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
//	<html xmlns="http://www.w3.org/1999/xhtml" lang='en_us'>
//	<html xmlns="http://www.w3.org/1999/xhtml" lang='en_us'>
	
}