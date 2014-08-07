package com.bluebox;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageIf;

public class TestUtils extends TestCase {
	private static final Logger log = Logger.getAnonymousLogger();

	public void testLoadEML() throws MessagingException, IOException {
		MimeMessage message = Utils.loadEML(new FileInputStream("src/test/resources/test-data/m0017.txt"));
		assertNotNull("No subject",message.getSubject());
	}
	
	public static void sendMailDirect(StorageIf storage, String to, String from) throws Exception {
		log.fine("Delivering mail to "+to);
		Inbox inbox = Inbox.getInstance();
		MimeMessage message = Utils.createMessage(null,from, to, null,null, Utils.randomLine(25), Utils.randomLine(25));
		inbox.deliver(from, to, Utils.streamMimeMessage(message));
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
				new Date(),
				Utils.createMessage(null,"steve@there.com", "steve@here.com", "steve@here.com", "steve@here.com", Utils.randomLine(25), Utils.randomLine(25)));
		return message;
	}

	//	public static MimeMessageWrapper createBlueBoxMimeMessage(Session session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, boolean attachment) throws MessagingException, IOException {
	//		return new MimeMessageWrapper(Utils.createMessage(session, from, to,  cc, bcc, subject, body, attachment));
	//	}

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
		HtmlEmail email = new HtmlEmail();
		email.setHostName("localhost");
		email.setSmtpPort(Config.getInstance().getInt(Config.BLUEBOX_PORT));
		//email.setAuthenticator(new DefaultAuthenticator("username", "password"));
		email.setSSLOnConnect(false);
		email.setFrom(from);
		email.setSubject(subject);
		email.setMsg(body);
		email.setHtmlMsg("<html><head><script>alert('crap');</script></head><body>"+body+"</body></html>");
		if (to!=null)
			email.addTo(to);
		if (cc!=null)
			email.addCc(cc);
		if (bcc!=null)
			email.addBcc(bcc);
		email.send();
	}

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
