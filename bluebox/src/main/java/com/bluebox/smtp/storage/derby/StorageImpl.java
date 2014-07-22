package com.bluebox.smtp.storage.derby;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.StorageIf;

public class StorageImpl extends AbstractStorage implements StorageIf {
	private static final Logger log = Logger.getAnonymousLogger();
	private static final String INBOX_TABLE = "INBOX";
	private static final String PROPS_TABLE = "PROPERTIES";
	public static final String KEY = "keyname";
	public static final String VALUE = "value";
	public static final String ERROR_COUNT = "error_count";
	public static final String ERROR_TITLE = "error_title";
	public static final String ERROR_CONTENT = "error_content";
	public static final String RAW = "pic";
	private boolean started = false;

	public void start() throws Exception {
		if (started) {
			throw new Exception("Storage instance already started");
		}
		log.info("Starting Derby repository");
		Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
		int count = 10;
		while (count-- > 0) {
			try {
				started = true;
				setupTables();
				log.info("Started Derby repository.");
				count = 0;
			}
			catch (Throwable t) {
				log.warning("Trying again "+t.getMessage());
				Thread.sleep(750);
				started=false;
			}
		}
	}

	public void stop() throws Exception {
		if (!started) {
			throw new Exception("Storage instance was not running");
		}
		log.info("Stopping Derby repository");
		try {
			//StorageFactory.clearInstance();
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		}
		catch (Throwable t) {
			log.warning(t.getMessage());
		}
		// force gc to unload the derby classes
		//http://db.apache.org/derby/docs/10.3/devguide/tdevdvlp20349.html
		System.gc();
		started = false;
	}

	public Connection getConnection() throws Exception {
		if (!started) {
			Exception e = new Exception("Storage instance not started");
//			e.printStackTrace();
			throw e;
		}
		System.setProperty("derby.language.logQueryPlan", "false");
		String url = "jdbc:derby:bluebox.derby;create=true";
		Connection conn = DriverManager.getConnection(url);
		return conn;
	}

	public void clear() {
		try {
			dropTables();
		}
		catch (Throwable t) {
			log.warning(t.getMessage());
		}
		try {
			setupTables();
		}
		catch (Throwable t) {
			log.warning(t.getMessage());
		}
	}

	private void dropTables() throws Exception {
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		s.executeUpdate("DROP TABLE "+INBOX_TABLE);
		s.executeUpdate("DROP TABLE "+PROPS_TABLE);
		s.close();
	}

	private void createIndexes(String table, String[] indexes) throws Exception {
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		for (int i = 0; i < indexes.length; i++) {
			try {
				s.executeUpdate("CREATE INDEX "+indexes[i]+"_INDEX_ASC ON "+table+"("+indexes[i]+" ASC)");
			}
			catch (Throwable t) {
				log.fine("Problem creating asc index "+indexes[i]+" ("+t.getMessage()+")");
			}
			try {
				s.executeUpdate("CREATE INDEX "+indexes[i]+"_INDEX_DESC ON "+table+"("+indexes[i]+" DESC)");
			}
			catch (Throwable t) {
				log.fine("Problem creating asc index "+indexes[i]+" ("+t.getMessage()+")");
			}
		}
		s.close();
		connection.close();
	}

	private void setupTables() throws Exception {
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		try {
			s.executeUpdate("CREATE TABLE "+INBOX_TABLE+
					" ("+
					BlueboxMessage.UID+" VARCHAR(36), "+
					BlueboxMessage.INBOX+" VARCHAR(255), "+
					BlueboxMessage.TO+" VARCHAR(255), "+
					BlueboxMessage.FROM+" VARCHAR(255), "+
					BlueboxMessage.SUBJECT+" VARCHAR(255), "+
					BlueboxMessage.RECEIVED+" TIMESTAMP, "+
					BlueboxMessage.STATE+" INTEGER, "+
					BlueboxMessage.SIZE+" BIGINT, "+
					RAW+" blob(16M))");
		}
		catch (Throwable t) {
			log.fine(t.getMessage());
		}
		try {
			s.executeUpdate("CREATE TABLE "+PROPS_TABLE+" ("+KEY+" VARCHAR(256), "+VALUE+" VARCHAR(512))");
		}
		catch (Throwable t) {
			log.fine(t.getMessage());
		}
		s.close();
		connection.close();

		String[] indexes = new String[]{BlueboxMessage.UID,BlueboxMessage.INBOX,BlueboxMessage.TO,BlueboxMessage.FROM,BlueboxMessage.SUBJECT,BlueboxMessage.SIZE,BlueboxMessage.RECEIVED};
		createIndexes(INBOX_TABLE,indexes);

		indexes = new String[]{KEY,VALUE};
		createIndexes(PROPS_TABLE,indexes);
	}

