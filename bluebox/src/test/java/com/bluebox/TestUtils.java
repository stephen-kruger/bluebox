package com.bluebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

//import org.apache.commons.mail.EmailException;
//import org.apache.commons.mail.HtmlEmail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class TestUtils extends TestCase {
	private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

	@Test
	public void testLoadEML() throws MessagingException, IOException {
		MimeMessage message = Utils.loadEML(new FileInputStream("src/test/resources/test-data/m0017.txt"));
		assertNotNull("No subject",message.getSubject());
	}

	public static void waitFor(Inbox inbox, int count) throws Exception {
		int retryCount = 10*count;
		long reported=0;
		while ((retryCount-->0)&&((reported=inbox.getMailCount(BlueboxMessage.State.NORMAL))<count)) {
			try {
				log.info("Waiting for delivery expecting:{} reported:{}",count,reported);
				Thread.sleep(450);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (retryCount<=0) {
			log.warn("Timed out waiting for {} messages to arrive", count);
			throw new Exception("Timed out waiting for "+count+"messages to arrive");
		}
		else {
			log.info("Found expected message count received:{}",count);
		}
	}

	public static void sendMailDirect(Inbox inbox, String to, String from) throws Exception {
		log.debug("Delivering mail to "+to);
		MimeMessage message = Utils.createMessage(null,from, to, null,null, Utils.randomLine(25), Utils.randomLine(25));
		String uid = spoolMessage(StorageFactory.getInstance(),message);
		inbox.deliver(from, to, message, uid);
		//		removeSpooledMessage(StorageFactory.getInstance(),uid);
	}

	public static String spoolMessage(StorageIf si, MimeMessage message) throws Exception {
		return Utils.spoolStream(si,message);
	}

	//		public static InputStream getSpooledMessage(String uid) throws Exception {
	//			return StorageFactory.getInstance().getSpooledStream(uid);
	//		}

	//		public static void removeSpooledMessage(StorageIf si, String uid) throws Exception {
	//			si.removeSpooledStream(uid);
	//		}

	public static void addRandomDirect(StorageIf storage, int count) throws Exception {
		for (int i = 0; i < count; i++) {
			log.debug("Adding {} of {} random messages",i,count);
			addRandomDirect(storage);
		}
	}

	@Test
	public void testMimeMessageCreation() throws Exception {
		MimeMessage original = Utils.createMessage(null, "test@here.com", "test@here.com", "test@here.com", "test@here.com", "subject", "body");
		String spooledUid = Utils.spoolStream(StorageFactory.getInstance(),original);
		MimeMessage copy = StorageFactory.getInstance().getSpooledStream(spooledUid);
		StorageFactory.getInstance().removeSpooledStream(spooledUid);
		assertEquals("Mismatched subject",copy.getSubject(),original.getSubject());
	}

	public static BlueboxMessage addRandomDirect(StorageIf storage) throws Exception {
		MimeMessage mm = Utils.createMessage(null,"steve@there.com", "steve@here.com", "steve@here.com", "steve@here.com", Utils.randomLine(25), Utils.randomLine(25), Utils.randomLine(25));
		String rawuid = spoolMessage(storage,mm);
		Date received = storage.getUTCTime();
		BlueboxMessage message = storage.store(
				"steve@there.com",
				new InboxAddress("steve@here.com"),
				received,
				mm,
				rawuid);
		//		removeSpooledMessage(storage,uid);
		return message;
	}

	public static void addRandomNoThread(Inbox inbox, int count) throws Exception {
		Utils.generateNoThread(null, inbox, count);
	}

	//	public static MimeMessageWrapper createBlueBoxMimeMessage(Session session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, boolean attachment) throws MessagingException, IOException {
	//		return new MimeMessageWrapper(Utils.createMessage(session, from, to,  cc, bcc, subject, body, attachment));
	//	}

	public static void sendMailSMTP(InternetAddress from, InternetAddress to, InternetAddress cc, InternetAddress bcc, String subject, String body) throws Exception {
		TestUtils.sendMailSMTP(from, to, cc, bcc, subject, body,"text/plain");
	}
	
	public static void sendMailSMTP(InternetAddress from, InternetAddress to, InternetAddress cc, InternetAddress bcc, String subject, String body, String mimeType) throws Exception {
		String tos=null, ccs=null, bccs=null;
		if (to!=null)
			tos = to.toString();
		if (cc!=null)
			ccs = cc.toString();
		if (bcc!=null)
			bccs = bcc.toString();
		sendMailSMTP(from.toString(), tos, ccs, bccs,subject, body, mimeType);
	}

	public static void sendMailSMTP(String from, String to, String cc, String bcc, String subject, String body) throws Exception {
		sendMailSMTP(from, to, cc, bcc, subject,body, "text/plain");
	}
	
	public static void sendMailSMTP(String from, String to, String cc, String bcc, String subject, String body, String mimeType) throws Exception {
		sendMailSMTP(from, to, cc, bcc, subject, body, mimeType, new ArrayList<File>());
	}

	public static void sendMailSMTP(String from, String to, String cc, String bcc, String subject, String body, String mimeType, List<File> attachments) throws Exception {
		// Recipient's email ID needs to be mentioned.

		// Sender's email ID needs to be mentioned
		final String username = "username";
		final String password = "password";

		String host = "localhost";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", Config.getInstance().getString(Config.BLUEBOX_PORT));

		// Get the Session object.
//		Session session = Session.getInstance(props);
		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		try {
			// Create a default MimeMessage object.
			Message message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			if (to!=null)
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));
			if (cc!=null)
			message.setRecipients(Message.RecipientType.CC,
					InternetAddress.parse(cc));
			if (bcc!=null)
			message.setRecipients(Message.RecipientType.BCC,
					InternetAddress.parse(bcc));

			// Set Subject: header field
			message.setSubject(subject);

			// Send the actual HTML message, as big as you like
			message.setContent(
					body,
					mimeType);

			// Send message
			Transport.send(message);

			log.info("Sent message successfully....");

		} catch (MessagingException e) {
//			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	//	public static void sendMailSMTP(String from, String to, String cc, String bcc, String subject, String body, List<File> attachments) throws Exception {
	//		HtmlEmail email = new HtmlEmail();
	//		email.setHostName("localhost");
	//		email.setSmtpPort(Config.getInstance().getInt(Config.BLUEBOX_PORT));
	//		//email.setAuthenticator(new DefaultAuthenticator("username", "password"));
	//		email.setSSLCheckServerIdentity(false);
	//		email.setStartTLSRequired(false);
	//		email.setFrom(from);
	//		email.setSubject(subject);
	//		email.setMsg(body);
	//		email.setHtmlMsg("<html><head><script>alert('crap');</script></head><body><a href='http://test1.com'>test1</a>"+body+"<a href='http://test2.com'>test2</a></body></html>");
	//		if (to!=null)
	//			email.addTo(to);
	//		if (cc!=null)
	//			email.addCc(cc);
	//		if (bcc!=null)
	//			email.addBcc(bcc);
	//		
	//		for (File file : attachments) {
	//			try {
	//				log.debug("Adding attachment "+file.getCanonicalPath());
	//			} catch (IOException e) {
	//				e.printStackTrace();
	//			}
	//			email.attach(file);
	//		}
	//		email.send();
	//	}

	//	public static MimeMessage createMail(String from, String to, String cc, String bcc, String subject, String body) throws MessagingException, IOException, EmailException {
	//		return Utils.createMessage(null, from, to, cc, bcc, subject, body);
	//	}
	//
	//	public static MimeMessage createMail(InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body) throws IOException, MessagingException, EmailException {		return Utils.createMessage(null, from, to, cc, bcc, subject, body, false);
	//	}

	//	private static MimeBodyPart createAttachment(HttpServletRequest req) throws MessagingException, IOException, EmailException {
	//		String[] names = new String[] {
	//				"MyDocument.odt",
	//				"MyPresentation.odp",
	//				"MySpreadsheet.ods",
	//				"GettingStarted.txt",
	//				"message.png",
	//				"bigpic.jpg",
	//				"BlueBox.png",
	//				"cv-template-Marketing-Manager.doc"
	//		}; 
	//		String[] extensions = new String[] {
	//				".odt",
	//				".odp",
	//				".ods",
	//				".txt",
	//				".png",
	//				".jpg",
	//				".png",
	//				"doc"
	//		}; 
	//		String[] mime = new String[] {
	//				"application/document odt ODT",
	//				"application/presentation odp ODP",
	//				"application/spreadsheet ods ODS",
	//				"text/plain txt TXT",
	//				"image/png png PNG",
	//				"image/jpeg jpg JPG",
	//				"image/png png PNG",
	//				"application/document doc DOC"
	//		}; 
	//
	//		if (req!=null) {
	//			Random r = new Random();
	//			int index = r.nextInt(extensions.length); 
	//			String name = Integer.toString(r.nextInt(99))+"-"+names[index];
	//			String mimeType = mime[index];
	//			InputStream content = req.getSession().getServletContext().getResourceAsStream("data/"+names[index]);
	//
	//			MimeBodyPart mbp = new MimeBodyPart(content);
	//			mbp.setFileName(name);
	//			
	//			mbp.setDisposition(mimeType);
	//			return mbp;
	//		}
	//		return null;
	//	}
}
