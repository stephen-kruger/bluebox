package com.bluebox;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.fileupload.util.mime.MimeUtility;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import com.bluebox.rest.json.AbstractHandler;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class Utils {
	public static final String UTF8 = "UTF-8";
	private static final Logger log = Logger.getAnonymousLogger();
	//	private static Config config = Config.getInstance();
	private static Map<String,File> cachedFiles = new HashMap<String,File>();
	private static int counter=0;

	public static String getHostName() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			String hostname;
			if (addr.getHostName()!=null) {
				hostname = addr.getHostName();
			}
			else {
				hostname = "localhost";
			}
			return hostname;
		} 
		catch (UnknownHostException e) {
			return "localhost";
		}
	}

	public static String convertStreamToString(java.io.InputStream is) throws IOException {
		Scanner scanner = new Scanner(is);
		scanner.useDelimiter("\\A");
		String result = scanner.hasNext() ? scanner.next() : "";
		is.close();
		scanner.close();
		return result;
	}

	public static InputStream convertStringToStream(String s) {
		try {
			return new ByteArrayInputStream(s.getBytes("UTF-8"));
		} 
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static String[] getStringArray(Object obj) {
		if (obj instanceof String[]) {
			return (String[])obj;
		}
		if (obj instanceof Collection) {
			String[] res = new String[((Collection<String>)obj).size()];
			return ((Collection<String>)obj).toArray(res);
		}		
		if (obj instanceof JSONArray) {
			JSONArray ja = (JSONArray)obj;
			String[] res = new String[ja.length()];
			for (int i = 0; i < res.length; i++) {
				try {
					res[i] = ja.getString(i);
				} 
				catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return res;
		}
		return new String[]{obj.toString()};
	}

	public static JSONArray getJSONArray(String[] arr) throws JSONException {
		JSONArray res = new JSONArray();
		if (arr!=null) {
			for (int i = 0; i < arr.length;i++) {
				res.put(i,arr[i]);
			}
		}
		return res;
	}

	public static JSONArray getJSONArray(Collection<String> coll) throws JSONException {
		return getJSONArray(coll,false);
	}

	public static JSONArray getJSONArray(Collection<String> coll, boolean escapeHTML) throws JSONException {
		JSONArray res = new JSONArray();
		int i = 0;
		for (String s : coll) {
			//			if (escapeHTML) {
			//				res.put(i,escapeHTML(s));
			//			}
			//			else {
			res.put(i,s);
			//			}
			i++;
		}
		return res;
	}

	public static JSONArray getJSONArray(Address[] addr) throws JSONException {
		JSONArray res = new JSONArray();
		if (addr!=null) {
			for (int i = 0; i < addr.length;i++) {
				res.put(i,Utils.decodeQuotedPrintable(addr[i].toString()));
			}
		}
		return res;
	}	

	/*
	 * Returns the name portion of an email address
	 * e.g. "Stephen Johnson" <stephen@mail.com> will return "Stephen Johnson"
	 */
	public static final String getEmailName(String email) {
		try {
			if ((email==null)||(email.trim().length()==0)) {
				return "*";
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

			InternetAddress address = new InternetAddress(email);
			return address.getPersonal()+"";
		}
		catch (Throwable e) {
			log.fine(e.getMessage()+" "+email);
			e.printStackTrace();
		}
		return "";
	}

	//	public static final String getEscapedEmail(String email) {
	//		return StorageImpl.escape(getEmail(email));
	//	}

	public static String uploadEML(InputStream eml) throws IOException, MessagingException {
		if (eml==null) {
			log.severe("Could not load eml resource");
			return "Could not load eml resource";
		}
		try {
			//			Session sess = getSession();
			//			log.info("Session retrieved :"+sess);
			//MimeMessage message = new MimeMessage(sess, eml);
//			String mstr = Utils.convertStreamToString(eml);
			//log.info(mstr);
			MimeMessage message = loadEML(eml);
			sendMessageDirect(message);
			eml.close();
			return "Loaded email ok";
		}  
		catch (Throwable e) {
			e.printStackTrace();
			return e.toString()+":"+e.getMessage();
		}
		finally {
			try {
				eml.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static MimeMessage loadEML(InputStream emlStream) throws MessagingException, IOException {
//				Session sess = getSession();
		MimeMessage message = new MimeMessage(null,emlStream);
		emlStream.close();
		if (message.getSubject()==null)
			new Exception().printStackTrace();
		return message;
	}

	//	public static void testInlineImages(Inbox inbox) {
	//		try {
	//			Session sess = getSession();
	//			MimeMessage message = new MimeMessage(sess);
	//	        message.setSubject("HTML  mail with images");
	//	        message.setFrom(new InternetAddress("me@sender.com"));
	//	        message.addRecipient(Message.RecipientType.TO,new InternetAddress("you@receiver.com"));
	//
	//	        //
	//	        // This HTML mail have to 2 part, the BODY and the embedded image
	//	        //
	//	        Multipart multipart = new MimeMultipart("related");
	//
	//	        // first part  (the html)
	//	        BodyPart messageBodyPart = new MimeBodyPart();
	//	        String htmlText = "<H1>Hello</H1><img src=\"cid:image\">";
	//	        messageBodyPart.setContent(htmlText, "text/html; charset=\""+MimeMessageWrapper.UTF8+"\"");
	//
	//	        // add it
	//	        multipart.addBodyPart(messageBodyPart);
	//	        
	//	        // second part (the image)
	//	        messageBodyPart = new MimeBodyPart();
	//	        DataSource fds = new FileDataSource("C:\\workspaces\\workspace.galileo\\bluebox\\scratch\\BlueBox2.png");
	//	        messageBodyPart.setDataHandler(new DataHandler(fds));
	//	        messageBodyPart.setHeader("Content-ID","<image>");
	//
	//	        // add it
	//	        multipart.addBodyPart(messageBodyPart);
	//
	//	        // put everything together
	//	        message.setContent(multipart);
	//
	//			Transport.send(message);
	//		}  
	//		catch (MessagingException e) {
	//			e.printStackTrace();
	//		}
	//	}

	public static void test(Inbox inbox, String sz) {
		log.info("Into test");
		sendMessage(Integer.parseInt(sz));
	}

	public static InternetAddress[] getRandomAddresses(int count) throws AddressException {
		InternetAddress[] result = new InternetAddress[count];
		for (int i = 0; i < count ;i++) {
			result[i] = getRandomAddress();
		}
		return result;
	}

	public static InternetAddress[] getRandomAddress(int count) throws AddressException {
		InternetAddress[] result = new InternetAddress[count];
		for (int i = 0; i < count; i++) {
			result[i] = getRandomAddress();
		}
		return result;
	}

	public static InternetAddress getRandomAddress() throws AddressException {
		String[] firstNames = {"Stephen","Bob","John","Jake","Jane","Sally","Joe","Jenny","George","Anna","Monica","Chiara","Belinda","Lauren","Caylee","Mathis"};
		String[] lastNames = {"Johnsen","Jones","Gomez","DuPont","Smith","Williams","Johnson","White","Black","Brown","DuBois"};
		String[] domains = {"somewhere.com","xxx.com","test.com","tulip.com","bluebox.tulip.com","test.xxx.com"};		
		Random random = new Random();
		int fn = random.nextInt(firstNames.length);
		int ln = random.nextInt(lastNames.length);
		int dm = random.nextInt(domains.length);
		InternetAddress addr = new InternetAddress(firstNames[fn]+" "+lastNames[ln]+" <"+firstNames[fn].toLowerCase()+"."+lastNames[ln].toLowerCase()+"@"+domains[dm].toLowerCase()+">");
		return addr;
	}

	public static String randomLine(int length) {
		Random random = new Random();
		String line = RandomStringUtils.randomAlphabetic(random.nextInt(6)+1);
		while (line.length() < length) {
			line += RandomStringUtils.randomAlphabetic(random.nextInt(6)+1)+" ";
		}
		return line;
	}

	public static String randomText(int lines) {
		String text = "";
		for (int j = 0; j < lines; j++) {
			text += randomLine(25)+"\n\r";
		}
		return text;
	}

//		private static Properties getMailProperties() {
//			Properties mailProps = new Properties();
//			// http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
//			mailProps.setProperty("mail.smtp.host", Utils.getHostName());
//			mailProps.setProperty("mail.smtp.port", "" + Config.getInstance().getString(Config.BLUEBOX_PORT));
//			mailProps.setProperty("mail.smtp.sendpartial", "true");
//			//		mailProps.setProperty("mail.smtp.auth", "true");
//			mailProps.setProperty("mail.smtp.starttls.enable","false");
//			//		mailProps.setProperty("mail.smtp.ssl.trust","*");
//			return mailProps;
//		}

	//	public static void sendSingleMessage(int count) {
	//		boolean sent = false;
	//		int retryCount = 5;
	//		while ((!sent)&&(retryCount-->0)) {
	//			log.info("Sending message");
	//			try {
	//				MimeMessage msg = createMessage(getSession(),
	//						getRandomAddress(),  
	//						getRandomAddresses(1),//to
	//						getRandomAddresses(0),//cc
	//						getRandomAddresses(0),//bcc
	//						(counter++)+" "+randomLine(35), 
	//						randomText(14),
	//						true);
	//				if (Inbox.getInstance().accept(msg.getFrom()[0].toString(), msg.getRecipients(RecipientType.TO)[0].toString())) {
	//					Inbox.getInstance().deliver(msg.getFrom()[0].toString(), msg.getRecipients(RecipientType.TO)[0].toString(), msg.getInputStream());
	//					sent = true;
	//				}
	//			} 
	//			catch (Throwable e) {
	//				// server overloaded, try again later
	//				try {
	//					log.info("Waiting to deliver ("+e.getMessage()+")");
	//					Thread.sleep(5000);
	//				} 
	//				catch (InterruptedException e1) {
	//					e1.printStackTrace();
	//				}
	//			}
	//		}
	//	}
	public static void sendMessage(final int count) {
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		for (int j = 0; j < count/6; j++) {
			log.info("Sending message "+j);
			threadPool.execute(new Runnable() {

				@Override
				public void run() {
					boolean sent = false;
					int retryCount = 5;
					while ((!sent)&&(retryCount-->0)) {
						log.info("Sending message");
						try {
							MimeMessage msg = createMessage(null,
									getRandomAddress(),  
									getRandomAddresses(2),//to
									getRandomAddresses(2),//cc
									getRandomAddresses(2),//bcc
									(counter++)+" "+randomLine(35), 
									randomText(14),
									true);

							sendMessageDirect(msg);
							sent = true;

						} 
						catch (Throwable e) {
							// server overloaded, try again later
							try {
								log.info("Waiting to deliver ("+e.getMessage()+")");
								Thread.sleep(5000);
							} 
							catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
					}
				}



			});
		}		
	}

	private static void sendMessageDirect(MimeMessage msg) throws Exception {
		Address[] to = msg.getRecipients(RecipientType.TO);
		Address[] cc = msg.getRecipients(RecipientType.CC);
		Address[] bcc = msg.getRecipients(RecipientType.BCC);
		List<String> recipients = new ArrayList<String>();
		if (to!=null)
			for (int i = 0; i < to.length;i++)
				recipients.add(to[i].toString());
		if (cc!=null)
			for (int i = 0; i < cc.length;i++)
				recipients.add(cc[i].toString());
		if (bcc!=null)
			for (int i = 0; i < bcc.length;i++)
				recipients.add(bcc[i].toString());
		for (String recipient : recipients) {
			if (Inbox.getInstance().accept(msg.getFrom()[0].toString(), recipient)) {
				Inbox.getInstance().deliver(msg.getFrom()[0].toString(), recipient, Utils.streamMimeMessage(msg));
			}
		}

	}

//		public static Session getSession() {
//			Properties mailProps = getMailProperties();
//			Session session = Session.getInstance(mailProps, null);
//			session.setDebug(false);
//			return session;
//		}
	//
	//	public static void sendMessage(InternetAddress from, String subject, String body, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, boolean attachment) throws MessagingException, IOException {
	//		Properties mailProps = getMailProperties();
	//		Session session = Session.getInstance(mailProps, null);
	//		session.setDebug(false);
	//		MimeMessage msg = createMessage(session, from, to, cc, bcc, subject, body, attachment);
	//		Transport.send(msg);
	//	}

	public static void waitFor(int i) {
		Inbox inbox = Inbox.getInstance();
		int retryCount = 5;
		while ((retryCount-->0)&&(inbox.getMailCount(BlueboxMessage.State.NORMAL)<i)) {
			try {
				log.info("Waiting for delivery "+i);
				Thread.sleep(250);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (retryCount<=0)
			log.warning("Timed out waiting for messages to arrive");
		else
			log.info("Found expected message count received");
	}

	public static String trimURLParam(String p) {
		if (p.endsWith("/")) {
			return p.substring(0,p.length()-1);
		}
		else {
			return p;
		}
	}

	public static MimeMessage createMessage(Session session, String from, String to, String cc, String bcc, String subject, String body) throws MessagingException, IOException {
		InternetAddress[] toa=new InternetAddress[0], cca=new InternetAddress[0], bcca=new InternetAddress[0];
		if (to!=null)
			toa = new InternetAddress[]{new InternetAddress(to)};
		if (cc!=null)
			cca = new InternetAddress[]{new InternetAddress(cc)};
		if (bcc!=null)
			bcca = new InternetAddress[]{new InternetAddress(bcc)};
		return createMessage(session, 
				new InternetAddress(from),
				toa,
				cca,
				bcca,
				subject,
				body,
				false);
	}

	public static MimeMessage createMessage(Session session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String body, boolean attachment) 
			throws MessagingException, IOException {
		MimeMessage msg = new MimeMessage(session);
		msg.setFrom(from);
		msg.setSubject(subject,UTF8);
		msg.setSentDate(new Date());
		if (to.length>0)
			msg.addRecipients(Message.RecipientType.TO, to);
		if (cc.length>0)
			msg.setRecipients(Message.RecipientType.CC, cc);
		if (bcc.length>0)
			msg.setRecipients(Message.RecipientType.BCC, bcc);

		if (attachment) {
			// set the body
			Multipart multipart = new MimeMultipart();

			// create the text message part 
			MimeBodyPart textBodyPart = new MimeBodyPart();
			textBodyPart.setContent(body,"text/plain; charset=\""+UTF8+"\"");
			multipart.addBodyPart(textBodyPart);

			// create the html message part 
			MimeBodyPart htmlBodyPart = new MimeBodyPart();
			htmlBodyPart.setContent("<font color=\"red\">"+body.replaceAll("\n", "</br>")+"</font>","text/html; charset=\""+UTF8+"\"");

			//htmlBodyPart.setHeader("Content-Type","text/plain; charset=\"utf-8\"");
			//htmlBodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
			multipart.addBodyPart(htmlBodyPart);

			// randomly create up to 5 attachment
			int attachmentCount = new Random().nextInt(5);
			for (int i = 0; i < attachmentCount; i++)
				multipart.addBodyPart(createAttachment());

			// Put parts in message
			msg.setContent(multipart);
		}
		else {
			msg.setText(body,UTF8);
		}
		return msg;
	}

	private static MimeBodyPart createAttachment() throws MessagingException, IOException {
		Random r = new Random();
		String[] extensions = new String[] {
				".odp",
				".odt",
				".ods",
				".txt",
				".gif",
				".jpg",
				".png"
		}; 
		String[] src = new String[] {
				"http://"+Utils.getHostName()+":8080/bluebox/data/MyDocument.odt",
				"http://"+Utils.getHostName()+":8080/bluebox/data/MyPresentation.odp",
				"http://"+Utils.getHostName()+":8080/bluebox/data/MySpreadsheet.ods",
				"http://"+Utils.getHostName()+":8080/bluebox/data/GettingStarted.txt",
				"http://"+Utils.getHostName()+":8080/bluebox/data/message.png",
				"http://"+Utils.getHostName()+":8080/bluebox/data/bigpic.jpg",
				"http://"+Utils.getHostName()+":8080/bluebox/data/BlueBox.png"
		}; 
		String root = "src/main/resources/data/";
		String[] srcLocal = new String[] {
				root+"MyDocument.odt",
				root+"MyPresentation.odp",
				root+"MySpreadsheet.ods",
				root+"GettingStarted.txt",
				root+"message.png",
				root+"bigpic.jpg",
				root+"BlueBox.png"
		}; 
		int index = r.nextInt(extensions.length); 
		String name = "attachment"+extensions[index];

		MimetypesFileTypeMap ftm = (MimetypesFileTypeMap) FileTypeMap.getDefaultFileTypeMap();
		ftm.addMimeTypes("application/document odt ODT");
		ftm.addMimeTypes("application/presentation odp ODP");
		ftm.addMimeTypes("application/spreadsheet ods ODS");
		ftm.addMimeTypes("text/plain txt TXT");
		ftm.addMimeTypes("image/jpeg jpg JPG");
		ftm.addMimeTypes("image/png png PNG");
		ftm.addMimeTypes("image/gif gif GIF");
		ftm.addMimeTypes("application/zip zip ZIP");
		FileTypeMap.setDefaultFileTypeMap(ftm);

		if (!cachedFiles.containsKey(name)) {
			try {
				URL u = new URL(src[index]);
				URLConnection uc = u.openConnection();
				uc.connect();
				InputStream in = uc.getInputStream();
				File tmpFile = File.createTempFile("blueboxmail", extensions[r.nextInt(extensions.length)]);
				tmpFile.deleteOnExit();
				FileOutputStream out = new FileOutputStream(tmpFile);
				final int BUF_SIZE = 1 << 8;
				byte[] buffer = new byte[BUF_SIZE];
				int bytesRead = -1;
				while((bytesRead = in.read(buffer)) > -1) {
					out.write(buffer, 0, bytesRead);
				}
				in.close();
				out.close();
				cachedFiles.put(name, tmpFile);
			}
			catch (ConnectException ce) {
				// we are running in localtest, so read files off disk
				cachedFiles.put(name, new File(srcLocal[index]));
			}
		}

		MimeBodyPart messageBodyPart = new MimeBodyPart();
		FileDataSource source = new javax.activation.FileDataSource(cachedFiles.get(name));
		source.setFileTypeMap(ftm);
		messageBodyPart.setFileName(name);
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setHeader("Content-Type", source.getContentType()); 
		return messageBodyPart;
	}

	public static String decodeRFC2407(String s) {
		try {
			return javax.mail.internet.MimeUtility.decodeText(s);
		} 
		catch (UnsupportedEncodingException e) {
			//e.printStackTrace();
			log.warning("Problem decoding "+s);
			return s;
		}
	}

	public static String encodeRFC2407(String s) {
		try {
			return javax.mail.internet.MimeUtility.encodeText(s);
		} 
		catch (UnsupportedEncodingException e) {
			//			e.printStackTrace();
			log.warning("Problem encoding "+s);
			return s;
		}
	}

	public static boolean isNotesAddress(String email) {
		return ((email.indexOf('@')<0)&&((email.indexOf('/')>0)||(email.indexOf('%')>0)));
	}

	public static String convertNotesAddress(String notes) {
		if (notes.indexOf('/')>0)
			notes = notes.replaceAll("%", ".");
		else
			notes = notes.replaceAll("%", "/");
		String domain = AbstractHandler.extractFragment(notes, 0);
		String subdomain = AbstractHandler.extractFragment(notes, 1);
		String name = AbstractHandler.extractFragment(notes, 2);

		name = name+" "+"<"+name.replace(' ', '_')+"@"+subdomain+"."+domain+">";
		return name;
	}

	public static String convertEncoding(String text, String encoding) {
		try {
			// set up byte streams
			InputStream in = new ByteArrayInputStream(text.getBytes());
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			// Set up character stream
			Reader r = new java.io.BufferedReader(new java.io.InputStreamReader(in, encoding));
			Writer w = new BufferedWriter(new java.io.OutputStreamWriter(out, "UTF8"));

			// Copy characters from input to output.  The InputStreamReader
			// converts from the input encoding to Unicode,, and the OutputStreamWriter
			// converts from Unicode to the output encoding.  Characters that cannot be
			// represented in the output encoding are output as '?'
			char[] buffer = new char[4096];
			int len;
			while((len = r.read(buffer)) != -1) 
				w.write(buffer, 0, len);
			r.close();
			w.flush();
			return out.toString();
		}
		catch (Throwable t) {
			t.printStackTrace();
			return text;
		}
	}

	//	public static String decodeQuotedPrintable(String quoted) {
	//		try {
	//			return MimeUtility.decodeText(quoted);
	//		} catch (UnsupportedEncodingException e) {
	//			e.printStackTrace();
	//		}
	//		return quoted;
	//	}

	public static String decodeQuotedPrintable(String quoted) {
		StringTokenizer st = new StringTokenizer(quoted,"\r\n",false);
		StringBuffer res = new StringBuffer();
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			try {
				s = MimeUtility.decodeText(s);
				// remove trailing =
				int pos=s.lastIndexOf('=');
				if ((pos>0)&&(pos==s.length()-1)) {
					s = s.substring(0,s.lastIndexOf('='));
				}

				s = new String(QuotedPrintableCodec.decodeQuotedPrintable(s.getBytes(Charset.forName("UTF-8"))),"UTF-8");
				res.append(s);
			} 
			catch (Throwable e) {
				res.append(s);
				//e.printStackTrace();
				log.fine("Error decoding quoted-printable :"+e.getMessage());
			}				
		}
		return res.toString();
	}

	public static byte[] convertStreamToBytes(InputStream binaryStream) throws IOException {
		return convertStreamToString(binaryStream).getBytes();
	}

	public static void copy(InputStream is, OutputStream os) throws IOException {
		int byteRead;
		while((byteRead=is.read())>=0)
			os.write(byteRead);

		os.flush();
	}
	
	public static InputStream streamMimeMessage(MimeMessage msg) throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		msg.writeTo(os);
		return new ByteArrayInputStream(os.toByteArray());
	}


}