	private String add(String id, InboxAddress inbox, String from, String subject, Date date, State state, long size, InputStream blob) throws Exception {
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("INSERT INTO "+INBOX_TABLE+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
		ps.setString(1, id);
		ps.setString(2, inbox.getAddress());
		ps.setString(3, inbox.getFullAddress());
		ps.setString(4, from);
		ps.setString(5, subject);
		ps.setTimestamp(6, new Timestamp(date.getTime()));
		ps.setInt(7, state.ordinal());
		ps.setLong(8, size);
		ps.setBinaryStream(9, blob);
		ps.execute();
		connection.commit();
		connection.close();

		log.fine("Added mail entry "+inbox.getFullAddress());
		return id;
	}

	public BlueboxMessage store(InboxAddress inbox, String from, MimeMessage bbmm) throws Exception {
		String uid = UUID.randomUUID().toString();
		BlueboxMessage message = new BlueboxMessage(uid,inbox);
		message.setInbox(inbox);
		message.setBlueBoxMimeMessage(from, bbmm);
		if (bbmm.getSubject()==null)
			new Exception().printStackTrace();
		add(uid, 
				inbox, 
				BlueboxMessage.getFrom(from, bbmm),
				bbmm.getSubject(),
				new Date(Long.parseLong(message.getProperty(BlueboxMessage.RECEIVED))), 
				State.NORMAL, 
				bbmm.getSize(),
				message.getRawMessage());
		return message;
	}

	public void delete(String id) throws Exception {
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("DELETE FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
		ps.setString(1, id);
		ps.execute();
		connection.commit();
		connection.close();
		log.fine("Removed mail entry "+id);
	}

	//	public MessageImpl store(String recipient, MimeMessageWrapper message)throws Exception {
	////		MimeMessageWrapper message = rawMessage.getMimeMessage();
	//		return store(new InternetAddress(recipient).getAddress(),recipient,message);
	//	}

	public BlueboxMessage retrieve(String uid) throws Exception {
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
		ps.setString(1, uid);
		ps.execute();
		ResultSet result = ps.getResultSet();
		BlueboxMessage message = null;
		if (result.next()) {
			message = loadMessage(ps.getResultSet());
		}
		connection.close();
		return message;
	}

	public String getDBOString(Object dbo, String key, String def) {
		ResultSet mo = (ResultSet)dbo;
		try {
			String value = mo.getString(key);
			if (value!=null) {
				return value;
			}
			else {
				log.warning("Missing field "+key);
			}
		}
		catch (SQLException sqe) {
			sqe.printStackTrace();
		}
		return def;
	}

	@Override
	public int getDBOInt(Object dbo, String key, int def) {
		ResultSet mo = (ResultSet)dbo;
		try {
			return mo.getInt(key);
		} 
		catch (SQLException e) {
			e.printStackTrace();
			return def;
		}
	}
	
	@Override
	public long getDBOLong(Object dbo, String key, long def) {
		ResultSet mo = (ResultSet)dbo;
		try {
			return mo.getLong(key);
		} 
		catch (SQLException e) {
			e.printStackTrace();
			return def;
		}
	}
	
	public Date getDBODate(Object dbo, String key) {
		ResultSet mo = (ResultSet)dbo;
		try {
			return mo.getTimestamp(key);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public InputStream getDBORaw(Object dbo, String key) {
		ResultSet mo = (ResultSet)dbo;
		try {
			return mo.getBinaryStream(StorageImpl.RAW);
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
			return null;
		}
	}

	//	public BlueboxMessage loadMessage(ResultSet result) throws Exception {
	//
	//		//id VARCHAR(36), email VARCHAR(20), from VARCHAR(20), subject VARCHAR(255), received DATE, state INTEGER, pic blob(16M))");
	//		BlueboxMessage message = new BlueboxMessage(result.getString(BlueboxMessage.UID));
	//		message.setProperty(BlueboxMessage.TO, result.getString(BlueboxMessage.TO));
	//		message.setProperty(BlueboxMessage.INBOX, result.getString(BlueboxMessage.INBOX));
	//		message.setProperty(BlueboxMessage.FROM, result.getString(BlueboxMessage.FROM));
	//		message.setProperty(BlueboxMessage.SUBJECT, result.getString(BlueboxMessage.SUBJECT));
	//		message.setProperty(BlueboxMessage.RECEIVED, Long.toString(result.getTimestamp(BlueboxMessage.RECEIVED).getTime()));
	//
	//		message.setProperty(BlueboxMessage.STATE, State.values()[result.getInt(BlueboxMessage.STATE)].name());
	//		MimeMessageWrapper mmw = new MimeMessageWrapper(null, result.getBinaryStream(StorageImpl.RAW));
	//		message.loadBlueBoxMimeMessage(mmw);
	//		int size = message.getBlueBoxMimeMessage().getSize()/1000;
	//		if (size==0)
	//			size = 1;
	//		message.setProperty(BlueboxMessage.SIZE, Long.toString(size));
	//		return message;
	//	}

	public void deleteAll(InboxAddress inbox) throws Exception {
		log.fine("Deleting inbox "+inbox);
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("DELETE FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.TO+"=?");
		ps.setString(1, inbox.getAddress());
		ps.execute();
		ps.close();
		connection.close();
	}

	@Override
	public void deleteAll() throws Exception {
		log.fine("Deleting all inboxes");
		Connection connection = getConnection();
		Statement s;
		s = connection.createStatement();
		s.execute("delete from "+INBOX_TABLE);
		s.close();

		log.info("Deleting all properties");
		s = connection.createStatement();
		s.execute("delete from "+PROPS_TABLE);
		s.close();
		connection.close();
	}

	@Override
	public long getMailCount(BlueboxMessage.State state) throws Exception {
		long start = new Date().getTime();
		long count = 0;
		Connection connection = getConnection();
		PreparedStatement ps;
		if (state==State.ANY) {
			ps = connection.prepareStatement("SELECT COUNT(*) from "+INBOX_TABLE);
		}
		else {
			ps = connection.prepareStatement("SELECT COUNT(*) from "+INBOX_TABLE+" where "+BlueboxMessage.STATE+"=?");
			ps.setInt(1, state.ordinal());
		}
		ps.execute();
		ResultSet result = ps.getResultSet();
		if (result.next()) {
			count = result.getLong(1);
		}
		ps.close();
		connection.close();

		log.fine("Calculated mail count ("+count+") in "+(new Date().getTime()-start)+"ms");
		return count;
	}

	@Override
	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		if ((inbox==null)||(inbox.getAddress().length()==0))
			return getMailCount(state);
		//		String inbox = Utils.getEmail(email);
		long start = new Date().getTime();
		long count = 0;
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		PreparedStatement ps;
		if (state==State.ANY) {
			ps = connection.prepareStatement("SELECT COUNT(*) from "+INBOX_TABLE+" WHERE "+BlueboxMessage.INBOX+"=?");
			ps.setString(1, inbox.getAddress());
		}
		else {
			ps = connection.prepareStatement("SELECT COUNT(*) from "+INBOX_TABLE+" where "+BlueboxMessage.INBOX+"=? AND "+BlueboxMessage.STATE+"=?");
			ps.setString(1, inbox.getAddress());
			ps.setInt(2, state.ordinal());
		}
		ps.execute();
		ResultSet result = ps.getResultSet();
		if (result.next()) {
			count = result.getLong(1);
		}
		ps.close();
		s.close();
		connection.close();

		log.fine("Calculated mail count for "+inbox+" ("+count+") in "+(new Date().getTime()-start)+"ms");
		return count;
	}

	public List<BlueboxMessage> listMail(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		if (count<0)
			count = Integer.MAX_VALUE;
		String orderStr;
		if (ascending)
			orderStr = " ASC";
		else
			orderStr = " DESC";
		if (email!=null)
			if (email.getAddress().length()==0)
				email=null;
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		PreparedStatement ps;
		if (email==null) {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
			}
			else {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setInt(1, state.ordinal());
			}
		}
		else {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
			}
			else {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=? AND "+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
				ps.setInt(2, state.ordinal());
			}
		}
		ps.execute();
		ResultSet result = ps.getResultSet();
		List<BlueboxMessage> list = new ArrayList<BlueboxMessage>();
		while (result.next()) {
			BlueboxMessage m = loadMessage(result); 
			list.add(m);
		}
		s.close();
		connection.close();
		return list;
	}

	public List<JSONObject> listMailLite(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		if (count<0)
			count = Integer.MAX_VALUE;
		String orderStr;
		if (ascending)
			orderStr = " ASC";
		else
			orderStr = " DESC";
		if (email!=null)
			if (email.getAddress().length()==0)
				email=null;
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		PreparedStatement ps;
		if (email==null) {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
			}
			else {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setInt(1, state.ordinal());
			}
		}
		else {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
			}
			else {
				ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=? AND "+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
				ps.setInt(2, state.ordinal());
			}
		}
		ps.execute();
		ResultSet result = ps.getResultSet();
		List<JSONObject> list = new ArrayList<JSONObject>();
		while (result.next()) {
			JSONObject message = loadMessageJSON(result);			
			list.add(message);
		}
		s.close();
		connection.close();
		return list;
	}

