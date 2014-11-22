package com.bluebox.smtp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexNotFoundException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchIndexer;
import com.bluebox.search.SearchIndexer.SearchFields;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.LiteMessageIterator;
import com.bluebox.smtp.storage.MessageIterator;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class Inbox implements SimpleMessageListener {
	private static final String GLOBAL_COUNT_NODE = "global_message_count";
	Preferences prefs = Preferences.userNodeForPackage(Inbox.class);
	private JSONObject recentStats = new JSONObject();

	public static final String EMAIL = "Email";
	public static final String START = "Start";
	public static final String COUNT = "Count";
	public static final String ORDERBY = "OrderBy";
	private static final Logger log = LoggerFactory.getLogger(Inbox.class);
	private List<String> fromBlackList, toBlackList, toWhiteList, fromWhiteList;

	private static Timer timer = null;
	private static TimerTask timerTask = null;

	private static Inbox inbox;

	public static Inbox getInstance() {
		if (inbox == null) {
			inbox = new Inbox();
			inbox.start();
		}
		return inbox;
	}

	private Inbox() {
		StorageFactory.getInstance();
		loadConfig();
	}

	private void start() {
		log.debug("Starting inbox");

		// ensure storage instance if loaded and started
		try {
			StorageFactory.getInstance().start();
		} 
		catch (Exception e) {
			log.error("Error starting storage instance",e.getMessage());
			e.printStackTrace();
		}
		// now start a background timer for the mail expiration
		// only one per jvm instance
		if (timer == null) {
			log.debug("Scheduling cleanup timer");
			long frequency = Config.getInstance().getLong(Config.BLUEBOX_DAEMON_DELAY);
			timer = new Timer();
			long period = frequency*60*1000;  // repeat every hour.
			long delay = period;   // delay for same amount of time before first run.
			timer = new Timer();
			timer.scheduleAtFixedRate(timerTask = new TimerTask() {

				public void run() {
					log.info("Cleanup timer activated");
					try {
						new Thread(cleanUp()).start();
					} 
					catch (Exception e) {
						log.error("Error running message cleanup",e);
						e.printStackTrace();
					}
				}
			}, delay, period);
		}
		log.info("Started inbox");

	}

	public void stop() {
		log.debug("Stopping cleanup timer");
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		log.debug("Stopping inbox");
		try {
			StorageFactory.getInstance().stop();
		}
		catch (Throwable e) {
			log.error("Error stopping storage :{}",e.getMessage());
		}

		log.debug("Stopping search engine");
		try {
			SearchIndexer.getInstance().stop();
		} 
		catch (IOException e) {
			e.printStackTrace();
			log.error("Error stopping search engine",e);
		}
		inbox = null;
		log.info("Stopped inbox");
	}

	public BlueboxMessage retrieve(String uid) throws Exception {
		return StorageFactory.getInstance().retrieve(uid);
	}

	/**
	 * Get the number of messages received.
	 * @return size of received email list
	 */
	public long getMailCount(BlueboxMessage.State state) {
		try {
			return StorageFactory.getInstance().getMailCount(state);
		} 
		catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		return StorageFactory.getInstance().getMailCount(inbox,state);
	}

	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		log.debug("Sending inbox contents for {}",inbox);
		StorageFactory.getInstance().listInbox(inbox, state, writer, start, count, orderBy, ascending, locale);
	}

	public List<BlueboxMessage> listInbox(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		log.debug("Sending inbox contents for {}",inbox);
		return StorageFactory.getInstance().listMail(inbox, state, start, count, orderBy, ascending);
	}

	public List<LiteMessage> listInboxLite(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending, Locale loc) throws Exception {
		log.debug("Sending inbox contents for {}",inbox);
		return StorageFactory.getInstance().listMailLite(inbox, state, start, count, orderBy, ascending);
	}

	public long searchInbox(String search, Writer writer, int start, int count, SearchIndexer.SearchFields searchScope, SearchIndexer.SearchFields orderBy, boolean ascending) throws Exception {
		log.debug("Searching for {} ordered by {}",search,orderBy);
		try {
			return SearchIndexer.getInstance().searchInboxes(search, writer, start, count, searchScope, orderBy, ascending);
		}
		catch (IndexNotFoundException inf) {
			log.info("Detected index problems ({})",inf.getMessage());
			return 0;
		}
	}

	public void delete(String uid) throws Exception {
		StorageFactory.getInstance().delete(uid);
		SearchIndexer.getInstance().deleteDoc(uid);
	}

	/*
	 * Delete the message, and all other mails with same sender address. Add sender to blacklist.
	 */
	public WorkerThread toggleSpam(final List<String> uids) throws Exception {
		WorkerThread wt = new WorkerThread("spam") {

			@Override
			public void run() {
				for (String uid : uids) {

					try {
						final BlueboxMessage spam = retrieve(uid);
						boolean spamAction = true;
						if (spam.getState()==BlueboxMessage.State.NORMAL) {
							softDelete(uid);
						}
						else {
							softUndelete(uid,spam);
							spamAction = false;
						}		
						// now process all mails looking for same smtp source
						String smtpDomain = spam.getSMTPSender();
						if (smtpDomain.trim().length()>0) {
							BlueboxMessage msg;
							MessageIterator mi = new MessageIterator();
							while (mi.hasNext()) {
								msg = mi.next();
								setProgress(mi.getProgress());
								try {
									if (smtpDomain.indexOf(msg.getSMTPSender())>=0) {
										if (spamAction) {
											log.info("Spam detected - soft deleting "+msg.getIdentifier());
											softDelete(msg.getIdentifier());
										}
										else {
											log.info("Spam detected - soft undeleting");
											softUndelete(msg.getIdentifier(),msg);
										}
									}
								}
								catch (Throwable t) {
									log.warn(t.getMessage());
								}
							}		

						}
						else {
							log.info("No smtp sender found, cannot blacklist or un-blacklist");
						}
					}
					catch (Throwable t) {
						log.error("Error toggling spam",t);
					}
					finally {
						setProgress(100);
					}
				}
			}
		};
		return wt;
	}

	public void deleteAll() {
		try {
			StorageFactory.getInstance().deleteAll();
			SearchIndexer.getInstance().deleteIndexes();
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public WorkerThread cleanUp() throws Exception {
		WorkerThread wt = new WorkerThread("cleanup") {

			@Override
			public void run() {
				try {
					setProgress(30);
					// remove old messages
					expire();
					setProgress(60);
					// trim total mailbox size
					trim();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				finally {
					setProgress(100);
					setStatus("Complete");
				}
			}

		};

		return wt;
	}

	private void trim() {
		List<LiteMessage> list;
		try {
			long max = Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_MAX);
			log.info("Trimming mailboxes to limit of {} entries",max);
			long count;
			while ((count=StorageFactory.getInstance().getMailCount(BlueboxMessage.State.NORMAL))>max) {
				list = StorageFactory.getInstance().listMailLite(null, BlueboxMessage.State.NORMAL, 0, 1000, BlueboxMessage.RECEIVED, true);
				count = count-max; // how many to delete
				for (LiteMessage msg : list) {
					if (count-->0)
						delete(msg.getIdentifier());
					else
						break;
				}
				log.info("Trimmed {} messages",list.size());
			}
		}
		catch (Throwable t) {
			log.error("Problem trimming mailboxes",t);
		}
	}

	public void expire() throws Exception {
		Date messageDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_AGE)*60*60*1000));
		Date trashDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_TRASH_AGE)*60*60*1000));
		expireOld(messageDate);
		expireDeleted(trashDate);
	}

	/*
	 * Remove all deleted mails, regardless of when they were received.
	 */
	public void purge() throws Exception {
		log.info("Cleaning deleted messages");
		int count  = 0;
		int start = 0;
		List<BlueboxMessage> list;
		do {
			list = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.DELETED, start, 500, BlueboxMessage.RECEIVED, true);
			for (BlueboxMessage msg : list) {
				try {
					delete(msg.getIdentifier());
					count++;			
				}
				catch (Throwable t) {
					log.warn("Problem cleaning up message {}",msg.getIdentifier());
				}
			}
			start+=500;
		} while (list.size()>0);
		log.info("Removed {} deleted messages",count);
	}

	private void expireOld(Date messageExpireDate) throws Exception {
		log.info("Cleaning messages received before {}",messageExpireDate);
		LiteMessage msg;
		int count = 0;
		LiteMessageIterator mi = new LiteMessageIterator(null, BlueboxMessage.State.NORMAL);
		while (mi.hasNext()) {
			msg = mi.next();
			try {
				if ((msg.getReceived()).before(messageExpireDate)) {
					delete(msg.getIdentifier());
					count++;
				}
			}
			catch (Throwable t) {
				log.warn("Problem cleaning up message {} {}",msg.getIdentifier(),t.getMessage());
			}
		}
		log.info("Cleaned up {} messages",count);
	}

	private void expireDeleted(Date trashExpireDate) throws Exception {
		log.info("Cleaning deleted messages received before {}",trashExpireDate);
		LiteMessage msg;
		int count = 0;
		LiteMessageIterator mi = new LiteMessageIterator(null, BlueboxMessage.State.DELETED);
		while (mi.hasNext()) {
			msg = mi.next();
			try {
				if ((msg.getReceived()).before(trashExpireDate)) {
					delete(msg.getIdentifier());
					count++;
				}				
			}
			catch (Throwable t) {
				log.warn("Problem cleaning up message {}",msg.getIdentifier());
			}
		}
		log.info("Cleaned up {} deleted messages",count);
	}

	@Override
	public boolean accept(String from, String recipient) {	
		try {
			InboxAddress fromAddress = new InboxAddress(from);
			InboxAddress recipientAddress = new InboxAddress(recipient);
			if (!fromAddress.isValidAddress()) {
				throw new Exception("Invalid from address specified :"+from);
			}
			if (!recipientAddress.isValidAddress()) {
				throw new Exception("Invalid recipient address specified :{}"+recipient);
			}

			// check from blacklist
			for (Object badDomain : getFromBlacklist()) {
				log.debug("{}<<<---- Comparing fromBlackList---->>>{}",badDomain,from);
				if (fromAddress.getDomain().endsWith(badDomain.toString())) {
					log.warn("Rejecting mail from "+from+" to "+recipient+" due to blacklisted FROM:"+badDomain);
					return false;
				}
			}
			// check to blacklist
			for (Object badDomain : getToBlacklist()) {
				log.debug("{}<<<---- Comparing toBlackList---->>>{}",badDomain,recipient);
				if (recipientAddress.getDomain().endsWith(badDomain.toString())) {
					log.warn("Rejecting mail from {} to {} due to blacklisted TO:",from,recipient,badDomain);
					return false;
				}
			}

			// check the from whitelist
			if (fromWhiteList.size()>0) {
				for (Object goodDomain : fromWhiteList) {
					log.debug(goodDomain.toString()+"<<<---- Comparing fromWhiteList---->>>{}",from);
					if (fromAddress.getDomain().endsWith(goodDomain.toString())) {
						return true;
					}
				}
				log.warn("Rejecting mail from {} to {} because not in FROM whitelist",from,recipient);
				return false;
			}

			// check the to whitelist
			if (toWhiteList.size()>0) {
				for (Object goodDomain : toWhiteList) {
					log.debug("{}<<<---- Comparing toWhiteList---->>>{}",goodDomain,recipient);
					if (recipientAddress.getDomain().endsWith(goodDomain.toString())) {
						return true;
					}
				}
				log.warn("Rejecting mail from {} to {} because not in TO whitelist",from,recipient);
				return false;
			}


			// else we accept everyone
			log.debug("Accepting mail for {} from {}",recipient,from);
			return true;
		}
		catch (Throwable t) {
			log.error("{} for from={} and recipient={}",t.getMessage(),from,recipient);
			errorLog("Accept error for address "+recipient+" sent by "+from, ExceptionUtils.getStackTrace(t));
			//			t.printStackTrace();
			return false;
		}
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
		recipient = javax.mail.internet.MimeUtility.decodeText(recipient);
		from = javax.mail.internet.MimeUtility.decodeText(from);
		// when reading from .eml files, we might get multiple recipients, not sure why they are delivered like this.
		// when called via SubEtha, they are normally single recipient addresses
		if (recipient.indexOf(',')>=0) {
			StringTokenizer tok = new StringTokenizer(recipient,",");
			List<String> recipients = new ArrayList<String>();
			String r;
			while (tok.hasMoreTokens()) {
				r = tok.nextToken();
				// ensure we remove duplicates
				if ((r.length()>1)&&(!recipients.contains(r))) {
					recipients.add(r);
				}		
				else {
					log.info("Skipping duplicate recipient");
				}
			}
			for (String nrec : recipients) {
				data.mark(Integer.MAX_VALUE);
				deliver(from, nrec, data);
				data.reset();
			}
		}
		else {
			try {
				deliver(from,recipient,Utils.loadEML(data));
			} 
			catch (Throwable e) {
				log.error(e.getMessage());
				errorLog("("+e.getMessage()+") Accepting raw message for recipient="+recipient +" "+e.getMessage(), data);
				e.printStackTrace();
			}
		}
		data.close();
	}

	public void deliver(String from, String recipient, MimeMessage mmessage) throws Exception {
		//		// if this is a spam blacklisted "from", then abort
		//		if (isReceivedBlackListedDomain(mmessage)) {
		//			return;
		//		}
		from = getFullAddress(from, mmessage.getFrom());
		recipient = getFullAddress(recipient, mmessage.getAllRecipients());
		log.info("Delivering mail for {} from {}",recipient,from);
		BlueboxMessage message = StorageFactory.getInstance().store( 
				from,
				new InboxAddress(recipient),
				new Date(),
				mmessage);
		// ensure the content is indexed
		try {
			SearchIndexer.getInstance().indexMail(message);
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		updateStats(message, recipient, false);
	}

	/*
	 * When mail is delivered, the personal name is not included, so parse the mail to fill it back in
	 */
	private String getFullAddress(String from,Address[] addresses) {
		try {
			for (int i = 0; i < addresses.length;i++) {
				if (((InternetAddress)addresses[i]).getAddress().equals(from)) {
					return addresses[i].toString();
				}
			}
			return from;
		}
		catch (Throwable t) {
			return from;
		}
	}

	public void updateStats(BlueboxMessage message, String recipient, boolean force) throws AddressException, JSONException {
		incrementGlobalCount();
		if (message!=null)
			updateStatsRecent(message.getInbox().getAddress(),message.getFrom().getString(0),message.getSubject(),message.getIdentifier());	
	}

	public void clearErrors() throws Exception {
		StorageFactory.getInstance().logErrorClear();
	}

	public void errorLog(String title, InputStream is) {
		StorageFactory.getInstance().logError(title, is);
	}

	public void errorLog(String title, String detail) {
		StorageFactory.getInstance().logError(title, detail);
	}

	public String errorDetail(String id) {
		return StorageFactory.getInstance().logErrorContent(id);
	}

	public int errorCount() throws Exception {
		return StorageFactory.getInstance().logErrorCount();
	}

	public JSONArray errorCount(int start, int count) throws Exception {
		return StorageFactory.getInstance().logErrorList(start, count);
	}


	public void softDelete(String uid) throws Exception {
		setState(uid, BlueboxMessage.State.DELETED);
		SearchIndexer.getInstance().deleteDoc(uid);
	}

	public void softUndelete(String uid, BlueboxMessage message) throws Exception {
		setState(uid, BlueboxMessage.State.NORMAL);
		SearchIndexer.getInstance().indexMail(message);
	}

	private void setState(String uid, BlueboxMessage.State state) throws Exception {
		StorageFactory.getInstance().setState(uid, state);
	}

	public JSONArray autoComplete(String hint, long start, long count) throws Exception {

		JSONObject curr;
		JSONArray children = new JSONArray();
		// no need to include wildcard
		//		if (hint.contains("*")) {
		//			hint=hint.substring(0,hint.indexOf('*'));
		//		}
		if (hint.length()==0)
			hint = "*";
		// ensure we check for all substrings
		if (!hint.startsWith("*"))
			hint = "*"+hint;
		if (hint.length()==2) {
			return children;
		}
		//			hint = QueryParser.escape(hint);
		SearchIndexer search = SearchIndexer.getInstance();
		Document[] results = search.search(hint, SearchIndexer.SearchFields.RECIPIENT, (int)start, (int)count*10, SearchIndexer.SearchFields.RECEIVED,false);
		for (int i = 0; i < results.length;i++) {
			String uid = results[i].get(SearchFields.UID.name());
			InboxAddress inbox;
			inbox = new InboxAddress(results[i].get(Utils.decodeRFC2407(SearchFields.INBOX.name())));
			
			if (!contains(children,inbox.getAddress())) {
				curr = new JSONObject();
				curr.put("name", inbox.getAddress());
				curr.put("label",search.getRecipient(inbox,results[i].get(SearchFields.RECIPIENT.name())).getFullAddress());
				curr.put("identifier", uid);
				children.put(curr);
			}
			if (children.length()>=count)
				break;
		}

		return children;
	}

	private boolean contains(JSONArray children, String name) {
		for (int i = 0; i < children.length();i++) {
			try {
				if (children.getJSONObject(i).getString("name").equals(name)) {
					return true;
				}
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public long getStatsGlobalCount() {
		return prefs.getLong(GLOBAL_COUNT_NODE, 0);	
	}

	public void setStatsGlobalCount(long count) {
		prefs.putLong(GLOBAL_COUNT_NODE,count);
	}

	private void incrementGlobalCount() {
		setStatsGlobalCount(getStatsGlobalCount()+1);
	}

	public JSONObject getStatsRecent() {
		return recentStats;
	}


	public JSONObject getStatsActiveInbox() {	
		return StorageFactory.getInstance().getMostActiveInbox();
	}

	public JSONObject getStatsActiveSender() {	
		return StorageFactory.getInstance().getMostActiveSender();
	}

	private JSONObject updateStatsRecent(String inbox, String from, String subject, String uid) {
		try {
			//			recentStats.put(BlueboxMessage.SUBJECT, StringEscapeUtils.escapeJavaScript(MimeUtility.decodeText(subject)));
			recentStats.put(BlueboxMessage.SUBJECT, MimeUtility.decodeText(subject));
			recentStats.put(BlueboxMessage.INBOX, StringEscapeUtils.escapeJavaScript(inbox));
			recentStats.put(BlueboxMessage.FROM, StringEscapeUtils.escapeJavaScript(from));
			recentStats.put(BlueboxMessage.UID, uid);
		} 
		catch (Throwable e1) {
			e1.printStackTrace();
		}

		return recentStats;
	}

	public WorkerThread rebuildSearchIndexes() {

		WorkerThread wt = new WorkerThread("reindex") {

			@Override
			public void run() {
				try {
					SearchIndexer searchIndexer = SearchIndexer.getInstance();
					searchIndexer.deleteIndexes();
					MessageIterator mi = new MessageIterator(null, BlueboxMessage.State.NORMAL);
					while (mi.hasNext()) {
						searchIndexer.indexMail(mi.next());						
						setProgress(mi.getProgress());
					}
				} 
				catch (Throwable e) {
					e.printStackTrace();
				}
				finally {
					log.info("Finished rebuilding search indexes");
					setProgress(100);
					setStatus("Indexes rebuilt");
				}

			}

		};
		return wt;
	}

	public void addToWhiteList(String goodDomain) {
		toWhiteList.remove(goodDomain);		
		toWhiteList.add(goodDomain);		
	}

	public List<String> getToWhitelist() {
		return toWhiteList;
	}

	public void addFromWhiteList(String goodDomain) {
		fromWhiteList.remove(goodDomain);	
		fromWhiteList.add(goodDomain);	
	}

	public List<String> getFromWhitelist() {
		return fromWhiteList;
	}

	public List<String> getSMTPBlacklist() {
		return Config.getInstance().getStringList(Config.BLUEBOX_SMTPBLACKLIST);		
	}

	public void addFromBlacklist(String badDomain) {
		log.info("Blacklisting domain {}",badDomain);
		fromBlackList.remove(badDomain);		
		fromBlackList.add(badDomain);		
	}

	public List<String> getFromBlacklist() {
		return fromBlackList;
	}

	public void addToBlacklist(String badDomain) {
		toBlackList.remove(badDomain);		
		toBlackList.add(badDomain);		
	}

	public List<String> getToBlacklist() {
		return toBlackList;
	}

	public void loadConfig() {
		// set up the blacklists and whitelists
		fromBlackList = Config.getInstance().getStringList(Config.BLUEBOX_FROMBLACKLIST);
		toBlackList = Config.getInstance().getStringList(Config.BLUEBOX_TOBLACKLIST);
		toWhiteList = Config.getInstance().getStringList(Config.BLUEBOX_TOWHITELIST);
		fromWhiteList = Config.getInstance().getStringList(Config.BLUEBOX_FROMWHITELIST);		
	}

	public WorkerThread runMaintenance() throws Exception {
		return StorageFactory.getInstance().runMaintenance();
	}

	public WorkerThread backup(final File dir) throws Exception {
		final Inbox inbox = Inbox.getInstance();
		WorkerThread wt = new WorkerThread("backup") {
private File zipFile;
			@Override
			public void run() {
				try {
					zipFile = new File(dir.getCanonicalPath()+File.separator+"bluebox.zip");
					log.info("Backing up mail to "+zipFile.getCanonicalPath());
					BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(zipFile));
					ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
					BlueboxMessage msg;
					String emlFile,jsonFile;
					MessageIterator mi = new MessageIterator(null, BlueboxMessage.State.ANY);
					while (mi.hasNext()) {
						msg = mi.next();
						setProgress(mi.getProgress());
						try {

							emlFile = msg.getIdentifier()+".eml";
							jsonFile = msg.getIdentifier()+".json";
							ZipEntry zipEntry;
							// the blob
							zipEntry = new ZipEntry(emlFile);
							zipOutputStream.putNextEntry(zipEntry);
							msg.getBlueBoxMimeMessage().writeTo(zipOutputStream);
							zipOutputStream.closeEntry();

							// the metadata
							zipEntry = new ZipEntry(jsonFile);
							zipOutputStream.putNextEntry(zipEntry);
							zipOutputStream.write(msg.toJSON().toString().getBytes());
							zipOutputStream.closeEntry();
						}
						catch (Throwable t) {
							log.warn(t.getMessage());
						}
					}

					zipOutputStream.close();
					fileOutputStream.close();
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				finally {
					setProgress(100);			
					try {
						setStatus("Backed up "+inbox.getMailCount(BlueboxMessage.State.ANY)+" mails to "+zipFile.getCanonicalPath());
					} 
					catch (IOException e) {
						setStatus("Error :"+e.getMessage());
					}
				}
			}

		};

		return wt;
	}

	public WorkerThread restore(final File dir) throws Exception {
		final File zipFile = new File(dir.getCanonicalPath()+File.separator+"bluebox.zip");
		log.info("Restoring mail from {}",zipFile.getCanonicalPath());
		WorkerThread wt = new WorkerThread("restore") {

			@Override
			public void run() {
				int restoreCount = 0;
				if (zipFile.exists()) {
					try {
						ZipFile archive = new ZipFile(zipFile);
						@SuppressWarnings("rawtypes")
						Enumeration entries = archive.entries();
						int progress = 0;
						int count = archive.size()/2;
						while (entries.hasMoreElements()) {
							ZipEntry zipEntry = (ZipEntry) entries.nextElement();
							setProgress(progress*100/count);
							log.debug("Progress : {}",(progress*100/count));
							String uid = zipEntry.getName().substring(0,zipEntry.getName().indexOf('.'));
							if ((!StorageFactory.getInstance().contains(uid))&&(zipEntry.getName().endsWith("eml"))) {
								try {
									// read the json metadata
									ZipEntry jsonEntry = archive.getEntry(zipEntry.getName().substring(0, zipEntry.getName().length()-4)+".json");
									JSONObject jo = new JSONObject(Utils.convertStreamToString(archive.getInputStream(jsonEntry)));

									// backwards compat workaround for backups prior to introduction of RECIPIENT field
									if (!jo.has(BlueboxMessage.RECIPIENT)) {
										jo.put(BlueboxMessage.RECIPIENT,jo.get(BlueboxMessage.INBOX));
									}
									else {
										// if it's there, but is a JSONarray, use value of inbox instead
										if (jo.get(BlueboxMessage.RECIPIENT) instanceof JSONArray) {
											// try get actual full name version from this array
											JSONArray ja = jo.getJSONArray(BlueboxMessage.RECIPIENT);
											jo.put(BlueboxMessage.RECIPIENT,jo.get(BlueboxMessage.INBOX));
											for (int j = 0; j < ja.length(); j++) {
												if (ja.getString(j).indexOf(jo.getString(BlueboxMessage.INBOX))>=0) {
													jo.put(BlueboxMessage.RECIPIENT,ja.getString(j));
													break;
												}
											}
										}
									}
									// store the message only if it doesn't already exist
									StorageIf si = StorageFactory.getInstance();
									if (!si.contains(uid)) {
										si.store(jo, archive.getInputStream(zipEntry));
										// index the message
										MimeMessage mm = Utils.loadEML(archive.getInputStream(zipEntry));
										SearchIndexer.getInstance().indexMail(new BlueboxMessage(jo,mm));
										restoreCount++;
									}
									else {
										log.info("Ignoring restore of {}",uid);
									}

								}
								catch (Throwable t) {
									t.printStackTrace();
									log.warn(t.getMessage());
								}
							}
							progress++;
						}
						archive.close();
					} 
					catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					log.error("Could not access zip archive, checking for old style backup");
					try {
						restoreOld(dir);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				setProgress(100);
				setStatus("Restored "+restoreCount+" mails");
			}

		};
		return wt;

	}

	public WorkerThread restoreOld(final File dir) throws Exception {
		log.info("Restoring mail from {}",dir.getCanonicalPath());
		WorkerThread wt = new WorkerThread("restore") {

			@Override
			public void run() {
				if (dir.exists()) {
					File[] files = dir.listFiles();
					for (int i = 0; i < files.length;i++) {
						setProgress(i*100/files.length);
						log.debug("Progress : {}",(i*100/files.length));
						if (files[i].getName().endsWith("eml")) {
							try {
								JSONObject jo = new JSONObject(FileUtils.readFileToString(new File(files[i].getCanonicalPath().substring(0, files[i].getCanonicalPath().length()-4)+".json")));
								// backwards compat workaround for backups prior to introduction of RECIPIENT field
								if (!jo.has(BlueboxMessage.RECIPIENT)) {
									jo.put(BlueboxMessage.RECIPIENT,jo.get(BlueboxMessage.INBOX));
								}
								else {
									// if it's there, but is a JSONarray, use value of inbox instead
									if (jo.get(BlueboxMessage.RECIPIENT) instanceof JSONArray) {
										// try get actual full name version from this array
										JSONArray ja = jo.getJSONArray(BlueboxMessage.RECIPIENT);
										jo.put(BlueboxMessage.RECIPIENT,jo.get(BlueboxMessage.INBOX));
										for (int j = 0; j < ja.length(); j++) {
											if (ja.getString(j).indexOf(jo.getString(BlueboxMessage.INBOX))>=0) {
												jo.put(BlueboxMessage.RECIPIENT,ja.getString(j));
												break;
											}
										}
									}
								}

								InputStream ms = new BufferedInputStream(new FileInputStream(files[i]));
								ms.mark(Integer.MAX_VALUE);
								StorageFactory.getInstance().store(jo, ms);
								ms.reset();
								MimeMessage mm = Utils.loadEML(ms);
								SearchIndexer.getInstance().indexMail(new BlueboxMessage(jo,mm));
							}
							catch (Throwable t) {
								t.printStackTrace();
								log.warn(t.getMessage());
							}
						}
					}
				}
				else {
					log.error("Could not access");
				}
				setProgress(100);
			}

		};
		return wt;

	}

	//	/**
	//	 * This method parses the Received header to see if the sending SMTP server is on our from blacklist.
	//	 * @param message
	//	 * @return false if not a blacklisted sender
	//	 * @throws MessagingException
	//	 */
	//	public boolean isSameSMTPSender(BlueboxMessage message, String smtpDomain) throws MessagingException {
	//		//		from wallstreetads.org ([193.104.41.200])
	//		//        by bluebox.xxx.yyy.com
	//		//        with SMTP (BlueBox) id I2IKBO4B
	//		//        for jan.schumacher@yahoo.de;
	//		//        Fri, 14 Nov 2014 23:57:26 -0600 (CST)
	//		try {
	//			String[] header = message.getBlueBoxMimeMessage().getHeader("Received");
	//			if ((header!=null)&&(header.length>0)) {
	//				StringTokenizer toks = new StringTokenizer(header[0]);
	//				toks.nextToken();// discard the "from
	//				String domain = toks.nextToken();
	//				// check from blacklist
	//
	//				if (domain.indexOf(smtpDomain.toString())>=0) {
	//					return true;
	//				}
	//			}
	//		}
	//		catch (Throwable t) {
	//			log.info("Error checking received header :"+t.getMessage());
	//		}
	//		return false;
	//	}

}
