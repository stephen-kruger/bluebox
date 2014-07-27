package com.bluebox.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexNotFoundException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.search.SearchIndexer;
import com.bluebox.search.SearchIndexer.SearchFields;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;

public class Inbox implements SimpleMessageListener {
	private static final String GLOBAL_COUNT_NODE = "global_message_count";
	Preferences prefs = Preferences.systemNodeForPackage(Inbox.class);
	private JSONObject recentStats = new JSONObject();

	public static final String EMAIL = "Email";
	public static final String START = "Start";
	public static final String COUNT = "Count";
	public static final String ORDERBY = "OrderBy";
	private static final Logger log = Logger.getAnonymousLogger();
	private List<String> fromBlackList, toBlackList, toWhiteList, fromWhiteList;

	private static Timer timer = null;
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
		log.info("Starting inbox");

		// ensure storage instance if loaded and started
		try {
			StorageFactory.getInstance().start();
		} 
		catch (Exception e) {
			log.severe("Error starting storage instance :"+e.getMessage());
			e.printStackTrace();
		}
		// now start a background timer for the mail expiration
		long frequency = Config.getInstance().getLong(Config.BLUEBOX_DAEMON_DELAY);
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		long period = frequency*60*1000;  // repeat every hour.
		long delay = period;   // delay for same amount of time before first run.
		timer = new Timer();

