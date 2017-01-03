package com.bluebox.smtp.storage.h2;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Hex;
import org.bson.Document;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.search.SearchUtils.SortFields;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;

public class StorageImpl extends AbstractStorage implements StorageIf {
    private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);
    private static final String INBOX_TABLE = "INBOX";
    private static final String PROPS_TABLE = "PROPERTIES";
    private static final String BLOB_TABLE = "BLOB";
    private static final String KEY = "keyname";
    private static final String VALUE = "value";
    public static final String ERROR_COUNT = "error_count";
    public static final String ERROR_TITLE = "error_title";
    public static final String ERROR_DATE = "error_date";
    public static final String ERROR_CONTENT = "error_content";
    private static final String RAW = "raw";
    private boolean started = false;


    public StorageImpl() {
	log.info("Forcing mail limit to 60K for H2 driver");
	// we know this driver cannot handle more than about 60K mails
	Config config = Config.getInstance();
	if (config.getInt(Config.BLUEBOX_MESSAGE_MAX)>60000) {
	    config.clearProperty(Config.BLUEBOX_MESSAGE_MAX);
	    config.setProperty(Config.BLUEBOX_MESSAGE_MAX, 60000);
	}
    }

    @Override
    public void start() throws Exception {
	if (started) {
	    throw new Exception("H2  Storage instance already started");
	}
	log.debug("Starting H2 repository");
	Class.forName("org.h2.Driver").newInstance();
	int count = 10;
	while (count-- > 0) {
	    try {
		started = true;
		setupTables();
		log.info("Started H2 repository.");
		log.debug("Adding custom defined functions");
		try {
		    createFunction();
		} 
		catch (Exception e) {
		    log.error("Problem adding custom functions",e);
		}
		count = 0;
	    }
	    catch (Throwable t) {
		log.warn("Trying again "+t.getMessage());
		Thread.sleep(750);
		started=false;
	    }
	}
    }

    public String getBlobSize() {
	long max_mail_size = Config.getInstance().getLong(Config.BLUEBOX_MAIL_LIMIT)/1000000;
	if (max_mail_size<=32) {
	    return "32M";
	}
	if (max_mail_size<=64) {
	    return "64M";
	}
	if (max_mail_size<=128) {
	    return "128M";
	}
	if (max_mail_size<=256) {
	    return "256M";
	}

	// default
	return "64M";
    }

    @Override
    public void stop() throws Exception {
	if (!started) {
	    throw new Exception("Storage instance was not running");
	}
	log.debug("Removing custom defined functions");
	try {
	    deleteFunction();
	} 
	catch (Exception e) {
	    log.debug(e.getMessage());
	}
	log.info("Stopping H2 repository");
	try {
	    DriverManager.getConnection("jdbc:h2:;shutdown=true");
	}
	catch (Throwable t) {
	    log.debug(t.getMessage());
	}
	StorageFactory.clearInstance();
	log.info("Stopped H2 repository");

	started = false;
    }

    public Connection getConnection() throws Exception {
	if (!started) {
	    log.error("Storage instance not started, trying to recover");
	    StorageFactory.clearInstance();
	    StorageFactory.getInstance().start();
	    return ((StorageImpl)StorageFactory.getInstance()).getConnection();
	}
	String url = "jdbc:h2:"+getDbName()+";create=true";
	Connection conn = DriverManager.getConnection(url);
	return conn;
    }

    private String getDbName() {
	// TODO Auto-generated method stub
	return "~/"+DB_NAME+"_h2";
    }

    public void clear() {
	try {
	    dropTables();
	}
	catch (Throwable t) {
	    log.warn(t.getMessage());
	}
	try {
	    setupTables();
	}
	catch (Throwable t) {
	    log.warn(t.getMessage());
	}
    }

    private void dropTables() {
	try {
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    s.executeUpdate("DROP TABLE "+INBOX_TABLE);
	    s.close();
	}
	catch (Throwable t) {
	    log.warn("Cannot drop INBOX_TABLE :"+t.getMessage());
	    //			t.printStackTrace();
	}
	try {
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    s.executeUpdate("DROP TABLE "+PROPS_TABLE);
	    s.close();
	}
	catch (Throwable t) {
	    log.warn("Cannot drop PROPS_TABLE :"+t.getMessage());
	    //			t.printStackTrace();
	}
	try {
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    s.executeUpdate("DROP TABLE "+BLOB_TABLE);
	    s.close();
	}
	catch (Throwable t) {
	    log.warn("Cannot drop BLOB_TABLE :"+t.getMessage());
	    //			t.printStackTrace();
	}
    }

    private void setupTables() throws Exception {
	Connection connection = getConnection();
	Statement s = connection.createStatement();
	try {
	    s.executeUpdate("CREATE TABLE "+INBOX_TABLE+
		    " ("+
		    StorageIf.Props.Uid.name()+" VARCHAR(36), "+
		    StorageIf.Props.RawUid.name()+" VARCHAR(255), "+
		    StorageIf.Props.Inbox.name()+" VARCHAR(255), "+
		    StorageIf.Props.Recipient.name()+" VARCHAR(255), "+
		    StorageIf.Props.Sender.name()+" VARCHAR(255), "+
		    StorageIf.Props.Subject.name()+" VARCHAR(255), "+
		    BlueboxMessage.HTML_BODY+" VARCHAR(2048), "+
		    BlueboxMessage.TEXT_BODY+" VARCHAR(2048), "+
		    StorageIf.Props.Received.name()+" TIMESTAMP, "+
		    StorageIf.Props.State.name()+" INTEGER, "+
		    StorageIf.Props.Size.name()+" BIGINT, "+
		    StorageIf.Props.Hideme.name()+" BOOLEAN "+")");
	}
	catch (Throwable t) {
	    log.debug(t.getMessage());
	}

	try {
	    s.executeUpdate("CREATE TABLE "+PROPS_TABLE+" ("+KEY+" VARCHAR(256), "+VALUE+" VARCHAR(512))");
	}
	catch (Throwable t) {
	    log.debug(t.getMessage());
	}

	try {
	    s.executeUpdate("CREATE TABLE "+BLOB_TABLE+" ("+BlueboxMessage.UID+" VARCHAR(255),  "+RAW+" blob("+getBlobSize()+"))");
	}
	catch (Throwable t) {
	    log.debug(t.getMessage());
	}
	s.close();
	connection.close();

	//		String[] indexes = new String[]{BlueboxMessage.UID,BlueboxMessage.INBOX,BlueboxMessage.FROM,BlueboxMessage.SUBJECT,BlueboxMessage.STATE,BlueboxMessage.SIZE,BlueboxMessage.RECEIVED,DOW};
	String[] indexes = new String[]{BlueboxMessage.UID,BlueboxMessage.RAWUID,BlueboxMessage.INBOX,BlueboxMessage.RECIPIENT,BlueboxMessage.FROM,BlueboxMessage.SUBJECT,BlueboxMessage.HTML_BODY,BlueboxMessage.TEXT_BODY,BlueboxMessage.RECEIVED,BlueboxMessage.STATE,BlueboxMessage.SIZE};
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
		log.debug("Problem creating asc index {} ({})",indexes[i],t.getMessage());
	    }
	    try {
		s.executeUpdate("CREATE INDEX "+indexes[i]+"_INDEX_DESC ON "+table+"("+indexes[i]+" DESC)");
	    }
	    catch (Throwable t) {
		log.debug("Problem creating desc index {} ({})",indexes[i],t.getMessage());
	    }
	}
	s.close();
	connection.close();
    }

    @Override
    public void store(JSONObject props, String spooledUid) throws Exception {
	Connection connection = getConnection();
	try {
	    PreparedStatement ps = connection.prepareStatement("INSERT INTO "+INBOX_TABLE+" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)");
	    ps.setString(1, props.getString(StorageIf.Props.Uid.name())); // UID
	    ps.setString(2, props.getString(StorageIf.Props.RawUid.name())); // RAW UID
	    ps.setString(3, props.getString(StorageIf.Props.Inbox.name()));// INBOX
	    ps.setString(4, props.getString(StorageIf.Props.Recipient.name())); // RECIPIENT
	    ps.setString(5, props.getString(StorageIf.Props.Sender.name())); // FROM
	    ps.setString(6, props.getString(StorageIf.Props.Subject.name())); // SUBJECT
	    ps.setString(7, trimString(props.getString(BlueboxMessage.HTML_BODY),2048)); // html body
	    ps.setString(8, trimString(props.getString(BlueboxMessage.TEXT_BODY),2048)); // text body
	    Timestamp timestamp = new Timestamp(props.getLong(StorageIf.Props.Received.name()));
	    ps.setTimestamp(9, timestamp); // RECEIVED
	    ps.setInt(10, props.getInt(StorageIf.Props.State.name())); // STATE
	    ps.setLong(11, props.getLong(StorageIf.Props.Size.name())); // SIZE
	    ps.setBoolean(12, props.getBoolean(StorageIf.Props.Hideme.name())); // HIDEME
	    //			ps.setBinaryStream(11, blob); // MIMEMESSAGE
	    ps.execute();
	    connection.commit();
	}
	catch (Throwable t) {
	    log.error("Error storing message :{}",t);
	    log.info(props.toString(3));
	}
	finally {
	    connection.close();
	}
    }

    private String trimString(String string, int i) {
	if (string.length()>i)
	    return string.substring(0,i);
	return string;
    }

    @Override
    public void delete(String id, String rawid) throws Exception {
	Connection connection = getConnection();
	PreparedStatement ps = connection.prepareStatement("DELETE FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
	ps.setString(1, id);
	ps.execute();
	connection.commit();
	log.debug("Removed mail entry {}",id);

	// now remove the blob if no more references exist
	if (!spoolReferenced(connection,rawid)) {
	    log.info("Removing associated blob");
	    removeSpooledStream(connection,rawid);
	}
	connection.close();
    }

    @Override
    public void delete(List<LiteMessage> bulkList) throws Exception {
	// TODO - improve this using bulk methods
	for (LiteMessage m : bulkList) {
	    delete(m.getIdentifier(),m.getRawIdentifier());
	}	    
    }

    @Override
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

    @Override
    public boolean contains(String uid) {
	boolean contains = false;
	try {
	    Connection connection = getConnection();
	    PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
	    ps.setString(1, uid);
	    ps.execute();
	    ResultSet result = ps.getResultSet();
	    if (result.next()) {
		contains = true;
	    }
	    connection.close();
	}
	catch (Throwable t) {
	    log.error("Problem checking for uid {} ({})",uid,t.getMessage());
	}
	return contains;
    }

    @Override
    public String getDBOString(Object dbo, String key, String def) {
	ResultSet mo = (ResultSet)dbo;
	try {
	    String value = mo.getString(key);
	    if (value!=null) {
		return value;
	    }
	    else {
		log.warn("Missing field "+key);
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
    public boolean getDBOBoolean(Object dbo, String key, boolean def) {
	ResultSet mo = (ResultSet)dbo;
	try {
	    return mo.getBoolean(key);
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

    @Override
    public Date getDBODate(Object dbo, String key, Date def) {
	ResultSet mo = (ResultSet)dbo;
	try {
	    return Utils.getUTCDate(getUTCTime(),mo.getTimestamp(key).getTime());
	} 
	catch (SQLException e) {
	    e.printStackTrace();
	}
	return def;
    }

    public InputStream getDBORaw(Object dbo, String key) {
	ResultSet mo = (ResultSet)dbo;
	try {
	    return mo.getBinaryStream(RAW);
	}
	catch (Throwable t) {
	    log.error(t.getMessage());
	    t.printStackTrace();
	    return null;
	}
    }

    @Override
    public void deleteAll() throws Exception {
	dropTables();
	setupTables();
    }

    @Override
    public long getMailCount(BlueboxMessage.State state) throws Exception {
	//		long start = getUTCTime().getTime();
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

	//		log.debug("Calculated mail count ({}) in {}ms", count, (getUTCTime().getTime()-start));
	return count;
    }

    @Override
    public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
	if ((inbox==null)||(inbox.getAddress().length()==0))
	    return getMailCount(state);
	//		String inbox = Utils.getEmail(email);
	//		long start = getUTCTime().getTime();
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

	//		log.debug("Calculated mail count for {} ({}) in {}ms",inbox,count,(getUTCTime().getTime()-start));
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
	if (email!=null) {
	    if (email.getAddress().length()==0) {
		email=null;
	    }
	}
	Statement s = connection.createStatement();
	PreparedStatement ps;

	if (email==null) {
	    if (state==State.ANY) {
		ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.HIDEME+"!=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
		ps.setBoolean(1, true);
	    }
	    else {
		ps = connection.prepareStatement("SELECT "+cols+" FROM "+INBOX_TABLE+" WHERE ("+BlueboxMessage.HIDEME+"!=?) AND ("+BlueboxMessage.STATE+"=?) ORDER BY "+orderBy+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
		ps.setBoolean(1, true);
		ps.setInt(2, state.ordinal());
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

    @Override
    public List<BlueboxMessage> listMail(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
	log.info("List mail called from start={}, count={}",start,count);
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

    @Override
    public List<LiteMessage> listMailLite(InboxAddress email, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
	List<LiteMessage> list = new ArrayList<LiteMessage>();
	if (count<=0) return list;
	Connection connection = getConnection();
	String cols = 	BlueboxMessage.UID+","+
		BlueboxMessage.RAWUID+","+
		BlueboxMessage.INBOX+","+
		BlueboxMessage.FROM+","+
		BlueboxMessage.SUBJECT+","+
		BlueboxMessage.RECIPIENT+","+
		BlueboxMessage.RECEIVED+","+
		BlueboxMessage.STATE+","+
		BlueboxMessage.SIZE;
	ResultSet result = listMailCommon(cols,connection, email, state, start, count, orderBy, ascending);

	while (result.next()) {
	    JSONObject message = loadMessageJSON(result);			
	    list.add(new LiteMessage(message));
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
	log.debug("Update mail entry "+uid+" to "+state);
    }

    private void clearProperty(String key) throws Exception {
	Connection connection = getConnection();
	PreparedStatement ps = connection.prepareStatement("DELETE FROM "+PROPS_TABLE+" WHERE "+KEY+"=?");
	ps.setString(1, key);
	ps.execute();
	connection.commit();
	connection.close();
	log.debug("Removed properties entry "+key);
    }

    public void setProperty(String key, String value) {
	final String randomStr = "DKSLDLKDFSURTJDSFIDFGHSJHGFSEYRIBVC";
	log.debug("------>{}={}",key,value);
	if (value.length()>512) {
	    value = value.substring(0,512);
	    log.error("Truncating data to fit 512 field");
	}
	try {
	    Connection connection = getConnection();
	    PreparedStatement ps;
	    if (getProperty(key,randomStr).equals(randomStr)) {
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
	    log.warn(t.getMessage());
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
	    log.warn(t.getMessage());
	}
	return value;
    }

    @Override
    public void logError(String title, InputStream content) {
	try {
	    logError(title,Utils.convertStreamToString(content));
	} 
	catch (Throwable e) {
	    logError(title,e.getMessage());
	}
    }

    @Override
    public void logError(String title, String content) {
	int count = logErrorCount();
	count++;
	setProperty(ERROR_TITLE+count,title);
	setProperty(ERROR_DATE+count,getUTCTime().toString());
	setProperty(ERROR_CONTENT+count,content);
	setProperty(ERROR_COUNT,Integer.toString(count));
    }

    @Override
    public int logErrorCount() {
	return Integer.parseInt(getProperty(ERROR_COUNT,"0"));
    }

    @Override
    public void logErrorClear() {
	int count = Integer.parseInt(getProperty(ERROR_COUNT,"0"));
	try {
	    for (int i = 1; i <= count; i++) {
		clearProperty(ERROR_TITLE+i);
		clearProperty(ERROR_DATE+i);
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	    log.error(e.getMessage());
	}
	setProperty(ERROR_COUNT,"0");
	//		logError("Error db cleared",Utils.convertStringToStream("Requested "+getUTCTime().toString()));
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
		String date = getProperty(ERROR_DATE+i,"");
		String id = i+"";
		logError = new JSONObject();
		logError.put("title", title);
		logError.put("date", date);
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
	long start = getUTCTime().getTime();

	JSONObject jo = new JSONObject();
	try {
	    jo.put(BlueboxMessage.COUNT, 0);
	    jo.put(Inbox.EMAIL,"");
	    jo.put(BlueboxMessage.RECIPIENT,"");

	    Connection connection = null;
	    try {
		connection = getConnection();
		try {
		    Statement s = connection.createStatement();
		    PreparedStatement ps;
		    ps = connection.prepareStatement("SELECT DISTINCT "+BlueboxMessage.RECIPIENT+","+BlueboxMessage.RECIPIENT+", COUNT(*)  FROM "+INBOX_TABLE+" GROUP BY "+BlueboxMessage.RECIPIENT+","+BlueboxMessage.RECIPIENT+" ORDER BY COUNT(*) DESC");
		    ps.execute();
		    ResultSet result = ps.getResultSet();

		    while (result.next()) {
			InboxAddress ia = new InboxAddress(result.getString(2));
			jo.put(Inbox.EMAIL,ia.getFullAddress());
			jo.put(BlueboxMessage.RECIPIENT,ia.getDisplayName());
			jo.put(BlueboxMessage.COUNT,result.getLong(3));
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
	log.debug("Calculated active inbox count in {}ms",(getUTCTime().getTime()-start));
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
			// could be multiple senders, so lets just take first one
			jo.put(BlueboxMessage.FROM,new JSONArray(result.getString(1)).get(0));
			jo.put(BlueboxMessage.COUNT,result.getLong(2));
			break; // list is already ordered, so first one is biggest
		    }
		    ps.close();
		    s.close();
		}
		catch (Throwable t) {
		    log.warn("Seems no stats are available");
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
    public JSONObject getCountByDay() {

	JSONObject resultJ = new JSONObject();
	try {
	    // init stats with empty values
	    for (int i = 1; i < 32; i++) {
		resultJ.put(i+"", 0);
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}

	String sql = "select day( "+StorageIf.Props.Received.name()+" ), count( "+StorageIf.Props.Uid.name()+" ) from "+INBOX_TABLE+" group by day( "+StorageIf.Props.Received.name()+" )";
	try {
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    PreparedStatement ps;
	    ps = connection.prepareStatement(sql);
	    ps.execute();
	    ResultSet result = ps.getResultSet();

	    while (result.next()) {
		resultJ.put(result.getString(1), result.getString(2));
	    }
	    ps.close();
	    s.close();
	    connection.close();
	}
	catch (Throwable t) {
	    t.printStackTrace();
	    log.warn("Seems no stats are available");
	}


	return resultJ;
    }

    @Override
    public JSONObject getCountByHour() {

	JSONObject resultJ = new JSONObject();
	try {
	    // init stats with empty values
	    for (int i = 0; i < 24; i++) {
		resultJ.put(i+"", 0);
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}

	String sql = "select hour( "+StorageIf.Props.Received.name()+" ), count( "+StorageIf.Props.Uid.name()+" ) from "+INBOX_TABLE+" group by hour( "+StorageIf.Props.Received.name()+" )";
	try {
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    PreparedStatement ps;
	    ps = connection.prepareStatement(sql);
	    ps.execute();
	    ResultSet result = ps.getResultSet();

	    while (result.next()) {
		resultJ.put(result.getString(1), result.getString(2));
	    }
	    ps.close();
	    s.close();
	    connection.close();
	}
	catch (Throwable t) {
	    t.printStackTrace();
	    log.warn("Seems no hourly stats are available");
	}


	return resultJ;
    }

    public void deleteFunction() throws Exception {
	//	String method = "drop alias app.dayOfWeek";	
	//	Connection connection = getConnection();
	//	try {
	//	    Statement s = connection.createStatement();
	//	    PreparedStatement ps;
	//	    ps = connection.prepareStatement(method);
	//	    ps.execute();
	//	    ResultSet result = ps.getResultSet();
	//	    log.info(result.toString());
	//	    result.close();
	//	    ps.close();
	//	    s.close();
	//	}
	//	catch (Throwable t) {
	//	    //t.printStackTrace();
	//	}
	//	finally {
	//	    connection.close();
	//	}
    }

    public void createFunction() throws Exception {
	//	String method = "create alias dayOfWeek( dateValue timestamp )\n"+ 
	//		"returns int\n"+ //"returns varchar( 8 )\n"+ 
	//		"parameter style java\n"+ 
	//		"no sql\n"+ 
	//		"language java\n"+ 
	//		"EXTERNAL NAME 'com.bluebox.smtp.storage.h2.H2Functions.dayOfWeek'\n";	
	//	Connection connection = getConnection();
	//	try {
	//	    Statement s = connection.createStatement();
	//	    PreparedStatement ps;
	//	    ps = connection.prepareStatement(method);
	//	    ps.execute();
	//	    ResultSet result = ps.getResultSet();
	//	    log.info(result.toString());
	//	    result.close();
	//	    ps.close();
	//	    s.close();
	//	}
	//	catch (Throwable t) {
	//	    log.error("Problem creating function",t);
	//	}
	//	finally {
	//	    connection.close();
	//	}
    }

    @Override
    public JSONObject getCountByDayOfWeek() {
	JSONObject resultJ = new JSONObject();
	try {
	    // init stats with empty values
	    for (int i = 1; i < 8; i++) {
		resultJ.put(i+"", 0);
	    }
	}
	catch (Throwable t) {
	    log.error("Problem getting count by Day Of Week",t);
	}

	//		String sql = "select "+DOW+", count("+StorageIf.Props.Uid.name()+") as cnt from "+INBOX_TABLE+" group by "+DOW+"";
	// 16.306 ; 14.216 ; 14.636
	// 15.184 ; 14.590 ; 15.449
	String sql = "select dow, count("+StorageIf.Props.Uid.name()+") from (select dayOfWeek("+StorageIf.Props.Received.name()+") as dow, "+StorageIf.Props.Uid.name()+" from "+INBOX_TABLE+") as t group by dow";
	try {
	    log.debug(sql);
	    Connection connection = getConnection();
	    Statement s = connection.createStatement();
	    PreparedStatement ps;
	    ps = connection.prepareStatement(sql);
	    ps.execute();
	    ResultSet result = ps.getResultSet();

	    while (result.next()) {
		resultJ.put(result.getString(1), result.getString(2));
	    }
	    ps.close();
	    s.close();
	    connection.close();
	}
	catch (Throwable t) {
	    log.error("Problem getting weekly stats",t);
	}


	return resultJ;
    }

    @Override
    public JSONObject getMPH(InboxAddress inbox) {
	Date started = getUTCTime();
	JSONObject resultJ = new JSONObject();

	// get mail count for last hour
	try {
	    String emailBit = "";
	    if ((inbox!=null)&&(inbox.getFullAddress().trim().length()>0)) {
		emailBit = " AND "+StorageIf.Props.Inbox.name()+"=? ";
	    }
	    Connection connection = getConnection();
	    Date lastHour = Utils.getUTCDate(getUTCTime(),getUTCTime().getTime()-60*60*1000);// one hour ago
	    //String sql = "select count ("+StorageIf.Props.Uid.name()+") from "+INBOX_TABLE+" where TIMESTAMP("+StorageIf.Props.Received.name()+") > TIMESTAMP('" + new Timestamp(lastHour.getTime()) + "')"+emailBit;
	    String sql = "select count ("+StorageIf.Props.Uid.name()+") from "+INBOX_TABLE+" where "+StorageIf.Props.Received.name()+" > '" + new Timestamp(lastHour.getTime()) + "'"+emailBit;
	    PreparedStatement ps;
	    ps = connection.prepareStatement(sql);
	    if ((inbox!=null)&&(inbox.getFullAddress().trim().length()>0)) {
		ps.setString(1, inbox.getAddress()); 
	    }
	    ps.execute();
	    ResultSet result = ps.getResultSet();

	    while (result.next()) {
		resultJ.put("mph", result.getInt(1));
	    }
	    ps.close();
	    connection.close();
	}
	catch (Throwable t) {
	    log.warn("Seems no mph stats are available",t);
	    try {
		resultJ.put("mph", 0);
	    } 
	    catch (JSONException e) {
		e.printStackTrace();
	    }
	}

	// get mail count average for last 24 hour
	//		try {
	//			Connection connection = getConnection();
	//			Date last24Hour = getUTCDate(getUTCTime().getTime()-24*60*60*1000);// 24 hours ago
	//			String sql = "select count ("+StorageIf.Props.Uid.name()+") from "+INBOX_TABLE+" where TIMESTAMP("+StorageIf.Props.Received+") > TIMESTAMP('" + new Timestamp(last24Hour.getTime()) + "')";
	//			PreparedStatement ps;
	//			ps = connection.prepareStatement(sql);
	//			ps.execute();
	//			ResultSet result = ps.getResultSet();
	//
	//			while (result.next()) {
	//				resultJ.put("mph24", result.getInt(1)/24);
	//			}
	//			ps.close();
	//			connection.close();
	//		}
	//		catch (Throwable t) {
	//			t.printStackTrace();
	//			log.warn("Seems no hourly stats are available");
	//		}
	log.debug("Got mph stats in {}ms",getUTCTime().getTime()-started.getTime());
	return resultJ;
    }

    @Override
    public WorkerThread cleanOrphans() throws Exception {
	WorkerThread wt = new WorkerThread(StorageIf.RAWCLEAN) {

	    @Override
	    public void run() {
		setStatus("Running");
		try {
		    int issues = 0;
		    long totalCount = getSpoolCount()+getMailCount(BlueboxMessage.State.ANY);
		    int count = 0;
		    // loop through all blobs
		    Connection connection = getConnection();
		    PreparedStatement ps = connection.prepareStatement("SELECT "+BlueboxMessage.UID+" FROM "+BLOB_TABLE);
		    ps.execute();
		    ResultSet result = ps.getResultSet();

		    while (result.next()) {
			String spooledId = result.getString(BlueboxMessage.UID);
			if (!spoolReferenced(connection,spooledId)) {
			    issues++;
			    removeSpooledStream(connection,spooledId);
			}
			setProgress((int)((100*count++)/totalCount)/2);
		    }
		    connection.close();

		    // loop through all the mails
		    List<LiteMessage> list = listMailLite(null, BlueboxMessage.State.ANY, 0, (int)getMailCount(BlueboxMessage.State.ANY), BlueboxMessage.RECEIVED, true);
		    for (LiteMessage m : list) {
			setProgress((int)((100*count++)/totalCount)/2);
			try {
			    if (!containsSpool(m.getRawIdentifier())) {
				// delete this mail entry
				delete(m.getIdentifier(),m.getRawIdentifier());
				issues++;
				setStatus("Deleting orphaned mail entry ("+issues+")");
				log.info("Deleting orphaned mail entry ({})",issues);
			    }
			}
			catch (Throwable t) {
			    log.warn("Issue with mail entry",t);
			    // delete it anyway
			    delete(m.getIdentifier(),m.getRawIdentifier());
			    issues++;
			    setStatus("Deleting orphaned mail entry ("+issues+")");
			    log.info("Deleting orphaned mail entry ({})",issues);
			}
		    }
		    setStatus("Completed");
		}
		catch (Throwable t) {
		    log.error("Error cleaning orphans",t);
		    setStatus("Error :"+t.getMessage());
		}
		finally {				
		    setProgress(100);
		}
	    }

	};
	return wt;
    }

    private boolean spoolReferenced(Connection connection,String spooledId) throws SQLException {
	PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) from "+INBOX_TABLE+" where "+BlueboxMessage.RAWUID+"=?");
	ps.setString(1, spooledId);
	ps.execute();
	ResultSet result = ps.getResultSet();
	long count = 0;
	if (result.next()) {
	    count = result.getLong(1);
	}
	result.close();
	return count>0;
    }

    public boolean containsSpool(String spoolUid) {
	boolean contains = false;
	try {
	    Connection connection = getConnection();
	    PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+BLOB_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
	    ps.setString(1, spoolUid);
	    ps.execute();
	    ResultSet result = ps.getResultSet();
	    if (result.next()) {
		contains = true;
	    }
	    connection.close();
	}
	catch (Throwable t) {
	    log.error("Problem checking for spool uid {} ({})",spoolUid,t.getMessage());
	}
	return contains;
    }

    @Override
    public String spoolStream(InputStream blob) throws Exception {
	//		log.info("Spool count is {} uid={}",getSpoolCount(),uid);
	Connection connection = getConnection();
	String uid;
	try {
	    MessageDigest md = MessageDigest.getInstance("MD5");
	    DigestInputStream dis = new DigestInputStream(blob, md);

	    PreparedStatement ps = connection.prepareStatement("INSERT INTO "+BLOB_TABLE+" VALUES (?, ?)");
	    ps.setString(1,uid = UUID.randomUUID().toString()); // UID
	    ps.setBinaryStream(2, dis); // MIMEMESSAGE
	    log.debug("Storing spooled entry using uid={}",uid);

	    ps.execute();
	    connection.commit();

	    // now get checksum
	    String newUid = Hex.encodeHexString(md.digest());
	    // if this digest already exists, delete the one we just added
	    if (containsSpool(newUid)) {
		log.debug("spool already exists, deleting new one {}",uid);
		removeSpooledStream(uid);
	    }
	    else {
		log.debug("renaming spool to {}",newUid);
		// rename uid to newUid
		ps = connection.prepareStatement("UPDATE "+BLOB_TABLE+" SET "+BlueboxMessage.UID+"=? WHERE "+BlueboxMessage.UID+"=?");
		ps.setString(1, newUid);
		ps.setString(2, uid);
		ps.execute();
		connection.commit();
	    }
	    uid = newUid;
	}
	catch (Throwable t) {
	    log.error("Error storing message :{}",t.getMessage());
	    return null;
	}
	finally {
	    connection.close();
	    blob.close();
	}
	return uid;
    }

    @Override
    public long getSpoolCount() throws Exception {
	long count = 0;
	Connection connection = getConnection();
	PreparedStatement ps;
	ps = connection.prepareStatement("SELECT COUNT(*) from "+BLOB_TABLE);
	ps.execute();
	ResultSet result = ps.getResultSet();
	if (result.next()) {
	    count = result.getLong(1);
	}
	result.close();
	ps.close();
	connection.close();

	return count;
    }

    @Override
    public MimeMessage getSpooledStream(String spooledUid) throws Exception {
	Connection connection = getConnection();
	MimeMessage msg = null;
	try {
	    PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+BLOB_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
	    ps.setString(1, spooledUid);
	    log.debug("Retrieving spooled entry using uid={}",spooledUid);
	    ps.execute();
	    ResultSet result = ps.getResultSet();
	    if (result.next()) {
		msg = Utils.loadEML(getDBORaw(ps.getResultSet(),getDBOString(ps.getResultSet(),BlueboxMessage.UID,UUID.randomUUID().toString())));
	    }
	}
	catch (Throwable t) {
	    log.error("Problem getting spooled stream",t);
	}
	finally {
	    connection.close();
	}
	return msg;
    }

    //	private InputStream getSpooledStream(Connection connection, String spooledUid) throws SQLException {
    //		PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+BLOB_TABLE+" WHERE "+BlueboxMessage.UID+"=?");
    //		ps.setString(1, spooledUid);
    //		log.debug("Retrieving spooled entry using uid={}",spooledUid);
    //		ps.execute();
    //		ResultSet result = ps.getResultSet();
    //		if (result.next()) {
    //			return getDBORaw(ps.getResultSet(),getDBOString(ps.getResultSet(),BlueboxMessage.UID,UUID.randomUUID().toString()));
    //		}
    //		return null;
    //	}

    public void removeSpooledStream(Connection connection, String spooledUid) throws Exception {
	log.debug("Removing spooled entry for uid={}",spooledUid);
	PreparedStatement ps = connection.prepareStatement("DELETE FROM "+BLOB_TABLE+" WHERE "+StorageIf.Props.Uid.name()+"=?");
	ps.setString(1, spooledUid);
	ps.execute();
	connection.commit();
	log.debug("Removed blob entry {}",spooledUid);
    }

    @Override
    public void removeSpooledStream(String spooledUid) throws Exception {
	log.debug("Removing spooled entry for uid={}",spooledUid);
	Connection connection = getConnection();
	try {
	    removeSpooledStream(connection,spooledUid);
	}
	finally {
	    connection.close();
	}
	log.debug("Removed blob entry {}",spooledUid);

    }

    @Override
    public long getSpooledStreamSize(String spooledUid) throws Exception {
	log.debug("Checking spool size of uid={}",spooledUid);
	Connection connection = getConnection();
	long size = 0;
	try {
	    PreparedStatement ps = connection.prepareStatement("SELECT * FROM "+BLOB_TABLE+" WHERE "+StorageIf.Props.Uid.name()+"=?");
	    ps.setString(1, spooledUid);
	    ps.execute();
	    ResultSet result = ps.getResultSet();
	    if (result.next()) {
		log.debug("Found spooled entry uid={}",spooledUid);
		Blob blob = result.getBlob(RAW);
		size = blob.length();
		blob.free();
	    }
	    //			InputStream is = getSpooledStream(spooledUid);
	    //			while(is.read()>=0)
	    //				size++;
	    //			is.close();
	}
	catch (Throwable t) {
	    log.error("Problem getting spooled stream size",t);
	}
	finally {
	    connection.close();			
	}
	log.debug("Size "+size);
	return size;
    }

    @Override
    public Date getUTCTime() {
	//		return new Date(getUTCCalendar().getTimeInMillis());

	final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
	final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
	sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	final String utcTime = sdf.format(new Date());

	Date dateToReturn = null;
	SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

	try {
	    dateToReturn = (Date)dateFormat.parse(utcTime);
	}
	catch (ParseException e) {
	    log.error("Problem getting utc time",e);
	}

	return dateToReturn;
    }

    @Override
    public Object[] search(String querystr, SearchFields fields, int start,	int count, SortFields orderBy, boolean ascending) throws Exception {

	querystr = querystr.toLowerCase();
	if (querystr=="*")
	    querystr = "";
	//		querystr = querystr.substring(0,querystr.length()-1);
	Connection connection = getConnection();
	PreparedStatement ps=null;

	String sortKey;
	switch (orderBy) {
	case SORT_RECEIVED : 
	    sortKey = BlueboxMessage.RECEIVED;
	    break;
	case SORT_SIZE : 
	    sortKey = BlueboxMessage.SIZE;
	    break;
	default :
	    sortKey = BlueboxMessage.RECEIVED;
	}
	String orderStr;
	if (ascending)
	    orderStr = " ASC";
	else
	    orderStr = " DESC";

	switch (fields) {
	case INBOX :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.INBOX+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) AND ("+BlueboxMessage.HIDEME+"!=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());
	    ps.setBoolean(3, false);
	    break;
	case SUBJECT :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.SUBJECT+") LIKE ? AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());
	    break;
	case TEXT_BODY :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.TEXT_BODY+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());
	    break;
	case HTML_BODY :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.HTML_BODY+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());
	    break;
	case BODY :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE ((LOWER("+BlueboxMessage.HTML_BODY+") LIKE LOWER(?) OR LOWER("+BlueboxMessage.TEXT_BODY+") LIKE LOWER(?)) AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setString(2, "%"+querystr+"%");
	    ps.setInt(3, BlueboxMessage.State.NORMAL.ordinal());
	    break;
	case FROM :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.FROM+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());		
	    break;
	case RECIPIENT :
	    //			ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.RECIPIENT+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps = connection.prepareStatement("SELECT DISTINCT "+getFields()+" FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.RECIPIENT+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) AND ("+BlueboxMessage.HIDEME+"!=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());	
	    ps.setBoolean(3, false);
	    break;
	case RECIPIENTS :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (LOWER("+BlueboxMessage.RECIPIENT+") LIKE LOWER(?) AND "+BlueboxMessage.STATE+"=?) AND ("+BlueboxMessage.HIDEME+"!=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setInt(2, BlueboxMessage.State.NORMAL.ordinal());					
	    ps.setBoolean(3, false);
	    break;
	case ANY :
	default :
	    ps = connection.prepareStatement("SELECT * FROM "+INBOX_TABLE+" WHERE (("+
		    "LOWER("+BlueboxMessage.INBOX+") LIKE LOWER(?) OR "+
		    "LOWER("+BlueboxMessage.FROM+") LIKE LOWER(?) OR "+
		    "LOWER("+BlueboxMessage.RECIPIENT+") LIKE LOWER(?) OR "+
		    "LOWER("+BlueboxMessage.HTML_BODY+") LIKE LOWER(?) OR "+
		    "LOWER("+BlueboxMessage.TEXT_BODY+") LIKE LOWER(?) OR "+
		    "LOWER("+BlueboxMessage.SUBJECT+") LIKE LOWER(?)"+
		    ") AND "+BlueboxMessage.STATE+"=?) ORDER BY "+sortKey+orderStr+" OFFSET "+start+" ROWS FETCH NEXT "+count+" ROWS ONLY");
	    ps.setString(1, "%"+querystr+"%");
	    ps.setString(2, "%"+querystr+"%");
	    ps.setString(3, "%"+querystr+"%");
	    ps.setString(4, "%"+querystr+"%");
	    ps.setString(5, "%"+querystr+"%");
	    ps.setString(6, "%"+querystr+"%");
	    ps.setInt(7, BlueboxMessage.State.NORMAL.ordinal());
	}
	ps.execute();
	ResultSet result = ps.getResultSet();
	List<Document> res = new ArrayList<Document>();
	while (result.next()) {
	    Document d = new Document();
	    d.put(BlueboxMessage.UID, result.getString(BlueboxMessage.UID));
	    d.put(BlueboxMessage.INBOX, result.getString(BlueboxMessage.INBOX));
	    d.put(BlueboxMessage.SUBJECT, result.getString(BlueboxMessage.SUBJECT));
	    d.put(BlueboxMessage.FROM, result.getString(BlueboxMessage.FROM));
	    d.put(BlueboxMessage.RECIPIENT, result.getString(BlueboxMessage.RECIPIENT));
	    d.put(BlueboxMessage.RECEIVED, result.getString(BlueboxMessage.RECEIVED));
	    d.put(BlueboxMessage.SIZE, result.getString(BlueboxMessage.SIZE));
	    res.add(d);
	}
	result.close();
	connection.close();
	return res.toArray();
    }

    private String getFields() {
	return 
		StorageIf.Props.Inbox.name()+", "+
		StorageIf.Props.Uid.name()+", "+
		StorageIf.Props.RawUid.name()+", "+
		StorageIf.Props.Recipient.name()+", "+
		StorageIf.Props.Sender.name()+", "+
		StorageIf.Props.Subject.name()+", "+
		StorageIf.Props.Received.name()+", "+
		StorageIf.Props.State.name()+", "+
		StorageIf.Props.Size.name();
    }

    @Override
    public List<String> getDBOArray(Object dbo, String key) {
	List<String> l = new ArrayList<String>();
	try {
	    JSONArray j = new JSONArray(getDBOString(dbo, key, "[]"));
	    for (int i = 0; i < j.length();i++)
		l.add(j.get(i).toString());
	} 
	catch (JSONException e) {
	    log.error("Problem getting dbo array",e);
	}
	return l;
    }

}
