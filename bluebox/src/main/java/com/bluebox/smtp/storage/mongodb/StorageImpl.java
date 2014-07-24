package com.bluebox.smtp.storage.mongodb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.util.JSON;

public class StorageImpl extends AbstractStorage implements StorageIf {
	// 1224264 
	private static final Logger log = Logger.getAnonymousLogger();
	private static final String DB_NAME = "bluebox";
	private static final String DB_ERR_NAME = "bluebox_errors";
	private static final String TABLE_NAME = "inbox";
	private static final String PROPS_TABLE_NAME = "properties";
	private DB db;
	private GridFS errorFS;

	public void start() throws Exception {
		log.info("Starting MongoDB connection");
		MongoClient mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
		db = mongoClient.getDB(DB_NAME);
		errorFS = new GridFS(mongoClient.getDB(DB_ERR_NAME),DB_ERR_NAME);

		createIndexes();

		log.fine("Started MongoDB connection");
	}

	private void createIndexes() {
		// create indexes
		String[] indexes = new String[]{
				BlueboxMessage.UID,BlueboxMessage.INBOX,BlueboxMessage.SUBJECT, 
				BlueboxMessage.FROM,BlueboxMessage.RECEIVED,BlueboxMessage.SIZE,BlueboxMessage.STATE};
		for (int i = 0; i < indexes.length;i++) {
			try {
				db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(indexes[i], 1));
			}
			catch (Throwable t) {
				log.severe(t.getMessage());
			}
			try {
				db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(indexes[i], -1));
			}
			catch (Throwable t) {
				log.severe(t.getMessage());
			}
		}  
	}

	public void stop() throws Exception {
		StorageFactory.clearInstance();
	}

	public BlueboxMessage store(InboxAddress inbox, String from, MimeMessage bbmm) throws Exception {
		BlueboxMessage message = new BlueboxMessage(UUID.randomUUID().toString());
		message.setInbox(inbox);
		message.setBlueBoxMimeMessage(from, bbmm);
		DBCollection coll = db.getCollection(TABLE_NAME);
		DBObject bson = ( DBObject ) JSON.parse( message.toJSON(true) );
		bson.put(BlueboxMessage.RAW, Utils.convertStreamToString(message.getRawMessage()));
		BasicDBObject doc = new BasicDBObject();
		doc.putAll(bson);
		coll.insert(doc);
		return message;
	}

	//	public MessageImpl store(InboxAddress recipient, String from, RawMessage rawMessage)throws Exception {
	//		MimeMessageWrapper message = rawMessage.getMimeMessage();
	//		return store(recipient,from,message);
	//	}

	public synchronized BlueboxMessage retrieve(String uid) throws Exception {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, uid);
		DBObject dbo = db.getCollection(TABLE_NAME).findOne(query);

		return loadMessage(dbo);
	}

	@Override
	public String getDBOString(Object dbo, String key, String def) {
		DBObject mo = (DBObject)dbo;
		if (mo.containsField(key)) {
			Object o = mo.get(key);
			if (o instanceof BasicDBList) {
				BasicDBList list = (BasicDBList)mo.get(key);
				if (list.size()>0)
					return list.get(0).toString();
				else
					return def;
			}
			return o.toString();
		}
		else {
			log.warning("Missing field "+key);
			return def;
		}
	}

	@Override
	public int getDBOInt(Object dbo, String key, int def) {
		DBObject mo = (DBObject)dbo;
		if (mo.containsField(key)) {
			return Integer.parseInt(mo.get(key).toString());
		}
		else {
			log.warning("Missing field "+key);
			return def;
		}
	}

	@Override
	public long getDBOLong(Object dbo, String key, long def) {
		DBObject mo = (DBObject)dbo;
		if (mo.containsField(key)) {
			return Long.parseLong(mo.get(key).toString());
		}
		else {
			log.warning("Missing field "+key);
			return def;
		}
	}


	public Date getDBODate(Object dbo, String key) {
		BasicDBObject mo = (BasicDBObject)dbo;
		return new Date(mo.getLong(key));
	}

	public InputStream getDBORaw(Object dbo, String key) {
		DBObject mo = (DBObject)dbo;
		try {
			return new ByteArrayInputStream( mo.get(key).toString().getBytes("UTF-8") );
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
			return null;
		}
	}

	//	public BlueboxMessage loadMessage(DBObject dbo) throws Exception {
	//		//		MimeMessageWrapper mmwDbo = new MimeMessageWrapper(null, new ByteArrayInputStream(dbo.get(MessageImpl.BINARY_CONTENT).toString().getBytes("UTF-8")));
	//		String uid = getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString());
	//		BlueboxMessage message = new BlueboxMessage(uid);
	//		if (dbo.containsField(BlueboxMessage.TO))
	//			message.setProperty(BlueboxMessage.TO,getDBOString(dbo,BlueboxMessage.TO,"bluebox@bluebox.com"));
	//		else {
	//			log.warning("Missing field "+BlueboxMessage.TO);
	//		}
	//		if (dbo.containsField(BlueboxMessage.AUTO_COMPLETE))
	//			message.setProperty(BlueboxMessage.AUTO_COMPLETE,getDBOString(dbo,BlueboxMessage.AUTO_COMPLETE,"bluebox@bluebox.com"));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.AUTO_COMPLETE);
	//
	//		if (dbo.containsField(BlueboxMessage.FROM))
	//			message.setProperty(BlueboxMessage.FROM,getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com"));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.FROM);
	//
	//		if (dbo.containsField(BlueboxMessage.SUBJECT))
	//			message.setProperty(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.SUBJECT);
	//
	//		if (dbo.containsField(BlueboxMessage.RECEIVED))
	//			message.setProperty(BlueboxMessage.RECEIVED,getDBOString(dbo,BlueboxMessage.RECEIVED,""));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.RECEIVED);
	//
	//		if (dbo.containsField(BlueboxMessage.STATE))
	//			message.setProperty(BlueboxMessage.STATE,getDBOString(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.name()));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.STATE);
	//
	//		if (dbo.containsField(BlueboxMessage.INBOX))
	//			message.setProperty(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.INBOX);
	//
	//		if (dbo.containsField(BlueboxMessage.SIZE))
	//			message.setProperty(BlueboxMessage.SIZE,getDBOString(dbo,BlueboxMessage.SIZE,"0"));
	//		else
	//			log.warning("Missing field "+BlueboxMessage.SIZE);
	//		//		if (dbo.containsField(MessageImpl.BINARY_CONTENT))
	//		//			message.setProperty(MessageImpl.BINARY_CONTENT,dbo.get(MessageImpl.BINARY_CONTENT).toString());
	//		//		else
	//		//			log.warning("Missing field "+MessageImpl.BINARY_CONTENT);
	//		message.loadBlueBoxMimeMessage(new MimeMessageWrapper(null, new ByteArrayInputStream(dbo.get(BlueboxMessage.RAW).toString().getBytes("UTF-8"))));
	//		return message;
	//	}

	@Override
	public void delete(String uid) {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, uid);
		db.getCollection(TABLE_NAME).remove(query);		
	}

	public void deleteAll(InboxAddress inbox) throws Exception {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.INBOX, inbox.getAddress());
		db.getCollection(TABLE_NAME).remove(query);	
	}

	@Override
	public void deleteAll() throws Exception {
		log.fine("Deleting all inboxes");
		if (db!=null) {
			DBCollection coll = db.getCollection(TABLE_NAME);
			if (coll!=null)
				coll.drop();
			DBCollection props = db.getCollection(PROPS_TABLE_NAME);
			if (props!=null)
				props.drop();
		}
		else {
			log.severe("Cannot delete from closed inbox");
		}
	}

	@Override
	public long getMailCount(BlueboxMessage.State state) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.name());
		return db.getCollection(TABLE_NAME).count(query);
	}

	@Override
	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		if ((inbox==null)||(inbox.getAddress().length()==0))
			return getMailCount(state);
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.name());
		if (inbox!=null)
			query.append(BlueboxMessage.INBOX, inbox.getAddress());
		long start = new Date().getTime();
		long count = db.getCollection(TABLE_NAME).count(query);
		log.fine("Calculated mail count in "+(new Date().getTime()-start)+"ms");
		return count;
	}

	public DBCursor listMailCommon(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.name());
		if ((inbox!=null)&&(inbox.getFullAddress().length()>0))
			query.append(BlueboxMessage.INBOX, inbox.getAddress());
		int sortBit;
		if (ascending) sortBit = 1; else sortBit = -1;
		if (count<0)
			count = 500;//Integer.MAX_VALUE; else we get "com.mongodb.MongoException: too much data for sort() with no index.  add an index or specify a smaller limit"
		return db.getCollection(TABLE_NAME).find(query).sort( new BasicDBObject( orderBy , sortBit )).skip(start).limit(count);
	}

	public List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		List<BlueboxMessage> results = new ArrayList<BlueboxMessage>();
		DBCursor cursor = this.listMailCommon(inbox, state, start, count, orderBy, ascending);
		try {
			while (cursor.hasNext()) {
				DBObject dbo = cursor.next();
				BlueboxMessage m = loadMessage(dbo);
				results.add(m);
			}
		} 
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		return results;
	}

	@Override
	public List<JSONObject> listMailLite(InboxAddress inbox, State state, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {

		List<JSONObject> results = new ArrayList<JSONObject>();
		DBCursor cursor = this.listMailCommon(inbox, state, start, count, orderBy, ascending);
		try {
			while (cursor.hasNext()) {
				DBObject dbo = cursor.next();
				JSONObject m = loadMessageJSON(dbo,locale);
				results.add(m);
			}
		} 
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		return results;
	}

	@Override
	public void setState(String uid, BlueboxMessage.State state) throws Exception {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, uid);
		DBCursor cursor = db.getCollection(TABLE_NAME).find(query);
		if (cursor.hasNext()) {
			DBObject dbo = cursor.next();
			dbo.put(BlueboxMessage.STATE, state.name());
			db.getCollection(TABLE_NAME).update(query, dbo);
		}
		cursor.close();
	}

	@Override
	public void setProperty(String key, String value) {
		//not allowed to have '.'s in field names
		key = key.replace('.', 'x');
		DBCollection coll = db.getCollection(PROPS_TABLE_NAME);		
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.put("name", key); 
		// your update condition
		DBCursor cursor = coll.find(dbObject);
		try {
			if (cursor.count()>0) {
				DBObject dbo = cursor.next();
				DBObject newObject =  dbo;
				newObject.put(key,value);			
				//add field, either a new field or any existing field
				coll.findAndModify(dbObject, newObject);
			}
			else {
				coll.insert(dbObject.append(key, value));
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		//not allowed to have '.'s in field names
		key = key.replace('.', 'x');
		@SuppressWarnings("unchecked")
		List<String> keys = db.getCollection(PROPS_TABLE_NAME).distinct(key);
		if (keys.size()>0)
			return keys.get(0);
		else		
			return defaultValue;
	}

	@Override
	public boolean hasProperty(String key) {
		String r = Long.toString(new Random().nextLong());
		return !getProperty(key,r).equals(r);		
	}

	@Override
	public void logError(String title, InputStream content) {
		try {
			GridFSInputFile gfs = errorFS.createFile(content);
			gfs.put("title", title);
			gfs.save();
			log.info("Saved with id "+gfs.getId());
			content.close();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public int logErrorCount() {
		try {
			// retrieve GridFS object "smithco"
			DBCursor cursor = errorFS.getFileList();
			int count = cursor.count();
			cursor.close();
			return count;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return 0;
	}

	public void logErrorClear() {
		try {
			log.info("Clearing error db");
			errorFS.getDB().dropDatabase();
			logError("Error db cleared",Utils.convertStringToStream("Requested "+new Date().toString()));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public JSONArray logErrorList(int start, int count) {
		try {
			DBCursor cursor = errorFS.getFileList();
			cursor.skip(start);
			JSONArray result = new JSONArray();
			JSONObject logError;
			while ((result.length()<count&&cursor.hasNext())) {
				DBObject dbo = cursor.next();
				//log.info(dbo.toString());;
				logError = new JSONObject();
				logError.put("title", dbo.get("title"));
				logError.put("id", dbo.get("_id"));
				result.put(logError);
			}
			cursor.close();
			return result;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	@Override
	public String logErrorContent(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			GridFSDBFile file = errorFS.findOne(oid);	
			//log.info("Found file length="+file.getLength()+" type="+file.getContentType());
			return Utils.convertStreamToString(file.getInputStream());
		}
		catch (Throwable t) {
			t.printStackTrace();
			return t.getMessage();
		}
	}

	@Override
	public JSONObject getMostActive() {
		//TODO - optimise this using Mongo aggregate functionality
		JSONObject jo = new JSONObject();
		try {
			jo.put(BlueboxMessage.INBOX,"");
			jo.put(BlueboxMessage.COUNT,0);
			@SuppressWarnings("unchecked")
			List<String> inboxes = db.getCollection(TABLE_NAME).distinct(BlueboxMessage.INBOX);
			long currcount, maxcount = 0;
			for (String inbox : inboxes) {
				currcount = this.getMailCount(new InboxAddress(inbox), BlueboxMessage.State.NORMAL);
				if (currcount>maxcount) {
					jo.put(BlueboxMessage.INBOX,inbox);
					jo.put(BlueboxMessage.COUNT,currcount);
					maxcount = currcount;
				}
			}
		}
		catch (Throwable je) {
			je.printStackTrace();
		}
		return jo;
	}

	//	@SuppressWarnings("unchecked")
	//	@Override
	//	public List<String> listUniqueInboxes() {
	//		List<String> inboxes = db.getCollection(TABLE_NAME).distinct(BlueboxMessage.INBOX);
	//		return inboxes;
	//	}

	@Override
	public void runMaintenance() throws Exception {
		List<DBObject> indexes = db.getCollection(TABLE_NAME).getIndexInfo();
		for (DBObject index : indexes) {
			db.getCollection(TABLE_NAME).remove(index);
		}
		createIndexes();
	}





}
