package com.bluebox.servlet;

import com.bluebox.TestUtils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import java.io.IOException;

public class BlueBoxServletTest extends BaseServletTest {
    private static final Logger log = LoggerFactory.getLogger(BlueBoxServletTest.class);

    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testBlacklistTo() {
        Inbox inbox = Inbox.getInstance();

        try {
            TestUtils.sendMailSMTP(new InternetAddress("test@here.com"), new InternetAddress("steve@blackdomain.com"), null, null, "subject", "body");
            fail("Mail to not blacklisted");
        } catch (Exception e) {
//			fail("Mail to not blacklisted");
            log.info("Got expected exception");
        }

        inbox.addToBlacklist("qwerty.com");

        try {
            TestUtils.sendMailSMTP(new InternetAddress("test@here.com"), new InternetAddress("steve@qwerty.com"), null, null, "subject", "body");

            fail("Mail to not blacklisted");
        } catch (Exception e) {
            log.info("Got expected exception");
        }
        inbox.stop();
    }

    @Test
    public void testBlacklistFrom() throws Exception {
        Inbox inbox = Inbox.getInstance();
        inbox.addFromBlacklist("qwerty.com");
        assertTrue(inbox.isFromBlackListed(new InboxAddress("test@qwerty.com")));
        assertFalse(inbox.isToBlackListed(new InboxAddress("test@qwerty.com")));
        assertFalse(inbox.accept("from@qwerty.com", "to@here.com"));
        assertTrue(inbox.accept("from@test.com", "to@qwerty.com"));
        inbox.stop();
    }

    @Test
    public void testToWhitelist() throws Exception {
        Inbox inbox = Inbox.getInstance();
        inbox.addToWhiteList("qwerty.com");
        assertTrue(inbox.isToWhiteListed(new InboxAddress("test@qwerty.com")));
        assertFalse(inbox.isFromWhiteListed(new InboxAddress("test@qwerty.com")));
        assertFalse(inbox.accept("test@here.com", "steve@here.com"));
        assertTrue(inbox.accept("test@qwerty.com", "steve@qwerty.com"));
        inbox.stop();
    }


}
