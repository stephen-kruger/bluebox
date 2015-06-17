package com.bluebox.smtp;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;

public class InboxAddress extends Object {
	private static final Logger log = LoggerFactory.getLogger(InboxAddress.class);

	private String address;

	public InboxAddress(String s) {
		this.address = s;
		// for test case testNoDomain
		if (address.indexOf('@')<=0)
			address = getAddress();
	}

	public InboxAddress(HttpServletRequest req) {
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
			email = StringEscapeUtils.unescapeJava(email);
			// check if it's a Notes address
			if (Utils.isNotesAddress(email)) {
				log.info("Converting Notes style address :{}",email);
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
			log.error("Error for {}",email,e);
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
		
		String domain =  EmailAddress.getDomain(address);
		if (domain==null)
			return "localhost";
		return domain;
	}

	public boolean isValidAddress() {
		return EmailAddress.isValidMailbox(address);
	}
}
