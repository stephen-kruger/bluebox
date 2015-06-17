package com.bluebox.smtp.storage.mongodb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.bluebox.smtp.storage.LiteMessage;
import com.bluebox.smtp.storage.StorageIf;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MongoImpl extends AbstractStorage implements StorageIf {
	public static final String DB_NAME = "bluebox43";
	private static final Logger log = LoggerFactory.getLogger(MongoImpl.class);
	private static final String DB_ERR_NAME = "bluebox_errors";
	private static final String TABLE_NAME = "inbox"; // name of collection AND blob database
	private static final String BLOB_DB_NAME = "blob";
	private static final String PROPS_DB_NAME = "properties";
	private static final String RAW_DB_NAME = "inbox";
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> errorFS, propsFS, mailFS;
	private GridFS blobFS, rawFS;

	@SuppressWarnings("deprecation")
	@Override
	public void start() throws Exception {
		log.info("Starting MongoDB connection");
		mongoClient = new MongoClient(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST));
		db = mongoClient.getDatabase(DB_NAME);
		mailFS = db.getCollection(TABLE_NAME);
		createIndexes();
		errorFS = db.getCollection(DB_ERR_NAME);
		propsFS = db.getCollection(PROPS_DB_NAME);
		mongoClient.getDatabase("");
		blobFS = new GridFS(mongoClient.getDB(BLOB_DB_NAME),BLOB_DB_NAME);
		rawFS = new GridFS(mongoClient.getDB(RAW_DB_NAME), BlueboxMessage.RAW);

		log.debug("Started MongoDB connection");

	}

	@Override
	public void stop() throws Exception {
		log.info("Stopping MongoDB connection");
		mongoClient.close();
		StorageFactory.clearInstance();
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
		store(props,getSpooledInputStream(spooledUid));
	}

	@Override
	public void store(JSONObject props, InputStream content) throws Exception {
		try {
			Document bson = Document.parse( props.toString() );
			// little hack for int getting converted to longs when going from JSON to BSON
			bson.put(BlueboxMessage.SIZE, Long.parseLong(bson.get(BlueboxMessage.SIZE).toString()));
			Date d = Utils.getUTCDate(getUTCTime(),props.getLong(StorageIf.Props.Received.name()));
			bson.put(StorageIf.Props.Received.name(), d);
			GridFSInputFile gfsFile = rawFS.createFile(content,true);
			gfsFile.setFilename(props.getString(StorageIf.Props.Uid.name()));
			gfsFile.save();
			mailFS.insertOne(bson);
		}
		catch (Throwable t) {
			log.error("Error storing message :{}",t.getMessage());
			t.printStackTrace();
		}
	}

	@Override
	public BlueboxMessage retrieve(String uid) throws Exception {
		FindIterable<Document> fi = mailFS.find(Filters.eq(StorageIf.Props.Uid.name(), uid));
		Document doc = fi.first();
		return loadMessage(doc);
	}

	@Override
	public boolean contains(String uid) {
		return mailFS.count(Filters.eq(StorageIf.Props.Uid.name(), uid))>0;
	}

	//	@Override
	//	public void deleteAll(InboxAddress inbox) throws Exception {
	//		mailFS.deleteMany(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()));
	//	}

	@SuppressWarnings("deprecation")
	@Override
	public void deleteAll() throws Exception {
		mailFS.drop();
		rawFS.getDB().dropDatabase();
		// TODO will be fixed in Mongo 3.1
		rawFS = new GridFS(mongoClient.getDB(RAW_DB_NAME), BlueboxMessage.RAW);
	}

	@Override
	public long getMailCount(State state) throws Exception {
		if (state == BlueboxMessage.State.ANY) {
			return mailFS.count();
		}
		return mailFS.count(Filters.eq(StorageIf.Props.State.name(), state.ordinal()));
	}

	@Override
	public long getMailCount(InboxAddress inbox, State state) throws Exception {
		if (state == BlueboxMessage.State.ANY) {
			if (inbox==null) {
				return mailFS.count();
			}
			else {
				return mailFS.count(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()));
			}
		}
		else {
			if (inbox==null) {
				return mailFS.count(Filters.eq(StorageIf.Props.State.name(), state.ordinal()));
			}
			else {
				return mailFS.count(Filters.and(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()),
						Filters.eq(StorageIf.Props.State.name(), state.ordinal())));
			}
		}
	}

	@Override
	public List<BlueboxMessage> listMail(InboxAddress inbox, State state,
			int start, int count, String orderBy, boolean ascending)
					throws Exception {
		List<BlueboxMessage> results = new ArrayList<BlueboxMessage>();
		MongoCursor<Document> cursor = listMailCommon(inbox, state, start, count, orderBy, ascending).iterator();
		try {
			while (cursor.hasNext()) {
				try {
					Document dbo = cursor.next();
					BlueboxMessage m = loadMessage(dbo);
					results.add(m);
				}
				catch (Throwable t) {
					t.printStackTrace();
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
	public void setState(String uid, State state) throws Exception {
		Document doc = mailFS.find(Filters.eq(StorageIf.Props.Uid.name(), uid)).first();
		doc.put(StorageIf.Props.State.name(), state.ordinal());
		mailFS.findOneAndReplace(Filters.eq(StorageIf.Props.Uid.name(), uid), doc);
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
				logError.put("date", error.getDate("date").getTime());
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
		JSONObject jo = new JSONObject();
		try {
			jo.put(Inbox.EMAIL,"");
			jo.put(StorageIf.Props.Recipient.name(),"");
			jo.put(BlueboxMessage.COUNT,0);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}

		Document sum = new Document();
		sum.put("$sum", 1);

		Document group = new Document();
		group.put("_id", "$"+StorageIf.Props.Recipient.name());
		group.put(BlueboxMessage.COUNT, sum);

		Document all = new Document();
		all.put("$group", group);

		Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
		List<Document> pipeline = Arrays.asList(all, sort);
		MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();

		while ( output.hasNext()) {
			try {
				Document result = output.next();
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
		output.close();
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
		Document all = new Document();
		all.put("$group", group);
		Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
		List<Document> pipeline = Arrays.asList(all, sort);
		MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();

		while (output.hasNext()) {
			try {
				Document result = output.next();
				jo.put(StorageIf.Props.Sender.name(),new JSONArray(result.get("_id").toString()).get(0));
				jo.put(BlueboxMessage.COUNT,result.get(BlueboxMessage.COUNT));
				break;// only care about first result
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		output.close();
		return jo;
	}

	@Override
	public void delete(String uid) throws Exception {
		DeleteResult res = mailFS.deleteOne(Filters.eq(StorageIf.Props.Uid.name(), uid));
		if (res.getDeletedCount()<=0) {
			log.warn("Nothing deleted for uid {}",uid);
		}
		// remove the RAW blob too
		rawFS.remove(rawFS.findOne(uid));
	}

	@Override
	public WorkerThread cleanRaw() throws Exception {
		WorkerThread wt = new WorkerThread(StorageIf.RAWCLEAN) {

			//private GridFS gfsRaw;

			@Override
			public void run() {
				setProgress(0);
				int issues = 0;
				DBCursor cursor = rawFS.getFileList();
				try {
					log.info("Looking for orphaned blobs");
					// clean up any blobs who have no associated inbox message
					DBObject dbo;
					int count = 0;
					setStatus("Running");
					setProgress(0);
					while(cursor.hasNext()) {
						if (isStopped()) break;
						dbo = cursor.next();
						Document query = new Document(StorageIf.Props.Uid.name(), dbo.get("filename").toString());
						if (mailFS.count(query)<=0) {
							log.info("Removing orphaned blob {}",dbo.get("filename"));
							rawFS.remove(dbo);
							issues++;
						}
						count++;
						setProgress(count*100/cursor.count());
					}
					setStatus("Found and cleaned "+issues+" orphaned blobs");
					setProgress(0);
					log.info("Finished looking for orphaned blobs");
				}
				catch (Throwable t) {
					t.printStackTrace();
				}
				finally {
					cursor.close();
					setProgress(100);
					setStatus(issues+" issues fixed");
				}
			}
		};
		return wt;
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
		Document sum = Document.parse(json);
		Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
		List<Document> pipeline = Arrays.asList(sum, sort);
		MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
		//{ "_id" : { "day" : 30} , "count" : 10}
		Document row;
		Document result;
		while ( output.hasNext()) {
			try {
				result = output.next();
				row = (Document) result.get("_id");
				resultJ.put(row.get("day").toString(),result.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		output.close();
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
		Document sum = Document.parse(json);
		Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, 1));
		List<Document> pipeline = Arrays.asList(sum, sort);
		MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
		//{ "hour" : 10}
		int hour;
		Document row;
		Document result;
		while ( output.hasNext()) {
			try {
				result = output.next();
				row = (Document) result.get("_id");
				hour = Integer.parseInt(row.get("hour").toString());
				//if (hour==24) hour = 0;
				resultJ.put(""+hour,result.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		output.close();
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
		Document sum = Document.parse(json);
		Document sort = new Document("$sort", new BasicDBObject(BlueboxMessage.COUNT, 1));
		List<Document> pipeline = Arrays.asList(sum, sort);
		MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
		//{ "dayOfWeek" : 2}, 1 = Sunday, 2 = monday etc
		int dayOfWeek;
		Document row;
		Document doc;
		while ( output.hasNext()) {
			try {
				doc = output.next();
				row = (Document) doc.get("_id");
				dayOfWeek = Integer.parseInt(row.get("dayOfWeek").toString());
				if (dayOfWeek==24) dayOfWeek = 0;
				resultJ.put(""+dayOfWeek,doc.get("count").toString());
			} 
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		output.close();
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
		mph = (int) mailFS.count(query);

		try {
			resultJ.put("mph", mph);
		} 
		catch (JSONException e) {
			e.printStackTrace();
		}
		return resultJ;
	}

	@Override
	public void setProperty(String key, String value) {
		key = key.replace('.', '_');
		Document document = new Document(key,value);
		if (propsFS.findOneAndReplace(Filters.exists(key), document )==null)
			propsFS.insertOne(document);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		try {
			key = key.replace('.', '_');
			FindIterable<Document> result = propsFS.find(Filters.exists(key));
			if (result!=null) {
				if (result.first()!=null) {
					return result.first().getString(key);
				}
			}
		}
		catch (Throwable t) {
			log.error("Something bad happened",t);
		}
		return defaultValue;
	}

	@Override
	public String spoolStream(InputStream blob) throws Exception {
		log.debug("Spool count is {}",getSpoolCount());
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
		log.debug("Removing spool {}",spooledUid);
		try {
			blobFS.remove(new ObjectId(spooledUid));	
		}
		catch (Throwable t){
			log.error("Could not delete specified spooled stream {}",spooledUid);
		}
	}

	@Override
	public long getSpooledStreamSize(String spooledUid) {
		GridFSDBFile blob = blobFS.findOne(new ObjectId(spooledUid));
		return blob.getLength();
	}

	@Override
	public long getSpoolCount() throws Exception {
		DBCursor cursor = blobFS.getFileList();
		long count = 0;
		try {
			count = cursor.count();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		if (count<=MAX_SPOOL_SIZE)
			return count;
		else
			return trimSpools(MAX_SPOOL_SIZE);
	}

	public long trimSpools(long maxSize) throws Exception {
		log.debug("Trimming spool count to {}",maxSize);
		DBCursor cursor = blobFS.getFileList();
		long count = 0;
		try {
			count = cursor.count();
			for (int i = 0; i < (count-maxSize); i++) {
				if (cursor.hasNext()) {
					GridFSDBFile object = (GridFSDBFile) cursor.next();
					removeSpooledStream(object.getId().toString());
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally {
			cursor.close();
		}
		return getSpoolCount();
	}

	@Override
	public Date getUTCTime() {
		return Utils.getUTCCalendar().getTime();
	}

	private FindIterable<Document> listMailCommon(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(StorageIf.Props.State.name(), state.ordinal());
		if ((inbox!=null)&&(inbox.getFullAddress().length()>0))
			query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());
		int sortBit;
		if (ascending) sortBit = 1; else sortBit = -1;
		// limit search results t0 5000 entries
		if ((count<0)||(count>5000))
			count = 5000;
		return mailFS.find(query).sort( new BasicDBObject( orderBy , sortBit )).skip(start).limit(count);
	}

	@Override
	public List<LiteMessage> listMailLite(InboxAddress inbox, State state,
			int start, int count, String orderBy, boolean ascending)
					throws Exception {
		List<LiteMessage> results = new ArrayList<LiteMessage>();
		MongoCursor<Document> cursor = listMailCommon(inbox, state, start, count, orderBy, ascending).iterator();
		try {
			while (cursor.hasNext()) {
				Document dbo = cursor.next();
				try {
					JSONObject m = loadMessageJSON(dbo);
					results.add(new LiteMessage(m));
				}
				catch (Throwable t) {
					log.error("Error loading message {}",t);
				}
			}
		} 
		catch (Throwable t) {
			log.error("Problem listing mail",t);
		}
		finally {
			cursor.close();
		}
		return results;
	}

	@Override
	public String getDBOString(Object dbo, String key, String def) {
		Document doc = (Document)dbo;
		if (doc.containsKey(key))
			return doc.get(key).toString();
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
		if (doc.containsKey(key)) {
			try {
				return doc.getLong(key);
			}
			catch (Throwable t) {
				return (long)doc.getInteger(key);
			}
		}
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
	public InputStream getDBORaw(Object dbo, String uid) {
		try {
			GridFSDBFile imageForOutput = rawFS.findOne(uid);
			return imageForOutput.getInputStream();
		}
		catch (Throwable t) {
			log.error("Error loading raw object for uid="+uid,t);
			return null;
		}
	}



}
