package com.bluebox.smtp.storage.mongodb;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
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

@Deprecated
public class StorageImpl extends AbstractStorage implements StorageIf {
//	public static final String DB_NAME = "bluebox401";
	private static final Logger log = LoggerFactory.getLogger(StorageImpl.class);
	private static final String DB_ERR_NAME = "bluebox_errors";
	private static final String TABLE_NAME = "inbox";
	private static final String BLOB_NAME = "blob";
	private static final String PROPS_TABLE_NAME = "properties";
	private MongoClient mongoClient;
	private DB db;
	private GridFS errorFS, blobFS;

	@Override
	public void start() throws Exception {
		log.info("Starting MongoDB connection");
		mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
		db = mongoClient.getDB(DB_NAME);
		errorFS = new GridFS(mongoClient.getDB(DB_ERR_NAME),DB_ERR_NAME);
		blobFS = new GridFS(mongoClient.getDB(BLOB_NAME),BLOB_NAME);
		createIndexes();

		log.debug("Started MongoDB connection");
	}

	private void createIndexes() {
		// create indexes
//		String[] indexes = new String[]{
//				StorageIf.Props.Uid.name(),StorageIf.Props.Inbox.name(),StorageIf.Props.Subject.name(), 
//				StorageIf.Props.Sender.name(),StorageIf.Props.Received.name(),StorageIf.Props.Size.name(),StorageIf.Props.State.name()};
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
	public void stop() throws Exception {
		mongoClient.close();
		//StorageFactory.clearInstance();
		log.info("Stopped Mongo client");
	}

	@Override
	public void store(JSONObject props, String spooledUid) throws Exception {
		store(props,getSpooledInputStream(spooledUid));
	}

	public void store(JSONObject props, InputStream blob) throws Exception {
		try {
			DBCollection coll = db.getCollection(TABLE_NAME);
			DBObject bson = ( DBObject ) JSON.parse( props.toString() );
			Date d = Utils.getUTCDate(getUTCTime(),props.getLong(StorageIf.Props.Received.name()));
			bson.put(StorageIf.Props.Received.name(), d);
			GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAWUID);
			GridFSInputFile gfsFile = gfsRaw.createFile(blob,true);
			gfsFile.setFilename(props.getString(StorageIf.Props.Uid.name()));
			gfsFile.save();
			coll.insert(bson);
		}
		catch (Throwable t) {
			log.error("Error storing message :{}",t.getMessage());
		}
	}

	@Override
	public synchronized BlueboxMessage retrieve(String uid) throws Exception {
		BasicDBObject query = new BasicDBObject(StorageIf.Props.Uid.name(), uid);
		log.debug("Looking for uid {}",uid);
		DBObject dbo = db.getCollection(TABLE_NAME).findOne(query);
		if (dbo==null) {
			throw new Exception("Trying to retrieve non-existent uid "+uid);
		}
		return loadMessage(dbo);
	}

	@Override
	public boolean contains(String uid) {
		BasicDBObject query = new BasicDBObject(StorageIf.Props.Uid.name(), uid);
		DBObject dbo = db.getCollection(TABLE_NAME).findOne(query);
		return (dbo!=null);
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
		return Integer.parseInt(getDBOString(dbo, key, Integer.toString(def)));
	}

	@Override
	public long getDBOLong(Object dbo, String key, long def) {
		return Long.parseLong(getDBOString(dbo, key, Long.toString(def)));
	}

	@Override
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
			GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAWUID);
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
	public void delete(String uid, String rawId) {
		log.error("rawid not yet implemented");
		BasicDBObject query = new BasicDBObject(StorageIf.Props.Uid.name(), uid);
		db.getCollection(TABLE_NAME).remove(query);	
		// remove the RAW blob too
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAWUID);
		gfsRaw.remove(gfsRaw.findOne(uid));
	}

