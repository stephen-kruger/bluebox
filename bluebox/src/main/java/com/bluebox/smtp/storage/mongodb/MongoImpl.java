package com.bluebox.smtp.storage.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageIf;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MongoImpl extends AbstractStorage implements StorageIf {
	private static final Logger log = LoggerFactory.getLogger(MongoImpl.class);
	private static final String DB_ERR_NAME = "bluebox_errors";
	private static final String TABLE_NAME = "inbox";
	private static final String BLOB_NAME = "blob";
	private static final String PROPS_TABLE_NAME = "properties";
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> errorFS, propsFS, mailFS;
	private GridFS blobFS;

	@Override
	public void start() throws Exception {
		log.info("Starting MongoDB connection");
		mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
		db = mongoClient.getDatabase(DB_NAME);
		mailFS = db.getCollection(TABLE_NAME);
		createIndexes();
		errorFS = db.getCollection(DB_ERR_NAME);
		propsFS = db.getCollection(PROPS_TABLE_NAME);
		blobFS = new GridFS(mongoClient.getDB(BLOB_NAME),BLOB_NAME);

		log.debug("Started MongoDB connection");

	}

	@Override
	public void stop() throws Exception {
		log.info("Stopping MongoDB connection");
		mongoClient.close();
	}

	private void createIndexes() {
		for (StorageIf.Props index : StorageIf.Props.values()) {
			try {
				db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(index.name(), 1));
			}
			catch (Throwable t) {
				log.error(t.getMessage());
			}
			try {
				db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(index.name(), -1));
			}
			catch (Throwable t) {
				log.error(t.getMessage());
			}
		}  
	}
	
	@Override
	public void store(JSONObject props, String spooledUid) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void store(JSONObject props, InputStream content) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public BlueboxMessage retrieve(String uid) throws Exception {
		Document doc = mailFS.find(Filters.eq(StorageIf.Props.Uid.name(), uid)).first();
		return loadMessage(doc);
	}

	@Override
	public boolean contains(String uid) {
		return mailFS.count(Filters.eq(StorageIf.Props.Uid.name(), uid))>0;
	}

	@Override
	public void deleteAll(InboxAddress inbox) throws Exception {
		mailFS.deleteMany(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()));
	}

	@Override
	public void deleteAll() throws Exception {
		mailFS.drop();
	}

	@Override
	public long getMailCount(State state) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMailCount(InboxAddress inbox, State state) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<BlueboxMessage> listMail(InboxAddress inbox, State state,
			int start, int count, String orderBy, boolean ascending)
					throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setState(String uid, State state) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void logError(String title, InputStream content) {
		try {
			logError(title,IOUtils.toString(content));		
		}
		catch (IOException ioe) {
			log.error("Error saving log",ioe);
		}
	}

	@Override
	public void logError(String title, String content) {
		Document document = new Document("title",title);
		document.put("date", getUTCTime());
		document.put("uid", UUID.randomUUID().toString());
		document.put("content", content);
		errorFS.insertOne(document );
	}

	@Override
	public int logErrorCount() {
		return (int) errorFS.count();
	}

	@Override
	public void logErrorClear() {
		errorFS.drop();		
	}

	@Override
	public JSONArray logErrorList(int start, int count) {
		FindIterable<Document> errors = errorFS.find(Filters.exists("uid"));

		try {
			JSONArray result = new JSONArray();
			JSONObject logError;
			for (Document error : errors) {
				logError = new JSONObject();
				logError.put("title", error.getString("title"));
				logError.put("date", error.getLong("date"));
				logError.put("id", error.getString("uid"));
				logError.put("id", error.getString("uid"));
				result.put(logError);
			}
			return result;
		}
		catch (Throwable t) {
			log.error("Problem getting errors",t);
			t.printStackTrace();
		}
		return null;
	}

	@Override
	public String logErrorContent(String uid) {
		return errorFS.find(Filters.eq("uid", uid)).first().getString("content");
	}

	@Override
	public JSONObject getMostActiveInbox() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getMostActiveSender() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String uid) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public WorkerThread cleanRaw() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getCountByDay() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getCountByHour() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getCountByDayOfWeek() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject getMPH(InboxAddress inbox) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperty(String key, String value) {
		Document document = new Document(key,value);
		propsFS.insertOne(document );
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		FindIterable<Document> result = propsFS.find(Filters.exists(key));
		if (result.first()!=null) {
			return result.first().getString(key);
		}
		return defaultValue;
	}

	@Override
	public String spoolStream(InputStream blob) throws Exception {
		log.info("Spool count is {}",getSpoolCount());
		try {
			GridFSInputFile gfs = blobFS.createFile(blob);
			gfs.save();
			log.debug("Saved blob with uid {}",gfs.getId());
			return gfs.getId().toString();
		}
		catch (Throwable t) {
			log.error("Error storing blob :{}",t.getMessage());
		}
		finally {
			blob.close();
		}
		return null;
	}

	@Override
	public MimeMessage getSpooledStream(String spooledUid) throws Exception {
		GridFSDBFile blob = blobFS.findOne(new ObjectId(spooledUid));
		MimeMessage msg = Utils.loadEML(blob.getInputStream());
		return msg;
	}

	public InputStream getSpooledInputStream(String spooledUid) throws Exception {
		GridFSDBFile blob = blobFS.findOne(new ObjectId(spooledUid));
		return blob.getInputStream();
	}

	@Override
	public void removeSpooledStream(String spooledUid) throws Exception {
		blobFS.remove(new ObjectId(spooledUid));		
	}

	@Override
	public long getSpooledStreamSize(String spooledUid) {
		GridFSDBFile blob = blobFS.findOne(new ObjectId(spooledUid));
		return blob.getLength();
	}

	@Override
	public long getSpoolCount() throws Exception {
		return blobFS.getFileList().count();
	}

	@Override
	public Date getUTCTime() {
		return Utils.getUTCCalendar().getTime();
	}

	@Override
	public List<LiteMessage> listMailLite(InboxAddress inbox, State state,
			int start, int count, String orderBy, boolean ascending)
					throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDBOString(Object dbo, String key, String def) {
		Document doc = (Document)dbo;
		if (doc.containsKey(key))
			return doc.getString(key);
		return def;
	}

	@Override
	public int getDBOInt(Object dbo, String key, int def) {
		Document doc = (Document)dbo;
		return doc.getInteger(key, def);
	}

	@Override
	public long getDBOLong(Object dbo, String key, long def) {
		Document doc = (Document)dbo;
		if (doc.containsKey(key))
			return doc.getLong(key);
		return def;
	}

	@Override
	public Date getDBODate(Object dbo, String key, Date def) {
		Document doc = (Document)dbo;
		if (doc.containsKey(key))
			return doc.getDate(key);
		return def;
	}

	@Override
	public InputStream getDBORaw(Object dbo, String key) {
		// TODO Auto-generated method stub
		return null;
	}



}
