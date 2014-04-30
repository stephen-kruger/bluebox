package com.bluebox.smtp;

import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import com.bluebox.Utils;

public class InboxAddress extends Object {
	private static final Logger log = Logger.getAnonymousLogger();

	private String address;

	public InboxAddress(String s) throws AddressException {
		this.address = s;
	}

	public InboxAddress(HttpServletRequest req) throws AddressException {
		this(req.getParameter(Inbox.EMAIL));
	}
	
	

	@Override
	public String toString() {
		return getAddress();
	}

	public String getAddress() {
		return getEmail(address);
	}
	
	public String getFullAddress() {
		return address;
	}

	private static final String getEmail(String email) {
		try {
			if ((email==null)||(email.trim().length()==0)) {
				return "";
			}
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

			InternetAddress address = new InternetAddress(Utils.decodeRFC2407(email));
			//			return StorageImpl.escape(address.getAddress());
			//			return address.getAddress().replace('<', ' ').replace('>', ' ').trim();
			return address.getAddress();
		}
		catch (Throwable e) {
			log.fine(e.getMessage()+" "+email);
			//e.printStackTrace();
		}
		return "*";
	}
}
