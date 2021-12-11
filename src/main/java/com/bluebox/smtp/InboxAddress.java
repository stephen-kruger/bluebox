package com.bluebox.smtp;


import com.bluebox.Utils;
import com.bluebox.utils.FallbackValidator;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import java.util.StringTokenizer;

public class InboxAddress extends Object {
    private static final Logger log = LoggerFactory.getLogger(InboxAddress.class);

    private String address;

    public InboxAddress(String s) {
        this.address = s;
        // for test case testNoDomain
        if (address.indexOf('@') <= 0)
            address = getAddress();
    }

    public InboxAddress(HttpServletRequest req) {
        this(req.getParameter(Inbox.EMAIL));
    }

    public static final String getEmail(String email) {
        try {
            if ((email == null) || (email.trim().length() == 0)) {
                return "";
            }
            email = Utils.decodeRFC2407(email);
            email = StringEscapeUtils.unescapeJava(email);

            // remove trailing slash if needed
            if (email.endsWith("/")) {
                email = email.substring(0, email.length() - 1);
            }
            // check if it's a Notes address
            if (Utils.isNotesAddress(email)) {
                log.info("Converting Notes style address :{}", email);
                email = Utils.convertNotesAddress(email);
            } else {
                // if no domain specified, add default
                if (email.indexOf('@') < 0) {
                    email += '@' + Utils.getHostName();
                }
            }

            //			return EmailAddress.getInternetAddress(Utils.decodeRFC2407(email)).getAddress();
            return EmailAddress.getInternetAddress(email).getAddress();
        } catch (Throwable e) {
            log.error("Error parsing {}", email);
            try {
                InternetAddress address = new InternetAddress(Utils.decodeRFC2407(email));
                return address.getAddress();
            } catch (Throwable t) {
                // try one more time using Apache Commons parser
                try {
                    FallbackValidator.getInternetAddress(email).getAddress();
                } catch (Throwable t2) {
                    log.error("Giving up trying to parse {}", email);
                    // throw away leading text with spaces
                    StringTokenizer tok = new StringTokenizer(email, " ");
                    String nospace = email;
                    while (tok.hasMoreTokens()) {
                        nospace = tok.nextToken();
                    }
                    // ok this address is really not good. try replace spaces
                    try {
                        return nospace.replace('+', ' ').replaceAll(" ", "_").trim();
                    } catch (Throwable t3) {
                        return "bumemailaddress@localhost.com";
                    }
                }
            }
        }
        return email;
    }

    @Override
    public String toString() {
        return getFullAddress();
    }

    public String getAddress() {
        return getEmail(address);
    }

    public String getFullAddress() {
        return address;
    }

    public String getDisplayName() {
        try {
            String p = new InternetAddress(address).getPersonal();
            //			String p = EmailAddress.getPersonalName(address);
            if (p != null) {
                if (p.length() > 0) {
                    return p;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return address;
    }

    public String getDomain() {

        String domain = EmailAddress.getDomain(address);
        if (domain == null)
            return "localhost";
        return domain;
    }

    public boolean isValidAddress() {
        return EmailAddress.isValidMailbox(address);
    }
}