	//	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
	//		long startTime = new Date().getTime();
	//		List<JSONObject> mail = listMailLite(inbox, state, start, count, orderBy, ascending);
	//
	////		JSONObject curr;
	//		int index = 0;
	//		writer.write("[");
	//		for (JSONObject curr : mail) {
	////			curr = new JSONObject();
	////			curr.put(BlueboxMessage.FROM, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.FROM)));
	////			curr.put(BlueboxMessage.SUBJECT, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.SUBJECT)));
	////			// convert the date to the locale used by the users browser
	////			if (message.hasProperty(BlueboxMessage.RECEIVED)) {
	////				curr.put(BlueboxMessage.RECEIVED, SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, locale).format(new Date(message.getLongProperty(BlueboxMessage.RECEIVED))));
	////			}
	////			if (message.hasProperty(BlueboxMessage.SIZE)) {
	////				curr.put(BlueboxMessage.SIZE, message.getPropertyString(BlueboxMessage.SIZE)+"K");
	////			}
	////			else {
	////				curr.put(BlueboxMessage.SIZE, "1K");
	////			}
	////			curr.put(BlueboxMessage.UID, message.getIdentifier());
	//			writer.write(curr.toString(3));
	//			if ((index++)<mail.size()-1) {
	//				writer.write(",");
	//			}
	//		}
	//		writer.write("]");
	//		writer.flush();
	//		log.fine("Served inbox contents in "+(new Date().getTime()-startTime)+"ms");
	//	}

