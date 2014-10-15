package com.bluebox.smtp;

import javax.mail.internet.AddressException;

import com.bluebox.smtp.InboxAddress;

import junit.framework.TestCase;

public class InboxAddressTest extends TestCase {

	public void testNameRemoval() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
		assertEquals("Expected clean email address","steve@nowhere.com",a.getAddress());
		
		a = new InboxAddress("steve@nowhere.com");
		assertEquals("Expected clean email address","steve@nowhere.com",a.getAddress());
	}
	
	public void testDisplayName() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
		assertEquals("Expected display name","Steve Johnson",a.getDisplayName());
	}
	
	public void testNotesAddress() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson/MA/XXX");
		assertEquals("Expected clean email address","Steve_Johnson@MA.XXX",a.getAddress());
	}
	
	public void testParser() throws AddressException {
		String email = "postmaster@localhost";
		assertTrue(new InboxAddress(email).isValidAddress());
	}
}
