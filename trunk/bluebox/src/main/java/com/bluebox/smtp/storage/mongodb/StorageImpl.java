package com.bluebox.smtp.storage.mongodb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import javax.mail.internet.MimeMessage;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.StorageIf;
import com.mongodb.AggregationOutput;
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
	private static final Logger log = Logger.getAnonymousLogger();
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

		migrate();

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
		DBObject bson = ( DBObject ) JSON.parse( message.toJSON() );

		//		bson.put(BlueboxMessage.RAW, Utils.convertStreamToString(Utils.streamMimeMessage(bbmm)));
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		GridFSInputFile gfsFile = gfsRaw.createFile(Utils.streamMimeMessage(bbmm));
		gfsFile.setFilename(message.getIdentifier());
		gfsFile.save();
		//		bson.put(BlueboxMessage.RAW, new BasicBSONDecoder().readObject(Utils.streamMimeMessage(bbmm)));
		coll.insert(bson);
		return message;
	}

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

	public InputStream getDBORaw(Object dbo, String uid) {
		try {
			//return new ByteArrayInputStream( mo.get(key).toString().getBytes("UTF-8") );
			GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
			GridFSDBFile imageForOutput = gfsRaw.findOne(uid);
			return imageForOutput.getInputStream();
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
			return null;
		}
	}

	@Override
	public void delete(String uid) {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, uid);
		db.getCollection(TABLE_NAME).remove(query);	
		// remove the RAW blob too
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		gfsRaw.remove(gfsRaw.findOne(uid));
	}

	public void deleteAll(InboxAddress inbox) throws Exception {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.INBOX, inbox.getAddress());
		db.getCollection(TABLE_NAME).remove(query);
		cleanRaw();
	}

	private void cleanRaw() {
		log.info("Looking for orphaned blobs");
		// clean up any blobs who have no associated inbox message
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		DBCursor cursor = gfsRaw.getFileList();
		DBObject dbo;
		while(cursor.hasNext()) {
			dbo = cursor.next();
			BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, dbo.get("filename").toString());
			if (db.getCollection(TABLE_NAME).findOne(query)==null) {
				log.info("Removing orphaned blob "+dbo.get("filename"));
				gfsRaw.remove(dbo);
			}
		}
		cursor.close();
		log.info("Finished looking for orphaned blobs");
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
		// remove all blobs
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		DBCursor cursor = gfsRaw.getFileList();
		DBObject dbo;
		while(cursor.hasNext()) {
			dbo = cursor.next();
			log.fine("Deleting raw "+dbo.get("filename"));
			gfsRaw.remove(dbo);
		}
		cursor.close();
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
	public JSONObject getMostActiveInbox() {
		JSONObject jo = new JSONObject();
		try {
			jo.put(BlueboxMessage.INBOX,"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		DBObject sum = new BasicDBObject();sum.put("$sum", 1);
		DBObject group = new BasicDBObject();
		group.put("_id", "$"+BlueboxMessage.INBOX);
		group.put(BlueboxMessage.COUNT, sum);
		DBObject all = new BasicDBObject();
		all.put("$group", group);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, -1));
		List<DBObject> pipeline = Arrays.asList(all, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);

		for (DBObject result : output.results()) {
			try {
				jo.put(BlueboxMessage.INBOX,result.get("_id"));
				jo.put(BlueboxMessage.COUNT,result.get(BlueboxMessage.COUNT));
				break;// only care about first result
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// for later Mongodb use Cursor 
		//				AggregationOptions aggregationOptions = AggregationOptions.builder()
		//						.batchSize(100)
		//						.outputMode(AggregationOptions.OutputMode.CURSOR)
		//						.allowDiskUse(true)
		//						.build();
		//				Cursor cursor = db.getCollection(TABLE_NAME).aggregate(pipeline, aggregationOptions);
		//				while (cursor.hasNext()) {
		//				    System.out.println(cursor.next());
		//				}
		return jo;
	}

	@Override
	public JSONObject getMostActiveSender() {
		JSONObject jo = new JSONObject();
		try {
			jo.put(BlueboxMessage.INBOX,"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		DBObject sum = new BasicDBObject();sum.put("$sum", 1);
		DBObject group = new BasicDBObject();
		group.put("_id", "$"+BlueboxMessage.FROM);
		group.put(BlueboxMessage.COUNT, sum);
		DBObject all = new BasicDBObject();
		all.put("$group", group);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, -1));
		List<DBObject> pipeline = Arrays.asList(all, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);

		for (DBObject result : output.results()) {
			try {
				jo.put(BlueboxMessage.FROM,new JSONArray(result.get("_id").toString()).get(0));
				jo.put(BlueboxMessage.COUNT,result.get(BlueboxMessage.COUNT));
				break;// only care about first result
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}

		// for later Mongodb use Cursor 
		//				AggregationOptions aggregationOptions = AggregationOptions.builder()
		//						.batchSize(100)
		//						.outputMode(AggregationOptions.OutputMode.CURSOR)
		//						.allowDiskUse(true)
		//						.build();
		//				Cursor cursor = db.getCollection(TABLE_NAME).aggregate(pipeline, aggregationOptions);
		//				while (cursor.hasNext()) {
		//				    System.out.println(cursor.next());
		//				}
		return jo;
	}

	//	public JSONObject getMostActiveOld() {
	//		JSONObject jo = new JSONObject();
	//		try {
	//			jo.put(BlueboxMessage.INBOX,"");
	//			jo.put(BlueboxMessage.COUNT,0);
	//			@SuppressWarnings("unchecked")
	//			List<String> inboxes = db.getCollection(TABLE_NAME).distinct(BlueboxMessage.INBOX);
	//			long currcount, maxcount = 0;
	//			for (String inbox : inboxes) {
	//				currcount = getMailCount(new InboxAddress(inbox), BlueboxMessage.State.NORMAL);
	//				if (currcount>maxcount) {
	//					jo.put(BlueboxMessage.INBOX,inbox);
	//					jo.put(BlueboxMessage.COUNT,currcount);
	//					maxcount = currcount;
	//				}
	//			}
	//		}
	//		catch (Throwable je) {
	//			je.printStackTrace();
	//		}
	//		return jo;
	//	}

	//	@SuppressWarnings("unchecked")
	//	@Override
	//	public List<String> listUniqueInboxes() {
	//		List<String> inboxes = db.getCollection(TABLE_NAME).distinct(BlueboxMessage.INBOX);
	//		return inboxes;
	//	}

	@Override
	public void runMaintenance() throws Exception {
		cleanRaw();
	}

	@Override
	public void migrate(String version) {
		try {
			MongoClient mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
			if (version.equals("bluebox400")) {
				log.info("Migrating db version "+version+" to "+DB_NAME);
				DB oldDb = mongoClient.getDB(version);
				DBCollection coll = oldDb.getCollection(TABLE_NAME);
				DBCursor cursor = coll.find();
				DBObject msg;
				while (cursor.hasNext()) {
					msg = cursor.next();
					try {
						if (msg.containsField(BlueboxMessage.RAW)) {
							MimeMessage mimeMessage = Utils.loadEML(new ByteArrayInputStream( msg.get(BlueboxMessage.RAW).toString().getBytes("UTF-8") ));
							store(new InboxAddress(getDBOString(msg,BlueboxMessage.INBOX,"")), 
									getDBOString(msg,BlueboxMessage.FROM,""), 
									mimeMessage);
						}
					}
					catch (Throwable t) {
						log.severe("Error migrating message :"+t.getMessage());
						t.printStackTrace();
					}
				}

				cursor.close();
			}
			mongoClient.close();
			log.info("Migration complete");
		}
		catch (Throwable t) {
			t.printStackTrace();
			log.severe(t.getMessage());
		}
	}





}
