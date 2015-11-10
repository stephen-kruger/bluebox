package com.bluebox.smtp;

import java.io.BufferedOutputStream;
import java.io.File;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.IndexNotFoundException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchFactory;
import com.bluebox.search.SearchIf;
import com.bluebox.search.SearchUtils;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.LiteMessageIterator;
import com.bluebox.smtp.storage.MessageIterator;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;
import com.bluebox.smtp.storage.mongodb.MongoImpl;

public class Inbox implements SimpleMessageListener {
	private static final String GLOBAL_COUNT_NODE = "global_message_count";
	private static final String BACKUP_WORKER = "backup";
	private JSONObject recentStats = new JSONObject();

	public static final String EMAIL = "Email";

	private static final Logger log = LoggerFactory.getLogger(Inbox.class);
	public static final long MAX_MAIL_BYTES = Config.getInstance().getLong(Config.BLUEBOX_MAIL_LIMIT);
	public static final String EXPIRE_WORKER = "expire";
	public static final String TRIM_WORKER = "trim";
	public static final String ORPHAN_WORKER = "orphan";
	private List<String> fromBlackList, toBlackList, toWhiteList, fromWhiteList;
	private BlueboxMessageHandlerFactory blueboxMessageHandlerFactory;
	//	private Thread migrationThread;

	private static Timer timer = null;
	private static TimerTask timerTask = null;

	public Inbox() {
		loadConfig();
	}

