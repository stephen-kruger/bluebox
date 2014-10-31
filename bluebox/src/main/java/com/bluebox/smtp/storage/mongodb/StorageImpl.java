package com.bluebox.smtp.storage.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;
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
	private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);
	private static final String DB_ERR_NAME = "bluebox_errors";
	private static final String TABLE_NAME = "inbox";
	private static final String PROPS_TABLE_NAME = "properties";
	private DB db;
	private GridFS errorFS;

	public void start() throws Exception {
		log.info("Starting MongoDB connection");
		//		MongoClientOptions options = MongoClientOptions.builder()
		//                .maxWaitTime(250)
		//                .connectTimeout(250)
		//                .socketTimeout(250)
		//                .build();
		MongoClient mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
		db = mongoClient.getDB(DB_NAME);
		errorFS = new GridFS(mongoClient.getDB(DB_ERR_NAME),DB_ERR_NAME);

		createIndexes();

		log.debug("Started MongoDB connection");
	}

	public static boolean mongoDetected() {
		try {
			Socket socket = new Socket(); // Unconnected socket, with the  system-default type of SocketImpl.
			InetSocketAddress endPoint = new InetSocketAddress( Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST),27017);
			socket.connect(  endPoint , 100);
			socket.close();
			return true;
		}
		catch (Throwable t) {

		}
		return false;
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
				log.error(t.getMessage());
			}
			try {
				db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(indexes[i], -1));
			}
			catch (Throwable t) {
				log.error(t.getMessage());
			}
		}  
	}

	public void stop() throws Exception {
		StorageFactory.clearInstance();
	}

	public void store(JSONObject props, InputStream blob) throws Exception {
		DBCollection coll = db.getCollection(TABLE_NAME);
		DBObject bson = ( DBObject ) JSON.parse( props.toString() );
		Date d = new Date(props.getLong(StorageIf.Props.Received.name()));
		bson.put(StorageIf.Props.Received.name(), d);
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		GridFSInputFile gfsFile = gfsRaw.createFile(blob);
		gfsFile.setFilename(props.getString(StorageIf.Props.Uid.name()));
		gfsFile.save();
		coll.insert(bson);
		blob.close();
	}

	//	public BlueboxMessage store(String from, InboxAddress recipient, Date received, MimeMessage bbmm) throws Exception {
	//		BlueboxMessage message = new BlueboxMessage(UUID.randomUUID().toString());
	//		message.setInbox(recipient);
	//		message.setBlueBoxMimeMessage(from, recipient, received, bbmm);
	//		DBCollection coll = db.getCollection(TABLE_NAME);
	//		DBObject bson = ( DBObject ) JSON.parse( message.toJSON().toString() );
	//
	//		//		bson.put(BlueboxMessage.RAW, Utils.convertStreamToString(Utils.streamMimeMessage(bbmm)));
	//		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
	//		GridFSInputFile gfsFile = gfsRaw.createFile(Utils.streamMimeMessage(bbmm));
	//		gfsFile.setFilename(message.getIdentifier());
	//		gfsFile.save();
	//		//		bson.put(BlueboxMessage.RAW, new BasicBSONDecoder().readObject(Utils.streamMimeMessage(bbmm)));
	//		coll.insert(bson);
	//		return message;
	//	}

	public synchronized BlueboxMessage retrieve(String uid) throws Exception {
		BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, uid);
		log.debug("Looking for uid {}",uid);
		DBObject dbo = db.getCollection(TABLE_NAME).findOne(query);
		if (dbo==null) {
			log.error("Trying to retrieve non-existent uid {}",uid);
			throw new Exception("Trying to retrieve non-existent uid "+uid);
		}
		return loadMessage(dbo);
	}

	@Override
	public String getDBOString(Object dbo, String key, String def) {
		DBObject mo = (DBObject)dbo;
		if (mo.containsField(key)) {
			Object o = mo.get(key);
			// if it's a JSONArray, return a string rep of the entire array
			if (o instanceof BasicDBList) {
				BasicDBList list = (BasicDBList)mo.get(key);
				return list.toString();
			}
			return o.toString();
		}
		else {
			log.warn("Missing field {}",key);
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
			log.warn("Missing field {}",key);
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
			log.warn("Missing field {}",key);
			return def;
		}
	}

	public Date getDBODate(Object dbo, String key, Date def) {
		DBObject mo = (DBObject)dbo;
		if (mo.containsField(key))
			return (Date)mo.get(key);
		else {
			log.warn("Missing field {}",key);
			return def;
		}
	}

	public InputStream getDBORaw(Object dbo, String uid) {
		try {
			GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
			GridFSDBFile imageForOutput = gfsRaw.findOne(uid);
			return imageForOutput.getInputStream();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
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
		new Thread(cleanRaw()).start();
	}

	private WorkerThread cleanRaw() {
		WorkerThread wt = new WorkerThread(StorageIf.WT_NAME) {

			@Override
			public void run() {
				setProgress(0);
				int issues = 0;
				try {
					log.info("Looking for orphaned blobs");
					// clean up any blobs who have no associated inbox message
					GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
					DBCursor cursor = gfsRaw.getFileList();
					DBObject dbo;
					int count = 0;
					while(cursor.hasNext()) {
						dbo = cursor.next();
						BasicDBObject query = new BasicDBObject(BlueboxMessage.UID, dbo.get("filename").toString());
						if (db.getCollection(TABLE_NAME).findOne(query)==null) {
							log.info("Removing orphaned blob {}",dbo.get("filename"));
							gfsRaw.remove(dbo);
							issues++;
						}
						count++;
						setProgress(count*100/cursor.count());
					}
					cursor.close();
					log.info("Finished looking for orphaned blobs");
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				finally {
					setProgress(100);
					setStatus(issues+" issues fixed");
				}
			}
		};
		return wt;
	}

	@Override
	public void deleteAll() throws Exception {
		log.debug("Deleting all inboxes");
		if (db!=null) {
			DBCollection coll = db.getCollection(TABLE_NAME);
			if (coll!=null)
				coll.drop();
			DBCollection props = db.getCollection(PROPS_TABLE_NAME);
			if (props!=null)
				props.drop();
		}
		else {
			log.error("Cannot delete from closed inbox");
		}
		// remove all blobs
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAW);
		DBCursor cursor = gfsRaw.getFileList();
		DBObject dbo;
		while(cursor.hasNext()) {
			dbo = cursor.next();
			log.debug("Deleting raw $s",dbo.get("filename"));
			gfsRaw.remove(dbo);
		}
		cursor.close();
	}

	@Override
	public long getMailCount(BlueboxMessage.State state) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.ordinal());
		return db.getCollection(TABLE_NAME).count(query);
	}

	@Override
	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		if ((inbox==null)||(inbox.getAddress().length()==0))
			return getMailCount(state);
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.ordinal());
		if (inbox!=null)
			query.append(BlueboxMessage.INBOX, inbox.getAddress());
		long start = new Date().getTime();
		long count = db.getCollection(TABLE_NAME).count(query);
		log.debug("Calculated mail count in {}",(new Date().getTime()-start)+"ms");
		return count;
	}

	public DBCursor listMailCommon(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.ordinal());
		if ((inbox!=null)&&(inbox.getFullAddress().length()>0))
			query.append(BlueboxMessage.INBOX, inbox.getAddress());
		int sortBit;
		if (ascending) sortBit = 1; else sortBit = -1;
		if (count<0)
			count = 500;
		return db.getCollection(TABLE_NAME).find(query).sort( new BasicDBObject( orderBy , sortBit )).skip(start).limit(count);
	}

	public List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		List<BlueboxMessage> results = new ArrayList<BlueboxMessage>();
		DBCursor cursor = listMailCommon(inbox, state, start, count, orderBy, ascending);
		try {
			while (cursor.hasNext()) {
				try {
					DBObject dbo = cursor.next();
					BlueboxMessage m = loadMessage(dbo);
					results.add(m);
				}
				catch (Throwable t) {
					log.error("Nasty problem loading message:{}",t.getMessage());;
				}
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
		try {
			if (cursor.hasNext()) {
				DBObject dbo = cursor.next();
				dbo.put(BlueboxMessage.STATE, state.ordinal());
				db.getCollection(TABLE_NAME).update(query, dbo);
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
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
			logError(title,Utils.convertStreamToString(content));
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void logError(String title, String content) {
		GridFSInputFile gfs = errorFS.createFile(content.getBytes());
		gfs.put("title", title);
		gfs.put("date", new Date());
		gfs.save();
		log.debug("Saved with id {}",gfs.getId());
	}

	public int logErrorCount() {
		DBCursor cursor = errorFS.getFileList();
		try {
			// retrieve GridFS object "smithco"
			int count = cursor.count();
			return count;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		return 0;
	}

	public void logErrorClear() {
		try {
			log.info("Clearing error db");
			errorFS.getDB().dropDatabase();
			//			logError("Error db cleared",Utils.convertStringToStream("Requested "+new Date().toString()));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public JSONArray logErrorList(int start, int count) {
		DBCursor cursor = errorFS.getFileList();
		try {
			cursor.skip(start);
			JSONArray result = new JSONArray();
			JSONObject logError;
			while ((result.length()<count&&cursor.hasNext())) {
				DBObject dbo = cursor.next();
				logError = new JSONObject();
				logError.put("title", getDBOString(dbo,"title",""));
				logError.put("date", getDBODate(dbo,"date",new Date()));
				logError.put("id", dbo.get("_id"));
				result.put(logError);
			}
			return result;
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		return null;
	}

	@Override
	public String logErrorContent(String id) {
		try {
			ObjectId oid = new ObjectId(id);
			GridFSDBFile file = errorFS.findOne(oid);	
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
			jo.put(Inbox.EMAIL,"");
			jo.put(BlueboxMessage.RECIPIENT,"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		DBObject sum = new BasicDBObject();
		sum.put("$sum", 1);

		DBObject group = new BasicDBObject();
		group.put("_id", "$"+BlueboxMessage.RECIPIENT);
		group.put(BlueboxMessage.COUNT, sum);

		DBObject all = new BasicDBObject();
		all.put("$group", group);

		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, -1));
		List<DBObject> pipeline = Arrays.asList(all, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);

		for (DBObject result : output.results()) {
			try {
				InboxAddress ia = new InboxAddress(result.get("_id").toString());

				jo.put(Inbox.EMAIL,ia.getFullAddress());
				jo.put(BlueboxMessage.RECIPIENT,ia.getDisplayName());
				jo.put(BlueboxMessage.COUNT,result.get(BlueboxMessage.COUNT));
				break;// only care about first result
			} 
			catch (Throwable e) {
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
		//				    log.info(cursor.next());
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
		//				    log.info(cursor.next());
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
	public WorkerThread runMaintenance() throws Exception {
		return cleanRaw();
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

		// now fill in query results
		String json = "{$group : { _id : { day: { $dayOfMonth: \"$"+StorageIf.Props.Received.name()+"\" }}, count: { $sum: 1 }}}";
		DBObject sum = (DBObject) JSON.parse(json);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, -1));
		List<DBObject> pipeline = Arrays.asList(sum, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);
		//{ "_id" : { "day" : 30} , "count" : 10}
		DBObject row;
		for (DBObject result : output.results()) {
			try {
				row = (DBObject) result.get("_id");
				resultJ.put(row.get("day").toString(),result.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
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

		// now fill in query results
		String json = "{$group : { _id : { hour: { $hour: \"$"+StorageIf.Props.Received.name()+"\" }}, count: { $sum: 1 }}}";
		DBObject sum = (DBObject) JSON.parse(json);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, 1));
		List<DBObject> pipeline = Arrays.asList(sum, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);
		//{ "hour" : 10}
		int hour;
		DBObject row;
		for (DBObject result : output.results()) {
			try {
				row = (DBObject) result.get("_id");
				// not sure why, but hour is always off by one
				// so 12 is returned as 11, etc
				hour = Integer.parseInt(row.get("hour").toString())+1;
				if (hour==24) hour = 0;
				resultJ.put(""+hour,result.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return resultJ;
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
			t.printStackTrace();
		}

		// now fill in query results
		String json = "{$group : { _id : { dayOfWeek: { $dayOfWeek: \"$"+StorageIf.Props.Received.name()+"\" }}, count: { $sum: 1 }}}";
		DBObject sum = (DBObject) JSON.parse(json);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, 1));
		List<DBObject> pipeline = Arrays.asList(sum, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);
		//{ "dayOfWeek" : 2}, 1 = Sunday, 2 = monday etc
		int dayOfWeek;
		DBObject row;
		for (DBObject result : output.results()) {
			try {
				row = (DBObject) result.get("_id");
				dayOfWeek = Integer.parseInt(row.get("dayOfWeek").toString());
				if (dayOfWeek==24) dayOfWeek = 0;
				resultJ.put(""+dayOfWeek,result.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return resultJ;
	}
}