	@Override
	public void setState(String uid, BlueboxMessage.State state) throws Exception {
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("UPDATE "+INBOX_TABLE+" SET "+BlueboxMessage.STATE+"=? WHERE "+BlueboxMessage.UID+"=?");
		ps.setInt(1, state.ordinal());
		ps.setString(2, uid);
		ps.execute();
		connection.commit();
		connection.close();
		log.fine("Update mail entry "+uid+" to "+state);
	}

	//	public JSONArray autoCompleteOld(String hint, long start, long count) throws Exception {
	//		Connection connection = getConnection();
	//		Statement s = connection.createStatement();
	//		PreparedStatement ps = connection.prepareStatement("SELECT "+MessageImpl.TO+","+MessageImpl.INBOX+" FROM "+TABLE_NAME+" WHERE "+MessageImpl.TO+" LIKE ?");
	//		ps.setString(1, hint);
	//		ps.execute();
	//		ResultSet result = ps.getResultSet();
	//		JSONArray children = new JSONArray();
	//
	//		JSONObject curr;
	//		while (result.next()) {
	//			String name = Utils.decodeRFC2407(result.getString(MessageImpl.INBOX));
	//			String label = Utils.decodeRFC2407(result.getString(MessageImpl.TO));
	//			String identifier = result.getString("id");
	//			curr = new JSONObject();
	//			curr.put("name", name);
	//			curr.put("label", label);
	//			curr.put("identifier", identifier);
	//			children.put(curr);
	//			if (children.length()>=count)
	//				break;
	//		}
	//		s.close();
	//		connection.close();
	//		return children;
	//	}

	//	@Override
	//	public JSONArray autoComplete(String hint, long start, long count) throws Exception {
	//		JSONObject curr;
	//		JSONArray children = new JSONArray();
	//		// no need to include wildcard
	//		if (hint.contains("*")) {
	//			hint=hint.substring(0,hint.indexOf('*'));
	//		}
	//		if (hint.length()==1)
	//			return children;
	//		if (hint.length()>1) {			
	//			hint = QueryParser.escape(hint);
	//			SearchIndexer search = SearchIndexer.getInstance();
	//			Document[] results = search.search(hint, SearchIndexer.SearchFields.TO, (int)start, (int)count, SearchIndexer.SearchFields.TO.name());
	//			for (int i = 0; i < results.length;i++) {
	//				String uid = results[i].get(SearchFields.UID.name());
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
	//				if (children.length()>=count)
	//					break;
	//			}
	//		}
	//		else {
	//			List<BlueboxMessage> mail =  listMail(null, BlueboxMessage.State.NORMAL, 0, 100, BlueboxMessage.RECEIVED, true);
	//			for (BlueboxMessage message : mail) {
	//				curr = new JSONObject();
	//				curr.put("name", message.getProperty(BlueboxMessage.INBOX));
	//				curr.put("label", message.getProperty(BlueboxMessage.TO));
	//				curr.put("identifier", message.getIdentifier());
	//				if (!contains(children,curr.getString("name")))
	//					children.put(curr);
	//				if (children.length()>=count)
	//					break;
	//			}
	//		}
	//
	//		return children;
	//	}

