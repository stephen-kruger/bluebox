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
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
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

	private void setupTables() throws Exception {
		Connection connection = getConnection();
		Statement s = connection.createStatement();
		try {
			s.executeUpdate("CREATE TABLE "+INBOX_TABLE+
					" ("+
					BlueboxMessage.UID+" VARCHAR(36), "+
					BlueboxMessage.INBOX+" VARCHAR(255), "+
					//					BlueboxMessage.TO+" VARCHAR(255), "+
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

		String[] indexes = new String[]{BlueboxMessage.UID,BlueboxMessage.INBOX,BlueboxMessage.FROM,BlueboxMessage.SUBJECT,BlueboxMessage.STATE,BlueboxMessage.SIZE,BlueboxMessage.RECEIVED};
		createIndexes(INBOX_TABLE,indexes);

		indexes = new String[]{KEY,VALUE};
		createIndexes(PROPS_TABLE,indexes);
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
	
	private String add(String id, InboxAddress inbox, String from, String subject, Date date, State state, long size, InputStream blob) throws Exception {
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("INSERT INTO "+INBOX_TABLE+" VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		ps.setString(1, id);
		ps.setString(2, inbox.getAddress());
		ps.setString(3, from);
		ps.setString(4, subject);
		ps.setTimestamp(5, new Timestamp(date.getTime()));
		ps.setInt(6, state.ordinal());
		ps.setLong(7, size);
		ps.setBinaryStream(8, blob);
		ps.execute();
		connection.commit();
		connection.close();

		log.fine("Added mail entry "+inbox.getFullAddress());
		return id;
	}

	public BlueboxMessage store(InboxAddress inbox, String from, MimeMessage bbmm) throws Exception {
		String uid = UUID.randomUUID().toString();
		BlueboxMessage message = new BlueboxMessage(uid,inbox);
		message.setBlueBoxMimeMessage(from, bbmm);
		add(uid, 
				inbox, 
				BlueboxMessage.getFrom(from, bbmm),
				bbmm.getSubject(),
				new Date(Long.parseLong(message.getProperty(BlueboxMessage.RECEIVED))), 
				State.NORMAL, 
				Long.parseLong(message.getProperty(BlueboxMessage.SIZE)),
				Utils.streamMimeMessage(bbmm));
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

	public void deleteAll(InboxAddress inbox) throws Exception {
		log.fine("Deleting inbox "+inbox);
		Connection connection = getConnection();
		PreparedStatement ps = connection.prepareStatement("DELETE FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.INBOX+"=?");
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

		log.fine("Deleting all properties");
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
		result.close();
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
		result.close();
		ps.close();
		s.close();
		connection.close();

		log.fine("Calculated mail count for "+inbox+" ("+count+") in "+(new Date().getTime()-start)+"ms");
		return count;
	}

	private ResultSet listMailCommon(String cols, Connection connection, InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
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
		Statement s = connection.createStatement();
		PreparedStatement ps;

		if (email==null) {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
			}
			else {
				ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setInt(1, state.ordinal());
			}
		}
		else {
			if (state==State.ANY) {
				ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
			}
			else {
				ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.INBOX+"=? AND "+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
				ps.setString(1, email.getAddress());
				ps.setInt(2, state.ordinal());
			}
		}
		ps.execute();
		s.close();
		return ps.getResultSet();
	}

	public List<BlueboxMessage> listMail(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		Connection connection = getConnection();
		ResultSet result = listMailCommon("*",connection, email, state, start, count, orderBy, ascending);

		List<BlueboxMessage> list = new ArrayList<BlueboxMessage>();
		while (result.next()) {
			BlueboxMessage m = loadMessage(result); 
			list.add(m);
		}
		result.close();
		connection.close();
		return list;
	}

	public List<JSONObject> listMailLite(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		Connection connection = getConnection();
		String cols = 	BlueboxMessage.UID+","+
				BlueboxMessage.INBOX+","+
				BlueboxMessage.FROM+","+
				BlueboxMessage.SUBJECT+","+
				BlueboxMessage.RECEIVED+","+
				BlueboxMessage.STATE+","+
				BlueboxMessage.SIZE;
		ResultSet result = listMailCommon(cols,connection, email, state, start, count, orderBy, ascending);
		List<JSONObject> list = new ArrayList<JSONObject>();
		while (result.next()) {
			JSONObject message = loadMessageJSON(result,locale);			
			list.add(message);
		}
		connection.close();
		return list;
	}

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
	public JSONObject getMostActiveInbox() {
		long start = new Date().getTime();

		JSONObject jo = new JSONObject();
		try {
			jo.put(BlueboxMessage.COUNT, 0);
			jo.put(BlueboxMessage.INBOX, "");

			Connection connection = null;
			try {
				connection = getConnection();
				try {
					Statement s = connection.createStatement();
					PreparedStatement ps;
					ps = connection.prepareStatement("SELECT DISTINCT "+BlueboxMessage.INBOX+", COUNT(*) FROM "+INBOX_TABLE+" GROUP BY "+BlueboxMessage.INBOX+" ORDER BY COUNT(*) DESC");
					ps.execute();
					ResultSet result = ps.getResultSet();

					while (result.next()) {
						jo.put(BlueboxMessage.INBOX,result.getString(1));
						jo.put(BlueboxMessage.COUNT,result.getLong(2));
						break; // list is already ordered, so first one is biggest
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
		}
		catch (JSONException je) {
			je.printStackTrace();
		}
		log.fine("Calculated active inbox count in "+(new Date().getTime()-start)+"ms");
		return jo;
	}

	@Override
	public JSONObject getMostActiveSender() {
		JSONObject jo = new JSONObject();
		try {
			jo.put(BlueboxMessage.COUNT, 0);
			jo.put(BlueboxMessage.INBOX, "");

			Connection connection = null;
			try {
				connection = getConnection();
				try {
					Statement s = connection.createStatement();
					PreparedStatement ps;
					ps = connection.prepareStatement("SELECT DISTINCT "+BlueboxMessage.FROM+", COUNT(*) FROM "+INBOX_TABLE+" GROUP BY "+BlueboxMessage.FROM+" ORDER BY COUNT(*) DESC");
					ps.execute();
					ResultSet result = ps.getResultSet();

					while (result.next()) {
						jo.put(BlueboxMessage.FROM,result.getString(1));
						jo.put(BlueboxMessage.COUNT,result.getLong(2));
						break; // list is already ordered, so first one is biggest
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
		}
		catch (JSONException je) {
			je.printStackTrace();
		}
		return jo;
	}

	@Override
	public void runMaintenance() throws Exception {
		setupTables();		
	}

}