package com.bluebox;

import java.io.IOException;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageIf;

public class TestUtils {
	private static final Logger log = Logger.getAnonymousLogger();

	public static MimeMessageWrapper createBlueBoxMimeMessage(Session session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, boolean attachment) throws MessagingException, IOException {
		return new MimeMessageWrapper(Utils.createMessage(session, from, to,  cc, bcc, subject, body, attachment));
	}

	public static void sendMail(StorageIf storage, String to, String from) throws Exception {
		log.fine("Delivering mail to "+to);
		Inbox inbox = Inbox.getInstance();
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(Utils.getSession(),
				new InternetAddress(from), 
				new InternetAddress[]{new InternetAddress(to)}, 
				new InternetAddress[]{},
				new InternetAddress[]{},
				Utils.randomLine(25),//"This is the subject", 
				"This is the body",
				true);
		inbox.deliver(from, to, message.getInputStream());
//		MessageImpl message = storage.store(to,to,message);
//		SearchIndexer.getInstance().indexMail(message);
	}
	
	public static void addRandom(StorageIf storage, int count) throws Exception {
		for (int i = 0; i < count; i++) {
			log.fine("Adding "+i+" of "+count+" random messages");
			addRandom(storage);
		}
	}

	public static BlueboxMessage addRandom(StorageIf storage) throws Exception {
//		MimeMessageWrapper message = createBlueBoxMimeMessage(Utils.getSession(),
//				new InternetAddress("steve@there.com"), 
//				new InternetAddress[]{new InternetAddress("steve@here.com")}, 
//				new InternetAddress[]{new InternetAddress("steve@cc.com")},
//				new InternetAddress[]{new InternetAddress("steve@bcc.com")},
//				Utils.randomLine(25),//"This is the subject", 
//				"This is the body",
//				true);
//		Inbox inbox = Inbox.getInstance();
//		inbox.deliver("steve@there.com", "steve@here.com", message.getInputStream());
		BlueboxMessage message = storage.store(new InboxAddress("steve@here.com"),
				"steve@there.com",
				createBlueBoxMimeMessage(Utils.getSession(),
						new InternetAddress("steve@there.com"), 
						new InternetAddress[]{new InternetAddress("steve@here.com")}, 
						new InternetAddress[]{new InternetAddress("steve@cc.com")},
						new InternetAddress[]{new InternetAddress("steve@bcc.com")},
						Utils.randomLine(25),//"This is the subject", 
						"This is the body",
						true));
		return message;
//		MessageImpl m = new MessageImpl(Long.toString(new java.util.Date().getTime()),new InboxAddress("steve@here.com"));
//		m.setBlueBoxMimeMessage(message);
//		return m;
	}
	
}