//	@Override
//	public void deleteAll(InboxAddress inbox) throws Exception {
//		BasicDBObject query = new BasicDBObject(StorageIf.Props.Inbox.name(), inbox.getAddress());
//		db.getCollection(TABLE_NAME).remove(query);
//		new Thread(cleanRaw()).start();
//	}

	public WorkerThread cleanRaw() throws Exception {
		WorkerThread wt = new WorkerThread(StorageIf.RAWCLEAN) {

			@Override
			public void run() {
				setProgress(0);
				int issues = 0;
				try {
					log.info("Looking for orphaned blobs");
					// clean up any blobs who have no associated inbox message
					GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAWUID);
					DBCursor cursor = gfsRaw.getFileList();
					DBObject dbo;
					int count = 0;
					setStatus("Running");
					setProgress(0);
					while(cursor.hasNext()) {
						if (isStopped()) break;
						dbo = cursor.next();
						BasicDBObject query = new BasicDBObject(StorageIf.Props.Uid.name(), dbo.get("filename").toString());
						if (db.getCollection(TABLE_NAME).findOne(query)==null) {
							log.info("Removing orphaned blob {}",dbo.get("filename"));
							gfsRaw.remove(dbo);
							issues++;
						}
						count++;
						setProgress(count*100/cursor.count());
					}
					setStatus("Found and cleaned "+issues+" orphaned blobs");
					setProgress(0);
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
		GridFS gfsRaw = new GridFS(db, BlueboxMessage.RAWUID);
		DBCursor cursor = gfsRaw.getFileList();
		DBObject dbo;
		while(cursor.hasNext()) {
			dbo = cursor.next();
//			log.debug("Deleting raw {}",dbo.get("filename"));
			gfsRaw.remove(dbo);
		}
		cursor.close();
	}

	@Override
	public long getMailCount(BlueboxMessage.State state) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(StorageIf.Props.State.name(), state.ordinal());
		return db.getCollection(TABLE_NAME).count(query);
	}

	@Override
	public long getMailCount(InboxAddress inbox, BlueboxMessage.State state) throws Exception {
		if ((inbox==null)||(inbox.getAddress().length()==0))
			return getMailCount(state);
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(StorageIf.Props.State.name(), state.ordinal());
		if (inbox!=null)
			query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());
//		long start = getUTCTime().getTime();
		long count = db.getCollection(TABLE_NAME).count(query);
//		log.debug("Calculated mail count in {}ms",(getUTCTime().getTime()-start));
		return count;
	}

	public DBCursor listMailCommon(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(StorageIf.Props.State.name(), state.ordinal());
		if ((inbox!=null)&&(inbox.getFullAddress().length()>0))
			query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());
		int sortBit;
		if (ascending) sortBit = 1; else sortBit = -1;
		if (count<0)
			count = 500;
		return db.getCollection(TABLE_NAME).find(query).sort( new BasicDBObject( orderBy , sortBit )).skip(start).limit(count);
	}

	@Override
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
	public List<LiteMessage> listMailLite(InboxAddress inbox, State state, int start, int count, String orderBy, boolean ascending) throws Exception {

		List<LiteMessage> results = new ArrayList<LiteMessage>();
		DBCursor cursor = listMailCommon(inbox, state, start, count, orderBy, ascending);
		try {
			while (cursor.hasNext()) {
				DBObject dbo = cursor.next();
				JSONObject m = loadMessageJSON(dbo);
				results.add(new LiteMessage(m));
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
		BasicDBObject query = new BasicDBObject(StorageIf.Props.Uid.name(), uid);
		DBCursor cursor = db.getCollection(TABLE_NAME).find(query);
		try {
			if (cursor.hasNext()) {
				DBObject dbo = cursor.next();
				dbo.put(StorageIf.Props.State.name(), state.ordinal());
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

	@Override
	public void logError(String title, InputStream content) {
		GridFSInputFile gfs = errorFS.createFile(content);
		gfs.put("title", title);
		gfs.put("date", getUTCTime());
		gfs.save();
		log.debug("Saved with id {}",gfs.getId());
	}

	@Override
	public void logError(String title, String content) {
		logError(title,IOUtils.toInputStream(content));
	}

	@Override
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

	@Override
	public void logErrorClear() {
		try {
			log.info("Clearing error db");
			errorFS.getDB().dropDatabase();
			//			logError("Error db cleared",Utils.convertStringToStream("Requested "+Utils.getUTCTime()().toString()));
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
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
				logError.put("date", getDBODate(dbo,"date",getUTCTime()));
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
			jo.put(StorageIf.Props.Recipient.name(),"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		DBObject sum = new BasicDBObject();
		sum.put("$sum", 1);

		DBObject group = new BasicDBObject();
		group.put("_id", "$"+StorageIf.Props.Recipient.name());
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
				jo.put(StorageIf.Props.Recipient.name(),ia.getDisplayName());
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
			jo.put(StorageIf.Props.Inbox.name(),"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		DBObject sum = new BasicDBObject();sum.put("$sum", 1);
		DBObject group = new BasicDBObject();
		group.put("_id", "$"+StorageIf.Props.Sender.name());
		group.put(BlueboxMessage.COUNT, sum);
		DBObject all = new BasicDBObject();
		all.put("$group", group);
		DBObject sort = new BasicDBObject("$sort", new BasicDBObject(BlueboxMessage.COUNT, -1));
		List<DBObject> pipeline = Arrays.asList(all, sort);
		AggregationOutput output = db.getCollection(TABLE_NAME).aggregate(pipeline);

		for (DBObject result : output.results()) {
			try {
				jo.put(StorageIf.Props.Sender.name(),new JSONArray(result.get("_id").toString()).get(0));
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
	//			jo.put(StorageIf.Props.Inbox.name(),"");
	//			jo.put(BlueboxMessage.COUNT,0);
	//			@SuppressWarnings("unchecked")
	//			List<String> inboxes = db.getCollection(TABLE_NAME).distinct(StorageIf.Props.Inbox.name());
	//			long currcount, maxcount = 0;
	//			for (String inbox : inboxes) {
	//				currcount = getMailCount(new InboxAddress(inbox), BlueboxMessage.State.NORMAL);
	//				if (currcount>maxcount) {
	//					jo.put(StorageIf.Props.Inbox.name(),inbox);
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
	//		List<String> inboxes = db.getCollection(TABLE_NAME).distinct(StorageIf.Props.Inbox.name());
	//		return inboxes;
	//	}

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
				hour = Integer.parseInt(row.get("hour").toString());
				//if (hour==24) hour = 0;
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

	@Override
	public JSONObject getMPH(InboxAddress inbox) {
		JSONObject resultJ = new JSONObject();
		int mph;
		// now fill in query results
		// db.posts.find({created_on: {$gte: start, $lt: end}});
		// db.gpsdatas.find({"createdAt" : { $gte : new ISODate("2012-01-12T20:15:31Z") }});
		Date lastHour = Utils.getUTCDate(getUTCTime(),getUTCTime().getTime()-60*60*1000);// one hour ago
		BasicDBObject query = new BasicDBObject();

		// calculate mph for last hour
		query.put(StorageIf.Props.Received.name(), new BasicDBObject("$gte", lastHour));
		if ((inbox!=null)&&(inbox.getFullAddress().trim().length()>0)) {
			query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());

		}
		DBCursor output = db.getCollection(TABLE_NAME).find(query);
		mph = output.count();
		output.close();
		try {
			resultJ.put("mph", mph);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		// calculate mph for last 24 hours
		//		Date last24Hour = Utils.getUTCDate(Utils.getUTCTime().getTime()-24*60*60*1000);// 24 hours ago
		//		query.put(StorageIf.Props.Received.name(), new BasicDBObject("$gte", last24Hour));
		//		output = db.getCollection(TABLE_NAME).find(query);
		//		mph24 = output.count()/24;
		//		output.close();
		//		try {
		//			resultJ.put("mph24", mph24);
		//		} 
		//		catch (JSONException e) {
		//			e.printStackTrace();
		//		}
		return resultJ;
	}

	public void setProperty(String key, String value) {
		//not allowed to have '.'s in field names
		key = key.replace('.', '_');
		DBCollection coll = db.getCollection(PROPS_TABLE_NAME);		
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.put("name", key); 
		// your update condition
		DBObject dbo = coll.findOne(dbObject);
		try {
			if (dbo!=null) {
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
			log.error("Error setting property :");
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		//not allowed to have '.'s in field names
		key = key.replace('.', '_');
		@SuppressWarnings("unchecked")
		List<String> keys = db.getCollection(PROPS_TABLE_NAME).distinct(key);
		if (keys.size()>0)
			return keys.get(0);
		else		
			return defaultValue;
	}

	@Override
	public String spoolStream(InputStream blob) throws Exception {
		return spoolStream(UUID.randomUUID().toString(), blob);
	}
	
	public String spoolStream(String uid, InputStream blob) throws Exception {
		log.info("Spool count is {}",getSpoolCount());
		new Exception().printStackTrace();
		try {
			GridFSInputFile gfs = blobFS.createFile(blob);
			gfs.setId(uid);
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
	public WorkerThread cleanOrphans() throws Exception {
		log.error("NOT IMPLEMENTED");
		return null;
	}
	
	@Override
	public Date getUTCTime() {
		return Utils.getUTCCalendar().getTime();
	}

	@Override
	public Object[] search(String querystr, SearchFields fields, int start,	int count, SortFields orderBy, boolean ascending) {
		log.error("This class is deprecated, this method should never be called");
		return null;
	}

	@Override
	public List<String> getDBOArray(Object dbo, String key) {
		// TODO Auto-generated method stub
		return null;
	}

}
