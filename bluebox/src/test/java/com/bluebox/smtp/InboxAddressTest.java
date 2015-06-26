package com.bluebox.smtp;

import javax.mail.internet.AddressException;

import junit.framework.TestCase;

import org.junit.Test;

public class InboxAddressTest extends TestCase {

	@Test
	public void testNameRemoval() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
		assertEquals("Expected clean email address","steve@nowhere.com",a.getAddress());
		
		a = new InboxAddress("steve@nowhere.com");
		assertEquals("Expected clean email address","steve@nowhere.com",a.getAddress());
	}
	
	@Test
	public void testDisplayName() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
		assertEquals("Expected display name","Steve Johnson",a.getDisplayName());
	}
	
	@Test
	public void testNotesAddress() throws AddressException {
		InboxAddress a = new InboxAddress("Steve Johnson/MA/XXX");
		assertEquals("Expected clean email address","Steve_Johnson@MA.XXX",a.getAddress());
	}
	
	@Test
	public void testNoDomain() throws AddressException {
		InboxAddress a = new InboxAddress("steve");
		assertTrue("invalid email address",a.isValidAddress());
//		assertEquals("Expected clean email address","Steve_Johnson@MA.XXX",a.getAddress());
	}
	
	@Test
	public void testParser() throws AddressException {
		String email1 = "postmaster@localhost";
		assertTrue(new InboxAddress(email1).isValidAddress());
		String email2 = "example.com!nobody@[9.32.154.176]";
		assertTrue(new InboxAddress(email2).isValidAddress());
		assertNotNull(new InboxAddress(email2).getDomain());
	}
	
	@Test
	public void testQuotedForm() {
		String email = "L’arrivée étendus9605 <queenelizabeth9605@smtphouse.com>";
		assertEquals("Email did not match","queenelizabeth9605@smtphouse.com",InboxAddress.getEmail(email));
		assertEquals("Email did not match","L’arrivée étendus9605",new InboxAddress(email).getDisplayName());
	}
}