	public void start() {
		log.debug("Starting inbox");

		// start up storage services
		StorageFactory.getInstance();

		// start up search services
		try {
			SearchFactory.getInstance();
		} 
		catch (Exception ioe) {
			ioe.printStackTrace();
			log.error("Error starting search instance",ioe);
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

				@Override
				public void run() {
					log.info("Cleanup timer activated");
					try {
						WorkerThread.startWorker(expireThread());
						WorkerThread.startWorker(trimThread());
						WorkerThread.startWorker(orphanThread());
					} 
					catch (Exception e) {
						log.error("Error running message cleanup",e);
						e.printStackTrace();
					}
				}
			}, delay, period);
		}
		log.info("Started inbox");
		try {
			migrate();
		} 
		catch (Exception e) {
			log.error("Error migrating data",e);
			e.printStackTrace();
		}
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

		// stopping migration thread
		WorkerThread.stopWorker("migrate");

		log.debug("Stopping search engine");
		try {
			SearchFactory.getInstance().stop();
		} 
		catch (Exception e) {
			e.printStackTrace();
			log.error("Error stopping search engine",e);
		}
		log.info("Stopping storage implementation");
		try {
			StorageFactory.getInstance().stop();
		}
		catch (Exception e) {
			e.printStackTrace();
			log.error("Error stopping storage implementation",e);
		}
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

	public long searchInbox(String search, Writer writer, int start, int count, SearchUtils.SearchFields searchScope, SearchUtils.SortFields orderBy, boolean ascending) throws Exception {
		log.debug("Searching for {} ordered by {}",search,orderBy);
		try {
			return SearchFactory.getInstance().searchInboxes(search, writer, start, count, searchScope, orderBy, ascending);
		}
		catch (IndexNotFoundException inf) {
			log.info("Detected index problems ({})",inf.getMessage());
			return 0;
		}
	}

	public void delete(String uid, String rawId) throws Exception {
		StorageFactory.getInstance().delete(uid, rawId);
		SearchFactory.getInstance().deleteDoc(uid);
	}

	/*
	 * Delete the message, and all other mails with same sender address. Add sender to blacklist.
	 */
	public WorkerThread toggleSpam(final List<String> uids) throws Exception {
		WorkerThread wt = new WorkerThread("spam") {

			@Override
			public void run() {
				for (String uid : uids) {
					if (isStopped())
						break;
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
						if ((smtpDomain.trim().length()>0)&&(smtpDomain!="localhost")) {
							// add to blacklist
							if (blueboxMessageHandlerFactory!=null)
								blueboxMessageHandlerFactory.addSMTPBlackList(smtpDomain.trim());
							else
								log.warn("Cannot blacklist SMTP sender, no handler specified");
							BlueboxMessage msg;
							MessageIterator mi = new MessageIterator();
							while (mi.hasNext()) {
								msg = mi.next();
								setProgress(mi.getProgress());
								try {
									if (smtpDomain.indexOf(msg.getSMTPSender())>=0) {
										if (spamAction) {
											log.info("Spam detected - soft deleting {}",msg.getIdentifier());
											softDelete(msg.getIdentifier());
										}
										else {
											log.info("Spam mismatch detected - soft undeleting");
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
			SearchFactory.getInstance().deleteIndexes();
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	//	public WorkerThread cleanUp() throws Exception {
	//		WorkerThread wt = new WorkerThread("cleanup") {
	//
	//			@Override
	//			public void run() {
	//				try {
	//					setProgress(30);
	//					// remove old messages
	//					expire();
	//					setProgress(60);
	//					// trim total mailbox size
	//					trim();
	//					// remove any orphaned blobs
	//					setProgress(90);
	//					orphan();
	//				}
	//				catch (Throwable t) {
	//					t.printStackTrace();
	//				}
	//				finally {
	//					setProgress(100);
	//					setStatus("Complete");
	//				}
	//			}
	//
	//		};
	//
	//		return wt;
	//	}

	public WorkerThread expireThread() throws Exception {
		WorkerThread wt = new WorkerThread(EXPIRE_WORKER) {

			@Override
			public void run() {
				try {
					Date messageExpireDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_AGE)*60*60*1000));
					log.info("Cleaning messages received before {}",messageExpireDate);
					LiteMessage msg;
					int count = 0;
					LiteMessageIterator mi = new LiteMessageIterator(null, BlueboxMessage.State.NORMAL);
					while (mi.hasNext()) {
						setProgress(mi.getProgress());
						msg = mi.next();
						try {
							if (isExpired(msg.getReceived(),messageExpireDate)) {
								delete(msg.getIdentifier(), msg.getRawIdentifier());
								count++;
							}
						}
						catch (Throwable t) {
							log.warn("Problem cleaning up message {} {}",msg.getIdentifier(),t.getMessage());
						}
					}
					log.info("Cleaned up {} messages",count);
				}
				catch (Throwable t) {
					t.printStackTrace();
					log.error("Problem expiring mail",t);
				}
				finally {
					setProgress(100);
					setStatus("Complete");
				}
			}

		};

		return wt;
	}

	public WorkerThread trimThread() throws Exception {
		WorkerThread wt = new WorkerThread(TRIM_WORKER) {

			@Override
			public void run() {
				int errors = 0;
				try {
					long max = Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_MAX);
					StorageIf si = StorageFactory.getInstance();
					final long count = si.getMailCount(BlueboxMessage.State.ANY);
					final long deleteCount = count-max;
					log.info("Trimming {} of total {} mailboxes to limit of {} entries",deleteCount,count,max);
					long deletedCount=0;
					while ((deleteCount>0)&&(si.getMailCount(BlueboxMessage.State.ANY)>max)&&(deletedCount<deleteCount)) {
						List<LiteMessage> list = si.listMailLite(null, BlueboxMessage.State.ANY, 0, 500, BlueboxMessage.RECEIVED, true);
						for (LiteMessage m : list) {
							// this checks the inner loop doesn't delete more than needed
							if (deletedCount<deleteCount) {
								setProgress((int)((deletedCount++)*100/deleteCount));
								try {
									delete(m.getIdentifier(), m.getRawIdentifier());
								}
								catch (Throwable t) {
									errors++;
									log.error("Error trimming message",t);
								}
							}
							else {
								break;
							}
						}
					} 
					setProgress(100);
					log.info("Finished trimming {} messages with {} errors encouters", deleteCount,errors);
				}
				catch (Throwable t) {
					log.error("Problem trimming mailboxes",t);
				}
			}
		};
		return wt;
	}

	protected WorkerThread orphanThread() throws Exception {
		WorkerThread wt = StorageFactory.getInstance().cleanOrphans();
		return wt;
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
					delete(msg.getIdentifier(),msg.getRawUid());
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

	//	public void expire() throws Exception {
	//		Date messageDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_AGE)*60*60*1000));
	//		Date trashDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_TRASH_AGE)*60*60*1000));
	//		//		expireOld(messageDate);
	//		expireDeleted(trashDate);
	//	}

	//	private void expireOld(Date messageExpireDate) throws Exception {
	//		log.info("Cleaning messages received before {}",messageExpireDate);
	//		LiteMessage msg;
	//		int count = 0;
	//		LiteMessageIterator mi = new LiteMessageIterator(null, BlueboxMessage.State.NORMAL);
	//		while (mi.hasNext()) {
	//			msg = mi.next();
	//			try {
	//				if (isExpired(msg.getReceived(),messageExpireDate)) {
	//					delete(msg.getIdentifier(), msg.getRawIdentifier());
	//					count++;
	//				}
	//			}
	//			catch (Throwable t) {
	//				log.warn("Problem cleaning up message {} {}",msg.getIdentifier(),t.getMessage());
	//			}
	//		}
	//		log.info("Cleaned up {} messages",count);
	//	}

	public static final boolean isExpired(Date receievedDate, Date expireDate) {
		return receievedDate.before(expireDate);
	}

	//	private void expireDeleted(Date trashExpireDate) throws Exception {
	//		log.info("Cleaning deleted messages received before {}",trashExpireDate);
	//		LiteMessage msg;
	//		int count = 0;
	//		LiteMessageIterator mi = new LiteMessageIterator(null, BlueboxMessage.State.DELETED);
	//		while (mi.hasNext()) {
	//			msg = mi.next();
	//			try {
	//				if ((msg.getReceived()).before(trashExpireDate)) {
	//					delete(msg.getIdentifier(),msg.getRawIdentifier());
	//					count++;
	//				}				
	//			}
	//			catch (Throwable t) {
	//				log.warn("Problem cleaning up message {}",msg.getIdentifier());
	//			}
	//		}
	//		log.info("Cleaned up {} deleted messages",count);
	//	}

	/*
	 * Some Non Delivery Records have null or <> as from. This hacks in a default
	 * email to allow delivery of the bounced message.
	 */
	public String checkBounce(String from) {
		if ((from==null)||(from.length()==0)||(from.equals("<>"))) {
			from = "bounce@"+Utils.getHostName();
		}
		return from;
	}

	@Override
	public boolean accept(String from, String recipient) {	
		try {
			// if no From is specified, treat as a bounce message and send to bounce@<hostname> inbox
			from = checkBounce(from);
			InboxAddress fromAddress = new InboxAddress(from);
			InboxAddress recipientAddress = new InboxAddress(recipient);
			if (!fromAddress.isValidAddress()) {
				log.warn("Invalid from address specified {} sent to {}",from,recipient);
				return false;
			}
			if (!recipientAddress.isValidAddress()) {
				log.warn("Invalid recipient address specified {} sent from {}",recipient,from);
				return false;
			}

			// check from blacklist
			if (isFromBlackListed(fromAddress)) {
				return false;
			}


			// check to blacklist
			if (isToBlackListed(recipientAddress)) {
				return false;
			}

			// check the from whitelist
			if (fromWhiteList.size()>0) {
				return isFromWhiteListed(fromAddress);
			}

			// check the to whitelist			
			if (toWhiteList.size()>0) {
				return isToWhiteListed(recipientAddress);
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

	public boolean isToBlackListed(InboxAddress recipientAddress) {
		for (Object badDomain : getToBlacklist()) {
			log.debug("{}<<<---- Comparing toBlackList---->>>{}",badDomain,recipientAddress);
			if (recipientAddress.getDomain().endsWith(badDomain.toString())) {
				log.warn("Rejecting mail to {} due to blacklisted TO:",recipientAddress,badDomain);
				return true;
			}
		}
		return false;
	}

	public boolean isFromBlackListed(InboxAddress fromAddress) {
		for (Object badDomain : getFromBlacklist()) {
			log.debug("{}<<<---- Comparing fromBlackList---->>>{}",badDomain,fromAddress);
			if (fromAddress.getDomain().endsWith(badDomain.toString())) {
				log.warn("Rejecting mail from {} due to blacklisted FROM {}",fromAddress,badDomain);
				return true;
			}
		}
		return false;
	}

	public boolean isToWhiteListed(InboxAddress recipientAddress) {
		for (Object goodDomain : toWhiteList) {
			log.debug("{}<<<---- Comparing toWhiteList---->>>{}",goodDomain,recipientAddress);
			if (recipientAddress.getDomain().endsWith(goodDomain.toString())) {
				return true;
			}
		}
		log.warn("Rejecting mail to {} because not in TO whitelist",recipientAddress);
		return false;
	}

	public boolean isFromWhiteListed(InboxAddress fromAddress) {
		for (Object goodDomain : fromWhiteList) {
			log.debug(goodDomain.toString()+"<<<---- Comparing fromWhiteList---->>>{}",fromAddress);
			if (fromAddress.getDomain().endsWith(goodDomain.toString())) {
				return true;
			}
		}
		log.warn("Rejecting mail from {} because not in FROM whitelist",fromAddress);
		return false;
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
		recipient = javax.mail.internet.MimeUtility.decodeText(recipient);
		from = javax.mail.internet.MimeUtility.decodeText(from);

		// if no From is specified, treat as a bounce message and send to bounce@<hostname> inbox
		from = checkBounce(from);

		// when reading from .eml files, we might get multiple recipients, not sure why they are delivered like this.
		// when called via SubEtha, they are normally single recipient addresses
		List<String> recipients = getRecipients(recipient);

		// spool the message to disk
		StorageIf si = StorageFactory.getInstance();
		String spooledUid="";
		try {
			//			MimeMessage mimeMessage = Utils.loadEML(data);
			spooledUid = si.spoolStream(data);
			if (si.getSpooledStreamSize(spooledUid)<MAX_MAIL_BYTES) {
				try {
					MimeMessage mimeMessage = si.getSpooledStream(spooledUid);
					// now send the message to each recipient
					for (String nrec : recipients) {
						try {
							deliver(from, nrec, mimeMessage, spooledUid);
						} 
						catch (Throwable e) {
							log.error("Delivery issue",e.getMessage());
							errorLog("Error accepting raw message from="+from+" for recipient="+nrec+" data bytes="+si.getSpooledStreamSize(spooledUid), e.getMessage());
						}
					}
				}
				catch (MessagingException me) {
					log.error("Problem loading message from stream",me);
				}
			}
			else {
				log.error("Mail exceeded maximum allowed size of "+(MAX_MAIL_BYTES/1000000)+"MB From={}  Recipient={} Size={}",from,recipient,si.getSpooledStreamSize(spooledUid));
				errorLog("Mail exceeded maximum allowed size of "+(MAX_MAIL_BYTES/1000000)+"MB","From="+from+" Recipient="+recipient+" Size="+si.getSpooledStreamSize(spooledUid));
				throw new TooMuchDataException("Mail exceeded maximum allowed size of "+(MAX_MAIL_BYTES/1000000)+"MB");
			}

		}
		catch (Exception ioe) {
			ioe.printStackTrace();
		}
		//		finally {
		//			try {
		//				si.removeSpooledStream(spooledUid);
		//			} 
		//			catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//		}
	}

	public void deliver(String from, String recipient, MimeMessage mimeMessage, String spooledUid) throws Exception {
		from = getFullAddress(from, mimeMessage.getFrom());
		recipient = getFullAddress(recipient, mimeMessage.getAllRecipients());
		log.debug("Delivering mail for {} from {}",recipient,from);

		BlueboxMessage blueboxMessage = StorageFactory.getInstance().store( 
				from,
				new InboxAddress(recipient),
				StorageFactory.getInstance().getUTCTime(),
				mimeMessage,
				spooledUid);

		// if backup is requested, make sure new messages are are backed-up as they arrive
		WorkerThread wt;
		if ((wt = WorkerThread.getInstance(BACKUP_WORKER))!=null) {
			log.info("Backing up newly arrive message");
			wt.generic(blueboxMessage);
		}
		updateStatsRecent(blueboxMessage.getInbox().getAddress(),from,blueboxMessage.getSubject(),blueboxMessage.getIdentifier());	

		// ensure the content is indexed
		try {
			SearchFactory.getInstance().indexMail(blueboxMessage,false);
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
	}

	private List<String> getRecipients(String recipient) {
		List<String> recipients = new ArrayList<String>();
		if (recipient.indexOf(',')>=0) {
			StringTokenizer tok = new StringTokenizer(recipient,",");
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
		}
		else {
			recipients.add(recipient);
		}
		return recipients;
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

	private void updateStatsRecent(String inbox, String from, String subject, String uid) {
		try {
			recentStats.put(BlueboxMessage.SUBJECT, MimeUtility.decodeText(subject));
			recentStats.put(BlueboxMessage.INBOX, StringEscapeUtils.escapeJavaScript(inbox));
			recentStats.put(BlueboxMessage.FROM, StringEscapeUtils.escapeJavaScript(from));
			recentStats.put(BlueboxMessage.UID, uid);
			incrementGlobalCount();
		} 
		catch (Throwable e1) {
			e1.printStackTrace();
		}
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
		SearchFactory.getInstance().deleteDoc(uid);
	}

	public void softUndelete(String uid, BlueboxMessage message) throws Exception {
		setState(uid, BlueboxMessage.State.NORMAL);
		SearchFactory.getInstance().indexMail(message,false);
	}

	private void setState(String uid, BlueboxMessage.State state) throws Exception {
		StorageFactory.getInstance().setState(uid, state);
	}

	public JSONArray autoComplete(String hint, long start, long count) throws Exception {
		return SearchFactory.getInstance().autoComplete(hint, start, count);
	}

	public long getStatsGlobalCount() {
		return StorageFactory.getInstance().getLongProperty(GLOBAL_COUNT_NODE,48755551);	
	}

	public void setStatsGlobalCount(long count) {
		StorageFactory.getInstance().setProperty(GLOBAL_COUNT_NODE,Long.toString(count));
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

	public JSONObject getMPH(InboxAddress inbox) {
		return StorageFactory.getInstance().getMPH(inbox);
	}

	public WorkerThread rebuildSearchIndexes() throws Exception {

		WorkerThread wt = new WorkerThread("reindex") {

			@Override
			public void run() {
				try {
					SearchIf searcher = SearchFactory.getInstance();
					searcher.deleteIndexes();
					MessageIterator mi = new MessageIterator(null, BlueboxMessage.State.NORMAL);
					while (mi.hasNext()) {
						if (isStopped()) break;
						searcher.indexMail(mi.next(),false);						
						setProgress(mi.getProgress());
					}
					searcher.commit(true);
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
		synchronized (toWhiteList) {
			log.info("Whitelisting mail to domain {}",goodDomain);
			toWhiteList.remove(goodDomain);		
			toWhiteList.add(goodDomain);		
		}
	}

	public List<String> getToWhitelist() {
		return toWhiteList;
	}

	public void addFromWhiteList(String goodDomain) {
		synchronized (fromWhiteList) {
			log.info("Whitelisting mail from domain {}",goodDomain);
			fromWhiteList.remove(goodDomain);	
			fromWhiteList.add(goodDomain);	
		}
	}

	public List<String> getFromWhitelist() {
		return fromWhiteList;
	}

	public List<String> getSMTPBlacklist() {
		return Config.getInstance().getStringList(Config.BLUEBOX_SMTPBLACKLIST);		
	}

	public void setSMTPBlacklist(String s) {
		blueboxMessageHandlerFactory.setSMTPBlacklist(s);
	}

	public void addFromBlacklist(String badDomain) {
		synchronized (fromBlackList) {
			log.info("Blacklisting mail from domain {}",badDomain);
			fromBlackList.remove(badDomain);		
			fromBlackList.add(badDomain);	
		}
	}

	public List<String> getFromBlacklist() {
		return fromBlackList;
	}

	public void addToBlacklist(String badDomain) {
		synchronized (toBlackList) {
			log.info("Blacklisting mail to domain {}",badDomain);
			toBlackList.remove(badDomain);		
			toBlackList.add(badDomain);		
		}
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
		File backupFile = new File(dir.getCanonicalPath()+File.separator+"bluebox.zip");
		return backupTo(StorageFactory.getInstance(), backupFile);
	}

	public static WorkerThread backupTo(final StorageIf si, final File zipFile) throws Exception {
		WorkerThread wt = new WorkerThread(BACKUP_WORKER) {
			private ZipOutputStream zipOutputStream;
			private int count = 0;

			@Override
			public synchronized void generic(Object obj) {
				BlueboxMessage msg = (BlueboxMessage)obj;
				try {
					if (zipOutputStream==null) {
						log.error("Unable to backup file - backup thread not started or already finished");
						return;
					}
					String emlFile = msg.getIdentifier()+".eml";
					String jsonFile = msg.getIdentifier()+".json";
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

					count++;
				}
				catch (Throwable t) {
					log.warn(t.getMessage());
				}	
			}

			@Override
			public void run() {
				try {
					log.info("Backing up mail to {}",zipFile.getCanonicalPath());
					BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(zipFile));
					zipOutputStream = new ZipOutputStream(fileOutputStream);
					BlueboxMessage msg;
					MessageIterator mi = new MessageIterator(si, null, BlueboxMessage.State.ANY);
					while (mi.hasNext()) {
						if (isStopped()) break;
						msg = mi.next();
						setProgress(mi.getProgress());
						generic(msg);
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
						setStatus("Backed up "+count+" mails to "+zipFile.getCanonicalPath());
					} 
					catch (IOException e) {
						setStatus("Error :"+e.getMessage());
					}
				}
			}

		};

		return wt;
	}

	public void migrate() throws Exception {
		WorkerThread wt = new WorkerThread("migrate") {

			@Override
			public void run() {
				// backup old inbox
				StorageIf si = StorageFactory.getInstance();
				if (si instanceof MongoImpl) {
					log.info("Checking for migration triggers");
					@SuppressWarnings("deprecation")
					StorageIf oldStorage = new com.bluebox.smtp.storage.mongodb.StorageImpl();
					try {
						oldStorage.start();
						if (oldStorage.getMailCount(State.ANY)>0) {
							log.info("Preparing to migrate");
							File backupFile = File.createTempFile("migration", "zip");
							backupFile.deleteOnExit();
							WorkerThread backupThread = backupTo(oldStorage, backupFile);
							Thread t = WorkerThread.startWorker(backupThread);
							while (t.isAlive()) {
								Thread.sleep(5000);
								log.info("Migrating data (export) {}% complete",backupThread.getProgress());
							}
							/// restore to new inbox
							log.info("Restoring migrated data");
							WorkerThread restoreThread = restoreTo(backupFile);
							t = WorkerThread.startWorker(restoreThread);
							while (t.isAlive()) {
								Thread.sleep(5000);
								log.info("Migrating data (import) {}% complete",restoreThread.getProgress());
							}

							log.info("Migration complete, cleaning up data");

							// delete backup file
							backupFile.delete();

							// delete the old data
							oldStorage.deleteAll();

							// stop old storage
							oldStorage.stop();
							log.info("Migration completed, old data deleted from database");
						}
						else {
							log.info("No migration needed - old storage was empty");
						}
					}
					catch (Throwable t) {
						t.printStackTrace();
					}
					finally {
						try {
							log.info("Shutting down old storage driver");
							oldStorage.stop();
						} 
						catch (Exception e) {
							e.printStackTrace();
						}
					}
				}		
				else {
					log.info("No migration needed for current driver");
				}
			}

		};
		WorkerThread.startWorker(wt);
	}

	public WorkerThread restore(final File dir) throws Exception {
		return restoreTo(new File(dir.getCanonicalPath()+File.separator+"bluebox.zip"));
	}

	public WorkerThread restoreTo(final File zipFile) throws Exception {
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
						int count = archive.size();
						Date expireDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_AGE)*60*60*1000));
						while (entries.hasMoreElements()) {
							if (isStopped()) break;
							ZipEntry zipEntry = (ZipEntry) entries.nextElement();
							progress++;
							setProgress(progress*100/count);
							log.debug("Restore  : progress={} count={} %={}",progress,count,(progress*100/count));
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
									// store the message only if it doesn't already exist and is not expired
									StorageIf si = StorageFactory.getInstance();

									if ((!isExpired(new Date(jo.getLong(BlueboxMessage.RECEIVED)), expireDate))&&(!si.contains(uid))) {
										String rawId = si.spoolStream(archive.getInputStream(zipEntry));
										// set up the text and html body
										BlueboxMessage m = new BlueboxMessage(jo,si.getSpooledStream(rawId));
										jo.put(BlueboxMessage.HTML_BODY, SearchUtils.htmlToString(m.getHtml(null).toLowerCase()));
										jo.put(BlueboxMessage.TEXT_BODY, m.getText());
										si.store(jo, rawId);
										// index the message
										MimeMessage mm = Utils.loadEML(archive.getInputStream(zipEntry));
										SearchFactory.getInstance().indexMail(new BlueboxMessage(jo,mm),false);
										restoreCount++;
									}
									else {
										log.info("Ignoring restore of {} as it already exists or is expired",uid);
									}
								}
								catch (Throwable t) {
									t.printStackTrace();
									log.warn(t.getMessage());
								}
							}
						}
						SearchFactory.getInstance().commit(true);
						archive.close();
					} 
					catch (Throwable e) {
						e.printStackTrace();
					}
				}
				setProgress(100);
				setStatus("Restored "+restoreCount+" mails");
			}

		};
		return wt;

	}

	public void setBlueboxMessageHandlerFactory(BlueboxMessageHandlerFactory blueboxMessageHandlerFactory) {
		this.blueboxMessageHandlerFactory = blueboxMessageHandlerFactory;		
	}

	public WorkerThread cleanOrphans() throws Exception {
		return StorageFactory.getInstance().cleanOrphans();
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
