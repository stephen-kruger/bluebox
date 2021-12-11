package com.bluebox.smtp;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;

public class InboxAddressTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(InboxAddressTest.class);

    @Test
    public void testNameRemoval() throws AddressException {
        InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
        assertEquals("Expected clean email address", "steve@nowhere.com", a.getAddress());

        a = new InboxAddress("steve@nowhere.com");
        assertEquals("Expected clean email address", "steve@nowhere.com", a.getAddress());
    }

    @Test
    public void testDisplayName() throws AddressException {
        InboxAddress a = new InboxAddress("Steve Johnson <steve@nowhere.com>");
        assertEquals("Expected display name", "Steve Johnson", a.getDisplayName());
    }

    @Test
    public void testNotesAddress() throws AddressException {
        InboxAddress a = new InboxAddress("Steve Johnson/MA/XXX");
        assertEquals("Expected clean email address", "Steve_Johnson@MA.XXX", a.getAddress());
    }

    @Test
    public void testNoDomain() throws AddressException {
        InboxAddress a = new InboxAddress("steve");
        assertTrue("invalid email address", a.isValidAddress());
    }

    @Test
    public void testSpace() throws AddressException {
        InboxAddress a = new InboxAddress("sandy4 user3@ghvm352.lotus.com");
        log.info(a.getAddress());
        assertEquals("invalid email address", "user3@ghvm352.lotus.com", a.getAddress());
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
        String email = "L�arriv�e �tendus9605 <queenelizabeth9605@smtphouse.com>";
        assertEquals("Email did not match", "queenelizabeth9605@smtphouse.com", InboxAddress.getEmail(email));
        assertEquals("Email did not match", "L�arriv�e �tendus9605", new InboxAddress(email).getDisplayName());
    }
}