	//	private boolean contains(JSONArray children, String name) {
	//		for (int i = 0; i < children.length();i++) {
	//			try {
	//				if (children.getJSONObject(i).getString("name").equals(name)) {
	//					return true;
	//				}
	//			} 
	//			catch (JSONException e) {
	//				e.printStackTrace();
	//			}
	//		}
	//		return false;
	//	}

	@Override
	public void setProperty(String key, String value) {
		if (value.length()>512) {
			value = value.substring(0,512);
			log.severe("Truncating data to fit 512 field");
		}
		try {
			Connection connection = getConnection();
			PreparedStatement ps;
			if (getProperty(key,"xxx").equals("xxx")) {
				ps = connection.prepareStatement("INSERT INTO "+PROPS_TABLE+" VALUES (?,?)");
				ps.setString(1, key);
				ps.setString(2, value);
			}
			else {
				ps = connection.prepareStatement("UPDATE "+PROPS_TABLE+" SET "+VALUE+"=? WHERE "+KEY+"=?");
				ps.setString(1, value);
				ps.setString(2, key);			}

			ps.execute();
			connection.commit();
			connection.close();
		}
		catch (Throwable t) {
			log.warning(t.getMessage());
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String value = defaultValue;
		try {
			Connection connection = getConnection();
			PreparedStatement ps = connection.prepareStatement("SELECT "+VALUE+" FROM "+PROPS_TABLE+" WHERE "+KEY+"=?");
			ps.setString(1, key);
			ps.execute();
			ResultSet result = ps.getResultSet();
			if (result.next()) {
				value =  result.getString(VALUE);
			}
			ps.close();
			connection.close();
		}
		catch (Throwable t) {
			log.warning(t.getMessage());
		}
		return value;
	}

	@Override
	public boolean hasProperty(String key) {
		String r = Long.toString(new Random().nextLong());
		return !getProperty(key,r).equals(r);		
	}

	@Override
	public void logError(String title, InputStream content) {
		try {
			int count = logErrorCount();
			count++;
			setProperty(ERROR_TITLE+count,title);
			setProperty(ERROR_CONTENT+count,Utils.convertStreamToString(content));
			setProperty(ERROR_COUNT,Integer.toString(count));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public int logErrorCount() {
		return Integer.parseInt(getProperty(ERROR_COUNT,"0"));
	}

	@Override
	public void logErrorClear() {
		setProperty(ERROR_COUNT,"0");
		logError("Error db cleared",Utils.convertStringToStream("Requested "+new Date().toString()));
	}

	@Override
	public JSONArray logErrorList(int start, int count) {
		int actualCount = Integer.parseInt(getProperty(ERROR_COUNT,"0"));
		if ((start+count)>actualCount) {
			count = actualCount-start;
			if (count>actualCount)
				count = actualCount;
		}
		try {

			JSONArray result = new JSONArray();
			JSONObject logError;
			for (int i = 1; i <= count; i++) {
				String title = getProperty(ERROR_TITLE+i,"");
				String id = i+"";
				logError = new JSONObject();
				logError.put("title", title);
				logError.put("id", id);
				result.put(logError);
			}
			return result;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	@Override
	public String logErrorContent(String id) {
		return getProperty(ERROR_CONTENT+id,"");
	}

	@Override
	public List<String> listUniqueInboxes() {
		List<String> inboxes = new ArrayList<String>();
		// list unique mails, then count each one
		Connection connection = null;
		try {
			connection = getConnection();
			try {
				Statement s = connection.createStatement();
				PreparedStatement ps;
				ps = connection.prepareStatement("SELECT DISTINCT "+BlueboxMessage.INBOX+" from "+INBOX_TABLE);
				ps.execute();
				ResultSet result = ps.getResultSet();
				while (result.next()) {
					String currInbox = result.getString(BlueboxMessage.INBOX);
					inboxes.add(currInbox);						
				}
				ps.close();
				s.close();
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			try {
				connection.close();
			} 
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return inboxes;
	}

	@Override
	public void runMaintenance() throws Exception {
		setupTables();		
	}



}
