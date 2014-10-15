package com.bluebox.smtp;


import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;

public class InboxAddress extends Object {
	private static final Logger log = LoggerFactory.getLogger(InboxAddress.class);

	private String address;

	public InboxAddress(String s) throws AddressException {
		this.address = s;
	}

	public InboxAddress(HttpServletRequest req) throws AddressException {
		this(req.getParameter(Inbox.EMAIL));
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

	public static final String getEmail(String email) {
		try {
			if ((email==null)||(email.trim().length()==0)) {
				return "";
			}
			email = Utils.decodeRFC2407(email);
			// check if it's a Notes address
			if (Utils.isNotesAddress(email)) {
				log.info("Converting Notes style address :"+email);
				email = Utils.convertNotesAddress(email);
			}
			else {
				// if no domain specified, add default
				if (email.indexOf('@')<0) {
					email += '@'+Utils.getHostName();
				}
			}

//			InternetAddress address = new InternetAddress(Utils.decodeRFC2407(email));
//			return address.getAddress();
			return EmailAddress.getInternetAddress(email).getAddress();
		}
		catch (Throwable e) {
			log.debug("Error for {}",email);
			//e.printStackTrace();
		}
		return "*";
	}

	public String getDisplayName() {
		try {
			String p = EmailAddress.getPersonalName(address);//new InternetAddress(address).getPersonal();
			if (p!=null) {
				if (p.length()>0) {
					return p;
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return address;
	}

	public String getDomain() {
		return EmailAddress.getDomain(address);
	}

	public boolean isValidAddress() {
		return EmailAddress.isValidMailbox(address);
	}
}