		timer.scheduleAtFixedRate(new TimerTask() {

			public void run() {
				log.info("Cleanup timer activated");
				try {
					cleanUp();
				} 
				catch (Exception e) {
					log.severe("Error running message cleanup");
					e.printStackTrace();
				}
			}
		}, delay, period);
	}

	public void stop() {
		log.info("Stopping inbox");
		try {
			StorageFactory.getInstance().stop();
		}
		catch (Throwable e) {
			log.severe("Error stopping storage :"+e.getMessage());
		}
		log.info("Cleanup timer cancelled");
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		log.info("Stopping search engine");
		try {
			SearchIndexer.getInstance().stop();
		} 
		catch (IOException e) {
			e.printStackTrace();
			log.severe("Error stopping search engine :"+e.getMessage());
		}
		inbox = null;
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
			log.severe(e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		return StorageFactory.getInstance().getMailCount(inbox,state);
	}

	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		log.fine("Sending inbox contents for "+inbox);
		StorageFactory.getInstance().listInbox(inbox, state, writer, start, count, orderBy, ascending, locale);
	}

	public List<BlueboxMessage> listInbox(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		log.fine("Sending inbox contents for "+inbox);
		return StorageFactory.getInstance().listMail(inbox, state, start, count, orderBy, ascending);
	}

	public long searchInbox(String search, Writer writer, int start, int count, SearchIndexer.SearchFields searchScope, SearchIndexer.SearchFields orderBy, boolean ascending) throws Exception {
		log.fine("Searching for "+search+" ordered by "+orderBy);
		try {
			return SearchIndexer.getInstance().searchInboxes(search, writer, start, count, searchScope, orderBy, ascending);
		}
		catch (IndexNotFoundException inf) {
			log.info("Detected index problems - rebuilding search indexes");
			// indexes messed up, so try auto-heal
			Thread searchIndexes = new Thread() {
				public void run() {
					rebuildSearchIndexes();
				}
			};
			searchIndexes.start();
			return 0;
		}
	}

	public void delete(String uid) throws Exception {
		StorageFactory.getInstance().delete(uid);
	}

	public void deleteAll() {
		try {
			StorageFactory.getInstance().deleteAll();
			SearchIndexer.getInstance().deleteIndexes();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cleanUp() throws Exception {
		// remove old messages
		expire();
	}

	private void expire() throws Exception {
		Date messageDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_MESSAGE_AGE)*60*60*1000));
		Date trashDate = new Date(new Date().getTime()-(Config.getInstance().getLong(Config.BLUEBOX_TRASH_AGE)*60*60*1000));
		expire(messageDate, trashDate);
	}

	private void expire(Date messageExpireDate, Date trashExpireDate) throws Exception {
		List<BlueboxMessage> list;

		long count = 0;

		log.info("Cleaning messages received before "+messageExpireDate);
		list = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.NORMAL, 0, -1, BlueboxMessage.RECEIVED, true);
		Date received;
		for (BlueboxMessage msg : list) {
			try {
				if ((received = new Date(Long.parseLong(msg.getProperty(BlueboxMessage.RECEIVED)))).before(messageExpireDate)) {
					StorageFactory.getInstance().delete(msg.getIdentifier());
					SearchIndexer.getInstance().deleteDoc(msg.getIdentifier());
				}
				else {
					log.fine("Not deleting since received:"+received+" but expiry window:"+messageExpireDate);
				}
			}
			catch (Throwable t) {
				log.warning("Problem cleaning up message "+msg.getIdentifier()+" "+t.getMessage());
			}
		}
		log.info("Cleaned up "+count+" messages");

		log.info("Cleaning deleted messages received before "+trashExpireDate);
		count  = 0;
		list = StorageFactory.getInstance().listMail(null, BlueboxMessage.State.DELETED, 0, -1, BlueboxMessage.RECEIVED, true);
		for (BlueboxMessage msg : list) {
			try {
				if ((received = new Date(Long.parseLong(msg.getProperty(BlueboxMessage.RECEIVED)))).before(trashExpireDate)) {
					StorageFactory.getInstance().delete(msg.getIdentifier());
					SearchIndexer.getInstance().deleteDoc(msg.getIdentifier());
					count++;
				}
				else {
					log.fine("Not deleting since received:"+received+" but expiry window:"+messageExpireDate);
				}				
			}
			catch (Throwable t) {
				log.warning("Problem cleaning up message "+msg.getIdentifier());
			}
		}
		log.info("Cleaned up "+count+" deleted messages");
	}

	@Override
	public boolean accept(String from, String recipient) {	

		try {
			InternetAddress fromIA = new InternetAddress(from);
			InternetAddress toIA = new InternetAddress(recipient);

			if (Config.getInstance().getBoolean(Config.BLUEBOX_STRICT_CHECKING)) {
				// validate the email format
				if (!from.contains("@"))return false;
				if (!recipient.contains("@"))return false;
				fromIA.validate();
				toIA.validate();
			}

			// check from blacklist
			for (Object badDomain : fromBlackList) {
				log.finest(badDomain+"<<<---- Comparing fromBlackList---->>>"+fromIA.getAddress());
				if (fromIA.getAddress().endsWith(badDomain.toString())) {
					log.warning("Rejecting mail from "+from+" to "+recipient+" due to blacklisted FROM:"+badDomain);
					return false;
				}
			}
			// check to blacklist
			for (Object badDomain : toBlackList) {
				log.finest(badDomain+"<<<---- Comparing toBlackList---->>>"+toIA.getAddress());
				if (toIA.getAddress().endsWith(badDomain.toString())) {
					log.warning("Rejecting mail from "+from+" to "+recipient+" due to blacklisted TO:"+badDomain);
					return false;
				}
			}

			// check the from whitelist
			if (fromWhiteList.size()>0) {
				for (Object goodDomain : fromWhiteList) {
					log.finest(goodDomain.toString()+"<<<---- Comparing fromWhiteList---->>>"+fromIA.getAddress());
					if (fromIA.getAddress().endsWith(goodDomain.toString())) {
						return true;
					}
				}
				log.warning("Rejecting mail from "+from+" to "+recipient+" because not in FROM whitelist");
				return false;
			}

			// check the to whitelist
			if (toWhiteList.size()>0) {
				for (Object goodDomain : toWhiteList) {
					log.finest(goodDomain.toString()+"<<<---- Comparing toWhiteList---->>>"+toIA.getAddress());
					if (toIA.getAddress().endsWith(goodDomain.toString())) {
						return true;
					}
				}
				log.warning("Rejecting mail from "+from+" to "+recipient+" because not in TO whitelist");
				return false;
			}


			// else we accept everyone
			log.fine("Accepting mail for "+recipient+" from "+from);
			return true;
		}
		catch (Throwable t) {
			log.severe(t.getMessage()+" for from="+from+" and recipient="+recipient);
			errorLog("Accept error for address "+recipient+" sent by "+from, Utils.convertStringToStream(t.toString()));
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
				log.severe(e.getMessage());
				errorLog("("+e.getMessage()+") Accepting raw message for recipient="+recipient +" "+e.getMessage(), data);
				e.printStackTrace();
			}
		}

	}

	public void deliver(String from, String recipient, MimeMessage mmessage) throws Exception {
		log.info("Delivering mail for "+recipient+" from "+from);

		InboxAddress inbox = new InboxAddress(BlueboxMessage.getRecipient(new InboxAddress(recipient), mmessage).toString());
		BlueboxMessage message = StorageFactory.getInstance().store(inbox, 
				from,
				mmessage);
		// ensure the content is indexed
		try {
			SearchIndexer.getInstance().indexMail(message);
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
		}
		updateStats(message, recipient, false);
	}

	public void updateStats(BlueboxMessage message, String recipient, boolean force) throws AddressException {
		incrementGlobalCount();
		if (message!=null)
			updateStatsRecent(message.getProperty(BlueboxMessage.INBOX),message.getProperty(BlueboxMessage.FROM),message.getProperty(BlueboxMessage.SUBJECT));	
	}

	public void clearErrors() throws Exception {
		StorageFactory.getInstance().logErrorClear();
	}

	public void errorLog(String title, InputStream is) {
		StorageFactory.getInstance().logError(title, is);
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

	public void setState(String uid, BlueboxMessage.State state) throws Exception {
		StorageFactory.getInstance().setState(uid, BlueboxMessage.State.DELETED);
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
		//		if (hint.length()==1)
		//			return children;

		//			hint = QueryParser.escape(hint);
		SearchIndexer search = SearchIndexer.getInstance();
		Document[] results = search.search(hint, SearchIndexer.SearchFields.RECIPIENTS, (int)start, (int)count, SearchIndexer.SearchFields.RECEIVED,false);
		for (int i = 0; i < results.length;i++) {
			String uid = results[i].get(SearchFields.UID.name());
			//				BlueboxMessage message = retrieve(uid);
			//				if (message!=null) {
			//					String name = Utils.decodeRFC2407(message.getProperty(BlueboxMessage.INBOX));
			//					String label = Utils.decodeRFC2407(message.getProperty(BlueboxMessage.TO));
			//					String identifier = uid;
			//					curr = new JSONObject();
			//					curr.put("name", name);
			//					curr.put("label", label);
			//					curr.put("identifier", identifier);
			//					if (!contains(children,name))
			//						children.put(curr);
			//				}
			//				else {
			//					log.severe("Sync error between search indexes and derby tables");		
			//				}
			curr = new JSONObject();
			InboxAddress inbox;
			inbox = new InboxAddress(results[i].get(Utils.decodeRFC2407(SearchFields.INBOX.name())));
			curr.put("name", inbox.getAddress());
			//				curr.put("label", Utils.decodeRFC2407(results[i].get(SearchFields.INBOX.name())));
			curr.put("label",search.getRecipient(inbox,results[i].get(SearchFields.RECIPIENTS.name())).getFullAddress());
			curr.put("identifier", uid);
			if (!contains(children,curr.getString("name")))
				children.put(curr);

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

	private JSONObject updateStatsRecent(String to, String from, String subject) {
		try {
			recentStats.put(BlueboxMessage.SUBJECT, subject);
			recentStats.put(BlueboxMessage.INBOX, to);
			recentStats.put(BlueboxMessage.FROM, from);
		} 
		catch (JSONException e1) {
			e1.printStackTrace();
		}

		return recentStats;
	}

	public void rebuildSearchIndexes() {
		try {
			SearchIndexer searchIndexer = SearchIndexer.getInstance();
			searchIndexer.deleteIndexes();
			long mailCount = getMailCount(BlueboxMessage.State.ANY);
			int start = 0;
			int blocksize = 100;
			while (start<mailCount) {
				log.info("Indexing mail batch "+start+" to "+(start+blocksize)+" of "+mailCount);
				try {
					List<BlueboxMessage> messages = listInbox(null, BlueboxMessage.State.ANY, start, blocksize, BlueboxMessage.RECEIVED, true);
					for (BlueboxMessage message : messages) {
						searchIndexer.indexMail(message);
					}
				} 
				catch (Exception e) {
					e.printStackTrace();
				}
				start += blocksize;
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			log.info("Finished rebuilding search indexes");
		}
	}

	public void addToWhiteList(String goodDomain) {
		toWhiteList.add(goodDomain);		
	}

	public void addFromWhiteList(String goodDomain) {
		fromWhiteList.add(goodDomain);	
	}

	public void addFromBlacklist(String badDomain) {
		fromBlackList.add(badDomain);		
	}

	public void addToBlacklist(String badDomain) {
		toBlackList.add(badDomain);		
	}

	public void loadConfig() {
		// set up the blacklists and whielists
		fromBlackList = Config.getInstance().getStringList(Config.BLUEBOX_FROMBLACKLIST);
		toBlackList = Config.getInstance().getStringList(Config.BLUEBOX_TOBLACKLIST);
		toWhiteList = Config.getInstance().getStringList(Config.BLUEBOX_TOWHITELIST);
		fromWhiteList = Config.getInstance().getStringList(Config.BLUEBOX_FROMWHITELIST);		
	}

	public void runMaintenance() throws Exception {
		StorageFactory.getInstance().runMaintenance();
	}


}
