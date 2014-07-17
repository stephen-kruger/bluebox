package com.bluebox;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageIf;

public class TestUtils {
	private static final Logger log = Logger.getAnonymousLogger();

	public static void sendMailDirect(StorageIf storage, String to, String from) throws Exception {
		log.fine("Delivering mail to "+to);
		Inbox inbox = Inbox.getInstance();
		MimeMessageWrapper message = TestUtils.createBlueBoxMimeMessage(null,
				new InternetAddress(from), 
				new InternetAddress[]{new InternetAddress(to)}, 
				new InternetAddress[]{},
				new InternetAddress[]{},
				Utils.randomLine(25),//"This is the subject", 
				"This is the body",
				true);
		inbox.deliver(from, to, message.getInputStream());
	}

	public static void addRandom(StorageIf storage, int count) throws Exception {
		for (int i = 0; i < count; i++) {
			log.fine("Adding "+i+" of "+count+" random messages");
			addRandom(storage);
		}
	}

	public static BlueboxMessage addRandom(StorageIf storage) throws Exception {
		BlueboxMessage message = storage.store(new InboxAddress("steve@here.com"),
				"steve@there.com",
				createBlueBoxMimeMessage(null,
						new InternetAddress("steve@there.com"), 
						new InternetAddress[]{new InternetAddress("steve@here.com")}, 
						new InternetAddress[]{new InternetAddress("steve@cc.com")},
						new InternetAddress[]{new InternetAddress("steve@bcc.com")},
						Utils.randomLine(25),//"This is the subject", 
						"This is the body",
						true));
		return message;
	}

	public static MimeMessageWrapper createBlueBoxMimeMessage(Session session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, boolean attachment) throws MessagingException, IOException {
		return new MimeMessageWrapper(Utils.createMessage(session, from, to,  cc, bcc, subject, body, attachment));
	}

	public static void sendMailSMTP(InternetAddress from, InternetAddress to, InternetAddress cc, InternetAddress bcc, String subject, String body) throws EmailException {
		String tos=null, ccs=null, bccs=null;
		if (to!=null)
			tos = to.toString();
		if (cc!=null)
			ccs = cc.toString();
		if (bcc!=null)
			bccs = bcc.toString();
		sendMailSMTP(from.toString(), tos, ccs, bccs,subject, body);
	}

	public static void sendMailSMTP(String from, String to, String cc, String bcc, String subject, String body) throws EmailException {
		Email email = new SimpleEmail();
		email.setHostName("localhost");
		email.setSmtpPort(Config.getInstance().getInt(Config.BLUEBOX_PORT));
		//email.setAuthenticator(new DefaultAuthenticator("username", "password"));
		email.setSSLOnConnect(false);
		email.setFrom(from);
		email.setSubject(subject);
		email.setMsg(body);
		if (to!=null)
			email.addTo(to);
		if (cc!=null)
			email.addCc(cc);
		if (bcc!=null)
			email.addBcc(bcc);
		email.send();
	}
	
	public static InputStream createMail(String from, String to, String cc, String bcc, String subject, String body) throws EmailException, IOException, MessagingException {
		Email email = new SimpleEmail();
		email.setHostName(Utils.getHostName());
		email.setSmtpPort(Config.getInstance().getInt(Config.BLUEBOX_PORT));
		//email.setAuthenticator(new DefaultAuthenticator("username", "password"));
		email.setSSLOnConnect(false);
		email.setFrom(from);
		email.setSubject(subject);
		email.setMsg(body);
		if (to!=null)
			email.addTo(to);
		if (cc!=null)
			email.addCc(cc);
		if (bcc!=null)
			email.addBcc(bcc);
		return email.getMimeMessage().getInputStream();
	}

	//	public static MimeMessage createMessage(Session session, String from, String to, String cc, String bcc, String subject, String body) throws MessagingException {
	//		MimeMessage msg = new MimeMessage(session);
	//		msg.setFrom(new InternetAddress(from));
	//		msg.setSubject(subject);
	//		msg.setSentDate(new Date());
	//		msg.setText(body);
	//		if (to!=null) {
	//			msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
	//		}
	//		if (cc!=null) {
	//			log.info("CC:"+cc);
	//			msg.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));			
	//		}
	//		if (bcc!=null) {
	//			msg.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));			
	//		}
	//		return msg;
	//	}

}
