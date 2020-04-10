package com.bluebox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
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
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.fileupload.util.mime.MimeUtility;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.search.SearchFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class Utils {
	public static final String UTF8 = "UTF-8";
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	private static int counter=0;
	private static MimetypesFileTypeMap ftm;

	static {
		ftm = (MimetypesFileTypeMap) FileTypeMap.getDefaultFileTypeMap();
		ftm.addMimeTypes("application/document odt ODT");
		ftm.addMimeTypes("application/presentation odp ODP");
		ftm.addMimeTypes("application/spreadsheet ods ODS");
		ftm.addMimeTypes("text/plain txt TXT");
		ftm.addMimeTypes("image/jpeg jpg JPG");
		ftm.addMimeTypes("image/png png PNG");
		ftm.addMimeTypes("image/gif gif GIF");
		ftm.addMimeTypes("application/zip zip ZIP");
		FileTypeMap.setDefaultFileTypeMap(ftm);
	}


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
			log.error("Could not find hostname",e);
			return "localhost";
		}
	}

	public static String convertStreamToString(InputStream is) throws IOException {
		String result = IOUtils.toString(is,UTF8);
		is.close();
		return result;
	}

	public static InputStream convertStringToStream(String s) {
		try {
			return IOUtils.toInputStream(s,UTF8);
		} 
		catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
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

	public static String uploadEML(Inbox inbox, InputStream eml) throws IOException, MessagingException {
		if (eml==null) {
			log.error("Could not load eml resource");
			return "Could not load eml resource";
		}
		try {
			MimeMessage message = loadEML(eml);
			sendMessageDirect(inbox,message);
			eml.close();
			return "Loaded email ok";
		}  
		catch (Throwable e) {
			e.printStackTrace();
			try {
				eml.close();
			}
			catch (IOException ioe) {
				// don't care
			}
			return e.toString()+":"+e.getMessage();
		}
	}

	public static MimeMessage loadEML(InputStream inputStream) throws MessagingException, IOException {
		MimeMessage message = new MimeMessage(null,inputStream);
		inputStream.close();
		return message;
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

	public static WorkerThread generate(final ServletContext session, final Inbox inbox, final int count) throws Exception {
		WorkerThread wt = new WorkerThread(Inbox.GENERATE_WORKER) {

			@Override
			public void run() {
				setStatus("Running");
				log.debug("Starting generation thread");
				Date now = new Date();
				try {
					int toC,ccC, bccC;
					int totalCount = 0;
					Random r = new Random();
					if(count>0) {
						do {
							if (isStopped()) {
								log.info("Someone stopped our thread!");
								break;
							}
							toC = r.nextInt(5);
							ccC = r.nextInt(5);
							bccC = r.nextInt(2);
							MimeMessage msg = createMessage(session,
									getRandomAddress(),  
									getRandomAddresses(toC),//to
									getRandomAddresses(ccC),//cc
									getRandomAddresses(bccC),//bcc
									(counter++)+" "+randomLine(35), 
									randomText(14),
									randomText(14),
									true);

							sendMessageDirect(inbox,msg);
							totalCount += toC+ccC+bccC;
							setProgress((totalCount*100)/count);
							if ((totalCount % 100)==0) {
								int elapsedSeconds = (int)(new Date().getTime()-now.getTime())/1000;
								log.info("{}% complete at {} mails/second",getProgress(),(totalCount/elapsedSeconds));
							}
						} while (totalCount<count);
					}
					setStatus("Generated "+count+" messages");
				}
				catch (Throwable t) {
					log.error("Problem generating mail",t);
					setStatus("Error :"+t.getMessage());
				}
				finally {
					setProgress(100);
					log.info("Ended generation thread");
				}
			}

		};
		return wt;
	}

	public static void generateNoThread(final ServletContext session, final Inbox inbox, final int count) {
		try {
			int toC,ccC, bccC;
			int totalCount = 0;
			Random r = new Random();
			do {
				toC = r.nextInt(count-totalCount+1);
				ccC = r.nextInt(count-toC-totalCount+1);
				bccC = r.nextInt(count-ccC-toC-totalCount+1);
				MimeMessage msg = createMessage(session,
						getRandomAddress(),  
						getRandomAddresses(toC),//to
						getRandomAddresses(ccC),//cc
						getRandomAddresses(bccC),//bcc
						(counter++)+" "+randomLine(35), 
						randomText(14),
						randomText(14),
						true);

				sendMessageDirect(inbox,msg);
				totalCount += toC+ccC+bccC;
			} while (totalCount<count);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static void sendMessage(final ServletContext session, final Inbox inbox, final int count) {
		ExecutorService threadPool = Executors.newFixedThreadPool(10);
		for (int j = 0; j < count/6; j++) {
			threadPool.execute(new Runnable() {

				@Override
				public void run() {
					boolean sent = false;
					int retryCount = 5;
					while ((!sent)&&(retryCount-->0)) {
						try {
							MimeMessage msg = createMessage(session,
									getRandomAddress(),  
									getRandomAddresses(2),//to
									getRandomAddresses(2),//cc
									getRandomAddresses(2),//bcc
									(counter++)+" "+randomLine(35), 
									randomText(14),
									randomText(14),
									true);

							sendMessageDirect(inbox,msg);
							sent = true;

						} 
						catch (Throwable e) {
							e.printStackTrace();
							// server overloaded, try again later
							try {
								log.debug("Waiting to deliver",e);
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



	public static String spoolStream(StorageIf si, MimeMessage message) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		message.writeTo(baos);
		String spooledUid = si.spoolStream(new ByteArrayInputStream(baos.toByteArray()));
		baos.close();
		return spooledUid;
	}

	private static List<String> getRecipients(MimeMessage msg) throws MessagingException {
		List<String> recipients = new ArrayList<String>();
		Address[] to = msg.getRecipients(RecipientType.TO);
		Address[] cc = msg.getRecipients(RecipientType.CC);
		Address[] bcc = msg.getRecipients(RecipientType.BCC);
		if (to!=null)
			for (int i = 0; i < to.length;i++)
				recipients.add(to[i].toString());
		if (cc!=null)
			for (int i = 0; i < cc.length;i++)
				recipients.add(cc[i].toString());
		if (bcc!=null)
			for (int i = 0; i < bcc.length;i++)
				recipients.add(bcc[i].toString());

		// if we load emails from file, there might not be a recipient (bcc)
		if (recipients.size()==0)
			recipients.add("anonymous@localhost");
		return recipients;
	}

	private static String getFrom(MimeMessage msg) throws MessagingException {
		if (msg.getFrom().length>0)
			return msg.getFrom()[0].toString();
		return "nullsender@localhost";
	}

	private static void sendMessageDirect(Inbox inbox, MimeMessage msg, String spooledUid) throws Exception {
		List<String> recipients = getRecipients(msg);
		for (String recipient : recipients) {
			if (inbox.accept(getFrom(msg), recipient)) {
				inbox.deliver(getFrom(msg), recipient, msg, spooledUid);
			}
		}
	}

	private static void sendMessageDirect(Inbox inbox,MimeMessage msg) throws Exception {
		StorageIf si = StorageFactory.getInstance();
		String spooledUid = spoolStream(si,msg);
		sendMessageDirect(inbox,msg,spooledUid);
		//		si.removeSpooledStream(spooledUid);
	}

	public static void sendMessageDirect(StorageIf storage, MimeMessage msg) throws Exception {
		List<String> recipients = getRecipients(msg);
		String spooledUid = Utils.spoolStream(storage,msg);
		for (String recipient : recipients) {
			log.debug("Sending message to {}",recipient);
			BlueboxMessage bbm = storage.store(getFrom(msg), new InboxAddress(recipient), new Date(), msg, spooledUid);
			SearchFactory.getInstance().indexMail(bbm,false);
		}
		SearchFactory.getInstance().commit(true);
		//		storage.removeSpooledStream(spooledUid);
	}

	public static MimeMessage createMessage(ServletContext session, String from, String to, String cc, String bcc, String subject, String body) throws MessagingException, IOException {
		return Utils.createMessage(session, 
				from,
				to,
				cc,
				bcc,
				subject,
				body,
				"");
	}

	public static MimeMessage createMessage(ServletContext session, String from, String to, String cc, String bcc, String subject, String body, String htmlBody) throws MessagingException, IOException {
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
				htmlBody,
				false);
	}

	public static MimeMessage createMessage(ServletContext session, InternetAddress from, InternetAddress[] to, InternetAddress[] cc, InternetAddress[] bcc, String subject, String textBody, String htmlBody, boolean attachment) 
			throws MessagingException, IOException {
		Session s = null;

		MimeMessage msg = new MimeMessage(s);
		msg.setFrom(from);
		msg.setSubject(subject,UTF8);
		msg.setSentDate(StorageFactory.getInstance().getUTCTime());
		if (to.length>0)
			msg.addRecipients(Message.RecipientType.TO, to);
		if (cc.length>0)
			msg.setRecipients(Message.RecipientType.CC, cc);
		if (bcc.length>0)
			msg.setRecipients(Message.RecipientType.BCC, bcc);
		if ((htmlBody.length()>0)||(attachment)) {
			// set the body
			Multipart multipart = new MimeMultipart();

			// create the html message part 
			if (htmlBody.length()>0) {
				// create the text message part 
				MimeBodyPart textBodyPart = new MimeBodyPart();
				textBodyPart.setContent(textBody,"text/plain; charset=\""+UTF8+"\"");
				multipart.addBodyPart(textBodyPart);

				// create the html part
				MimeBodyPart htmlBodyPart = new MimeBodyPart();
				htmlBodyPart.setContent("<html><head><!--<script>alert('gotcha!');</script>--></head><body><font color=\"red\">"+textBody.replaceAll("\n", "</br>")+"</font></body></html>","text/html; charset=\""+UTF8+"\"");
				htmlBodyPart.setContent(htmlBody,"text/html; charset=\""+UTF8+"\"");
				multipart.addBodyPart(htmlBodyPart);
			}
			if (attachment) {
				// randomly create up to 5 attachment
				int attachmentCount = new Random().nextInt(25);
				for (int i = 0; i < attachmentCount; i++)
					multipart.addBodyPart(createAttachment(session));
			}

			// Put parts in message
			msg.setContent(multipart);
		}
		else {
			msg.setText(textBody,UTF8);
		}


		//		else {
		//			msg.setText(textBody,UTF8);
		//		}
		return msg;
	}

	private static MimeBodyPart createAttachment(ServletContext session) throws MessagingException, IOException {
		Random r = new Random();
		String[] names = new String[] {
				"MyDocument.odt",
				"MyPresentation.odp",
				"MySpreadsheet.ods",
				"GettingStarted.txt",
				"message.png",
				"bigpic.jpg",
				"BlueBox.png",
				"cv-template-Marketing-Manager.doc"
		};

		int index = r.nextInt(names.length); 
		String name = Integer.toString(r.nextInt(99))+"-"+names[index];



		MimeBodyPart messageBodyPart = new MimeBodyPart();
		InputStream content;
		if (session!=null) {
			content = session.getResourceAsStream("data/"+names[index]);
		}
		else {
			content = new FileInputStream("src/main/webapp/data/"+names[index]);
		}
		DataSource source = new ByteArrayDataSource(content,ftm.getContentType(names[index]));
		messageBodyPart.setFileName(name);
		messageBodyPart.setContentID(UUID.randomUUID().toString());
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setHeader("Content-Type", ftm.getContentType(name)); 

		return messageBodyPart;
	}

	public static String decodeRFC2407(String s) {
		try {
			return javax.mail.internet.MimeUtility.decodeText(s);
		} 
		catch (UnsupportedEncodingException e) {
			//e.printStackTrace();
			log.warn("Problem decoding "+s);
			return s;
		}
	}

	public static String encodeRFC2407(String s) {
		try {
			return javax.mail.internet.MimeUtility.encodeText(s);
		} 
		catch (UnsupportedEncodingException e) {
			//			e.printStackTrace();
			log.warn("Problem encoding "+s);
			return s;
		}
	}

	/*
	 * A Notes address can be detected by the lack of '@' character, as well as the presence of forward-slashes.
	 */
	public static boolean isNotesAddress(String email) {
		return ((email.indexOf('@')<0)&&(email.indexOf('/')>0));
	}

	public static String convertNotesAddress(String notes) {
		//		if (notes.indexOf('/')>0)
		//			notes = notes.replaceAll("%", ".");
		//		else
		//			notes = notes.replaceAll("%", "/");
		String domain = extractFragment(notes, 2);
		String subdomain = extractFragment(notes, 1);
		String name = extractFragment(notes, 0);

		name = name+" "+"<"+name.replace(' ', '_')+"@"+subdomain+"."+domain+">";
		return name;
	}

	/*
	 * This will extract the fragment following an expected root prefix
	 */
	public static String extractFragment(String uri, String root, int index) {
		return extractFragment(uri.substring(uri.indexOf(root)+root.length()),index);
	}

	/*
	 * This will extract the uri fragment at position index, where the last fragment is 0
	 * So for /aaa/bbb/ccc/ddd
	 * extractFragment(0) = aaa
	 * extractFragment(1) = bbb
	 * extractFragment(2) = ccc
	 * extractFragment(3) = ddd etc
	 * 
	 * 	for /aaa/bbb/ccc/ddd/
	 * extractFragment(0) = aaa
	 * extractFragment(3) = ddd
	 * extractFragment(3) = ""
	 */
	public static String extractFragment(String uri, int index) {
		try {
			StringTokenizer tok = new StringTokenizer(uri,"/",true);
			List<String> list = new ArrayList<String>();
			String token="", prevToken;
			while (tok.hasMoreTokens()) {
				prevToken = token;
				token = tok.nextToken();
				if ("/".equals(token)) {
					// check for form xxx//zzz
					if ("/".equals(prevToken))
						list.add("");
				}
				else {
					try {
						list.add(URLDecoder.decode(token,Utils.UTF8));
					} catch (UnsupportedEncodingException e) {
						list.add(token);
						e.printStackTrace();
					}
				}
			}
			return list.get(index);
		}
		catch (Throwable ex) {
			// this happens for the form /aa/bb/cc/ when you get index 3
			return "";
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
		return decodeQuotedPrintable(quoted,UTF8);
	}

	public static String decodeQuotedPrintable(String quoted, String encoding) {
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

				s = new String(QuotedPrintableCodec.decodeQuotedPrintable(s.getBytes(Charset.forName(encoding))),encoding);
				res.append(s);
			} 
			catch (Throwable e) {
				res.append(s);
				//e.printStackTrace();
				log.error("Error decoding quoted-printable",e);
			}				
		}
		return res.toString();
	}

	public static String encodeQuotedPrintable(String original) throws DecoderException {
		return new String(QuotedPrintableCodec.decodeQuotedPrintable(original.getBytes()));
	}

	//	public static InputStream streamMimeMessage(MimeMessage msg) throws IOException, MessagingException {
	//		// spool to disk to prevent out of memory errors
	//		File f = getTempFile();
	//		FileOutputStream fos = new FileOutputStream(f);
	//		msg.writeTo(fos);
	//		fos.close();
	//		return new FileInputStream(f);
	//	}

	//	public static File getTempFile() throws IOException {
	//		File f = File.createTempFile("bluebox", ".spool");
	//		f.deleteOnExit();	
	//		if (tempFiles.size()>100) {
	//			File old = tempFiles.remove();
	//			log.debug("Removing {} total count = {}",old.lastModified(),tempFiles.size());
	//			if (!old.delete()) {
	//				// not serious, was probably deleted by proper cleanup
	//				log.debug("Could not delete temporary file :{}",old.getCanonicalPath());
	//			}
	//		}
	//		tempFiles.add(f);
	//		return f;
	//	}

	public static String getServletBase(HttpServletRequest request) {
		if (request==null) {
			return "";
		}
		String uri = request.getScheme() + "://" +
				request.getServerName() + 
				("http".equals(request.getScheme()) && request.getServerPort() == 80 || "https".equals(request.getScheme()) && request.getServerPort() == 443 ? "" : ":" + request.getServerPort() ) +
				request.getContextPath()+"/jaxrs";
		//		log.info("1:{}",request.getPathTranslated());
		//		log.info("2:{}",request.getServletPath());
		//		log.info("3:{}",request.getRequestURL());
		//		log.info("4:{}",request.getPathInfo());
		//		log.info("5:{}",request.getPathTranslated());
		//		log.info("6:{}",request.getRequestURI());
		return uri;
	}

	private static int[] getVersionNumbers(String ver) {
		Matcher m = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(beta(\\d*))?").matcher(ver);
		if (!m.matches())
			throw new IllegalArgumentException("Malformed FW version :"+ver);

		return new int[] { Integer.parseInt(m.group(1)),  // major
				Integer.parseInt(m.group(2)),             // minor
				Integer.parseInt(m.group(3)),             // rev.
				m.group(4) == null ? Integer.MAX_VALUE    // no beta suffix
						: m.group(5).isEmpty() ? 1        // "beta"
								: Integer.parseInt(m.group(5))    // "beta3"
		};
	}

	static boolean isVersionNewer(String availableVersion, String currentVersion) {
		try {
			int[] availableVer = getVersionNumbers(availableVersion);
			int[] currentVer = getVersionNumbers(currentVersion);

			for (int i = 0; i < availableVer.length; i++)
				if (availableVer[i] != currentVer[i])
					return availableVer[i] > currentVer[i];

					return false;
		}
		catch (Throwable t) {
			return false;
		}
	}

	private static Date lastChecked = new Date(0);
	private static Properties props;

	public static Properties getOnlinePropsCached() {
		String propsUrl = "http://raw.githubusercontent.com/stephen-kruger/bluebox/master/src/main/resources/bluebox.properties";
		// cache this value for 24 hours
		if ((new Date().getTime()-lastChecked.getTime())>86400000) {
			try {
				log.info("Checking for online update");
				props = new Properties();
				InputStream is = new URL(propsUrl).openStream();	
				props.load(is);
				is.close();
				lastChecked = new Date();
			}
			catch (Throwable t) {
				//t.printStackTrace();
				log.error("Problem connecting to {}",propsUrl);
				props.put(Config.BLUEBOX_VERSION, Config.getInstance().getString(Config.BLUEBOX_VERSION));
				props.put("online_war", Config.getInstance().getString("online_war"));
			}
		}
		else {
			log.debug("Using cached value for online update, expiring in {}ms",(new Date().getTime()-lastChecked.getTime()));
		}
		return props;
	}

	public static JSONObject updateAvailable() throws JSONException {
		JSONObject jo = new JSONObject();
		Properties props = getOnlinePropsCached();
		jo.put("update_available", isVersionNewer(props.getProperty(Config.BLUEBOX_VERSION),Config.getInstance().getString(Config.BLUEBOX_VERSION)));
		jo.put("current_version", Config.getInstance().getString(Config.BLUEBOX_VERSION));
		jo.put("available_version", props.getProperty(Config.BLUEBOX_VERSION));
		return jo;
	}

	public static String toString(List<String> list) {
		StringBuffer listS = new StringBuffer();
		for (String s : list) {
			listS.append("<div>").append(s).append("</div>");
		}
		return listS.toString();
	}

	public static String toCSVString(List<String> list) {
		StringBuffer listS = new StringBuffer();
		for (String s : list) {
			listS.append(s).append(',');
		}
		if (listS.length()>0)
			return listS.substring(0, listS.length()-1).toString();
		return listS.toString();
	}

	public static String toCSVString(JSONArray list) {
		return list.toString();
	}

	public static long getSize(MimeMessage bbmm) {
		long count = 0;
		try {
			count = bbmm.getSize();
			if (count<0) {
				log.debug("Manually calculating message size");
				count=0;
				InputStream is = bbmm.getInputStream();
				while (is.read()>=0)
					count++;
				is.close();
			}
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
		return count;
	}

	public static Calendar getUTCCalendar() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	}

	public static int getUTCHour() {
		return getUTCCalendar().get(Calendar.HOUR_OF_DAY);
	}

	public static Date getUTCDate(Date utct, long time) {
		//		Date utct =  getUTCTime();
		utct.setTime(time);
		return utct;
	}

	/*
	 * clean compile assembly:single will generate an executable jar with this entry point
	 */
	public static final void main(String[] args) {
		if (args.length<2) {
			log.info("usage : java -jar bluebox-utils.jar age 3    - delete mail older than three days");
			log.info("        java -jar bluebox-utils.jar max 160  - trim size to 160K");
		}
		if ("age".equals(args[0])) {
			StorageIf si = StorageFactory.getInstance();
			try {
				long three_days = Integer.valueOf(args[1])*24*60*60*1000;
				Date when = new Date(new Date().getTime()-three_days);
				long count = si.getMailCount(State.ANY);
				log.info("Deleting mail older than {}. Total count is {}",when,count);
				int pageSize = 500;
				long completion = 0;
				for (int i = 0; i < count; i+= pageSize) {
					try {
						log.info("Processing {}% {} of {}",completion*100/count,completion,count);
						List<LiteMessage> mail = si.listMailLite(null, State.ANY, i, pageSize, BlueboxMessage.RECEIVED, false);
						List<LiteMessage> bulkList = new ArrayList<LiteMessage>();
						for (LiteMessage message : mail) {
							if (message.getReceived().before(when)) {
								//si.delete(message.getIdentifier(), message.getRawIdentifier());
								bulkList.add(message);
							}
						}
						// remove the page in bulk
						si.delete(bulkList);
						completion+=mail.size();
					} 
					catch (Exception e) {
						log.error("Problem listing mail {}",e.getMessage());
						e.printStackTrace();
					}
				}
				log.info("Finished cleanup total count is now {}",si.getMailCount(State.ANY));
			} 
			catch (Exception e) {
				log.error("Problem listing mail {}",e.getMessage());
				e.printStackTrace();
			}
			return;
		}
		if ("max".equals(args[0])) {
			StorageIf si = StorageFactory.getInstance();
			try {
				long count = Integer.valueOf(args[1])*1000;
				log.info("Trimming mail down to {}",count);
				int pageSize = 500;
				long completion = 0;
				do {
					try {
						log.info("Processing {}% {} of {}",completion*100/count,completion,count);
						List<LiteMessage> mail = si.listMailLite(null, State.ANY, 0, pageSize, BlueboxMessage.RECEIVED, false);
						List<LiteMessage> bulkList = new ArrayList<LiteMessage>();
						for (LiteMessage message : mail) {
							//si.delete(message.getIdentifier(), message.getRawIdentifier());
							bulkList.add(message);
						}
						// remove the page in bulk
						si.delete(bulkList);
						completion+=mail.size();
					} 
					catch (Exception e) {
						log.error("Problem listing mail {}",e.getMessage());
						e.printStackTrace();
					}
				} while (si.getMailCount(State.ANY)>count);
				log.info("Finished cleanup total count is now {}",si.getMailCount(State.ANY));
			} 
			catch (Exception e) {
				log.error("Problem listing mail {}",e.getMessage());
				e.printStackTrace();
			}
			return;
		}
		for (int i = 0; i < args.length; i++)
			log.error("Unknown argument {}",args[i]);
	}
}
