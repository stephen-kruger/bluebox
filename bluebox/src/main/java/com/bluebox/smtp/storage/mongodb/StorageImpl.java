package com.bluebox.smtp.storage.mongodb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;
import com.bluebox.smtp.storage.BlueboxMessage;
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

public class StorageImpl implements StorageIf {
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
				BlueboxMessage.UID,BlueboxMessage.INBOX,BlueboxMessage.TO,BlueboxMessage.SUBJECT, 
				BlueboxMessage.FROM,BlueboxMessage.RECEIVED,BlueboxMessage.SIZE,BlueboxMessage.STATE,BlueboxMessage.AUTO_COMPLETE};
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

	public BlueboxMessage store(InboxAddress inbox, String from, MimeMessageWrapper bbmm) throws Exception {
		BlueboxMessage message = new BlueboxMessage(UUID.randomUUID().toString());
		message.setInbox(inbox);
		message.setBlueBoxMimeMessage(from, bbmm);
		DBCollection coll = db.getCollection(TABLE_NAME);
		DBObject bson = ( DBObject ) JSON.parse( message.toJSON(true) );

		//		bson.put(BlueboxMessage.TO, inbox.getFullAddress());
		//		bson.put(BlueboxMessage.FROM, BlueboxMessage.getFrom(from, bbmm));
		//		bson.put(BlueboxMessage.INBOX, inbox.getAddress());
		bson.put(BlueboxMessage.RAW, Utils.convertStreamToString(bbmm.getInputStream()));
		BasicDBObject doc = new BasicDBObject();
		doc.putAll(bson);
		//		doc.put(BlueboxMessage.INBOX, inbox.getAddress());
		//		StringBuffer s = new StringBuffer();
		//		Address[] r = bbmm.getAllRecipients();
		//		if (r!=null) {
		//			for (int i = 0; i < r.length; i++)
		//				s.append(r[i].toString()).append(' ');
		//		}
		//		else {
		//			// bcc message, we're not on the list
		//			s.append(inbox);
		//			doc.put(BlueboxMessage.TO, inbox.getAddress());
		//		}
		//		doc.put(MessageImpl.TO_LOWER, s.toString().toLowerCase().trim());
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

	public BlueboxMessage loadMessage(DBObject dbo) throws Exception {
		//		MimeMessageWrapper mmwDbo = new MimeMessageWrapper(null, new ByteArrayInputStream(dbo.get(MessageImpl.BINARY_CONTENT).toString().getBytes("UTF-8")));
		String uid = getDBOString(dbo,BlueboxMessage.UID,UUID.randomUUID().toString());
		BlueboxMessage message = new BlueboxMessage(uid);
		if (dbo.containsField(BlueboxMessage.TO))
			message.setProperty(BlueboxMessage.TO,getDBOString(dbo,BlueboxMessage.TO,"bluebox@bluebox.com"));
		else {
			log.warning("Missing field "+BlueboxMessage.TO);
		}
		if (dbo.containsField(BlueboxMessage.AUTO_COMPLETE))
			message.setProperty(BlueboxMessage.AUTO_COMPLETE,getDBOString(dbo,BlueboxMessage.AUTO_COMPLETE,"bluebox@bluebox.com"));
		else
			log.warning("Missing field "+BlueboxMessage.AUTO_COMPLETE);

		if (dbo.containsField(BlueboxMessage.FROM))
			message.setProperty(BlueboxMessage.FROM,getDBOString(dbo,BlueboxMessage.FROM,"bluebox@bluebox.com"));
		else
			log.warning("Missing field "+BlueboxMessage.FROM);

		if (dbo.containsField(BlueboxMessage.SUBJECT))
			message.setProperty(BlueboxMessage.SUBJECT,getDBOString(dbo,BlueboxMessage.SUBJECT,""));
		else
			log.warning("Missing field "+BlueboxMessage.SUBJECT);

		if (dbo.containsField(BlueboxMessage.RECEIVED))
			message.setProperty(BlueboxMessage.RECEIVED,getDBOString(dbo,BlueboxMessage.RECEIVED,""));
		else
			log.warning("Missing field "+BlueboxMessage.RECEIVED);

		if (dbo.containsField(BlueboxMessage.STATE))
			message.setProperty(BlueboxMessage.STATE,getDBOString(dbo,BlueboxMessage.STATE,BlueboxMessage.State.NORMAL.name()));
		else
			log.warning("Missing field "+BlueboxMessage.STATE);

		if (dbo.containsField(BlueboxMessage.INBOX))
			message.setProperty(BlueboxMessage.INBOX,getDBOString(dbo,BlueboxMessage.INBOX,"bluebox@bluebox.com"));
		else
			log.warning("Missing field "+BlueboxMessage.INBOX);

		if (dbo.containsField(BlueboxMessage.SIZE))
			message.setProperty(BlueboxMessage.SIZE,getDBOString(dbo,BlueboxMessage.SIZE,"0"));
		else
			log.warning("Missing field "+BlueboxMessage.SIZE);
		//		if (dbo.containsField(MessageImpl.BINARY_CONTENT))
		//			message.setProperty(MessageImpl.BINARY_CONTENT,dbo.get(MessageImpl.BINARY_CONTENT).toString());
		//		else
		//			log.warning("Missing field "+MessageImpl.BINARY_CONTENT);
		message.loadBlueBoxMimeMessage(new MimeMessageWrapper(null, new ByteArrayInputStream(dbo.get(BlueboxMessage.RAW).toString().getBytes("UTF-8"))));
		return message;
	}

	public static String getDBOString(DBObject dbo, String key, String defaultValue) {
		if (dbo.containsField(key)) {
			Object o = dbo.get(key);
			if (o instanceof BasicDBList) {
				return ((BasicDBList)dbo.get(key)).get(0).toString();
			}
			return o.toString();
		}
		else {
			return defaultValue;
		}
	}

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
		log.info("Calculated mail count in "+(new Date().getTime()-start)+"ms");
		return count;
	}

	public List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
		// this special case happens when no email specified on Atom feed
		//		if ("*"==inbox) {
		//			inbox=null;
		//		}

		BasicDBObject query = new BasicDBObject();
		if (state != BlueboxMessage.State.ANY)
			query.append(BlueboxMessage.STATE, state.name());
		if ((inbox!=null)&&(inbox.getFullAddress().length()>0))
			query.append(BlueboxMessage.INBOX, inbox.getAddress());
		int sortBit;
		if (ascending) sortBit = 1; else sortBit = -1;
		if (count<0)
			count = 500;//Integer.MAX_VALUE; else we get "com.mongodb.MongoException: too much data for sort() with no index.  add an index or specify a smaller limit"
		DBCursor cursor = db.getCollection(TABLE_NAME).find(query).sort( new BasicDBObject( orderBy , sortBit )).skip(start).limit(count);
		List<BlueboxMessage> results = new ArrayList<BlueboxMessage>();
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

	public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
		long startTime = new Date().getTime();
		// the Query has already been requested to start at correct place
		JSONObject curr;
		List<BlueboxMessage> mail = listMail(inbox, state, start, count, orderBy, ascending);
		int index = 0;
		writer.write("[");
		for (BlueboxMessage message : mail) {
			curr = new JSONObject();
			curr.put(BlueboxMessage.FROM, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.FROM)));
			curr.put(BlueboxMessage.SUBJECT, Utils.decodeRFC2407(message.getPropertyString(BlueboxMessage.SUBJECT)));
			//			curr.put(MessageImpl.SUBJECT,  Utils.convertEncoding(Utils.decodeRFC2407(msg.getPropertyString(MessageImpl.SUBJECT)),"GB2312"));
			//			ByteBuffer b = ByteBuffer.wrap(Utils.decodeRFC2407(msg.getBlueBoxMimeMessage().getSubject()).getBytes());
			//			curr.put(MessageImpl.SUBJECT,  java.nio.charset.Charset.forName("GB2312").newDecoder().decode(b));
			//			curr.put(MessageImpl.SUBJECT, msg.getBlueBoxMimeMessage().getSubject());
			// convert the date to the locale used by the users browser
			if (message.hasProperty(BlueboxMessage.RECEIVED)) {
				//				curr.put(MessageImpl.RECEIVED, new Date(msg.getLongProperty(MessageImpl.RECEIVED)));
				curr.put(BlueboxMessage.RECEIVED, SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.SHORT, locale).format(new Date(message.getLongProperty(BlueboxMessage.RECEIVED))));
			}
			if (message.hasProperty(BlueboxMessage.SIZE)) {
				curr.put(BlueboxMessage.SIZE, message.getPropertyString(BlueboxMessage.SIZE)+"K");
			}
			else {
				curr.put(BlueboxMessage.SIZE, "1K");
			}
			curr.put(BlueboxMessage.UID, message.getIdentifier());
			writer.write(curr.toString(3));
			if ((index++)<mail.size()-1) {
				writer.write(",");
			}
		}
		writer.write("]");
		writer.flush();
		log.info("Served inbox contents in "+(new Date().getTime()-startTime)+"ms");
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

	//	@Override
	//	public JSONArray autoComplete(String hint, long start, long count) throws Exception {
	//		if (count<0)
	//			count = 500;
	//		if ((hint==null)||(hint=="")) hint="*";
	//		DBCollection coll = db.getCollection(TABLE_NAME);
	//		DBCursor cursor;
	//		if (hint.length()<3) {
	//			if (hint.equals("*")) {
	//				//				cursor = coll.distinct(MessageImpl.INBOX);
	//				cursor = coll.find().limit((int)count).skip((int)start);;
	//				log.fine("wild card result-count="+cursor.count()+" start="+start+" count="+count);
	//			}
	//			else {
	//				log.fine("short circuit "+hint+" "+start+" "+count);
	//				return new JSONArray();
	//			}
	//		}
	//		else {
	//			//			autocompleteQuery = new BasicDBObject(MessageImpl.TO_LOWER,new BasicDBObject("$regex", hint.toLowerCase()));
	//			//			log.info(">>>>>>>>>looking for "+hint);
	//			DBObject autocompleteQuery = new BasicDBObject(BlueboxMessage.AUTO_COMPLETE,new BasicDBObject("$regex", hint.toLowerCase()));
	//			cursor = coll.find(autocompleteQuery).limit((int)count).skip((int)start);
	//			log.fine("normal "+cursor.count()+" hint:"+hint+" "+autocompleteQuery);
	//		}
	//
	//		JSONArray children = new JSONArray();
	//		JSONObject curr;
	//
	//		while (cursor.hasNext()) {
	//			DBObject dbo = cursor.next();
	//			//			log.info(dbo.toString());
	//			//			log.info(dbo.keySet().toString());
	//			String name = Utils.decodeRFC2407(getDBOString(dbo, BlueboxMessage.INBOX,"error8473"));
	//			String label = Utils.decodeRFC2407(getDBOString(dbo, BlueboxMessage.TO,name));
	//			String identifier = getDBOString(dbo, BlueboxMessage.UID,name);
	//			if (!autoCompleteContains(children,name)) {
	//				curr = new JSONObject();
	//				curr.put("name", name);
	//				curr.put("label", label);
	//				curr.put("identifier", identifier);
	//				children.put(curr);
	//			}
	//
	//			if (children.length()>=count)
	//				break;
	//		}
	//		cursor.close();
	//		return children;
	//	}

	//	/**
	//	 * Ensures no type-ahead duplicates
	//	 *
	//	 * @param children the existingchildren
	//	 * @param id the id of the root node containing the match
	//	 * @return true, if entry already exists
	//	 */
	//	private boolean autoCompleteContains(JSONArray children, String name) {
	//		try {
	//			JSONObject c;
	//			for (int i = 0; i < children.length();i++) {
	//				c = children.getJSONObject(i);
	//				if (c.get("name").equals(name)) {
	//					return true;
	//				}
	//			}
	//		}
	//		catch (JSONException e) {
	//			e.printStackTrace();
	//		}
	//		return false;
	//	}

	//	private JSONObject updateStatsRecent() {
	//		JSONObject jo = new JSONObject();
	//
	//		try {
	//			jo.put(MessageImpl.SUBJECT, "");
	//			jo.put(MessageImpl.TO, "");
	//			jo.put(MessageImpl.FROM, "");
	//		} 
	//		catch (JSONException e1) {
	//			e1.printStackTrace();
	//		}
	//
	//		try {
	//			List<MessageImpl> msgs = listMail(null, MessageImpl.State.NORMAL, 0, 1, MessageImpl.RECEIVED, false);
	//			if (msgs.size()>0) {
	//				MessageImpl msg = msgs.get(0);
	//				jo.put(MessageImpl.SUBJECT, msg.getBlueBoxMimeMessage().getSubject());
	//				jo.put(MessageImpl.TO, msg.getInbox());
	//				jo.put(MessageImpl.FROM, msg.getBlueBoxMimeMessage().getFrom()[0].toString());
	//			}
	//		} 
	//		catch (Throwable e) {
	//			e.printStackTrace();
	//		}
	//		setProperty("stats_recent",jo.toString());
	//		return jo;
	//	}

	//	@Override
	//	public JSONObject getStatsRecent() {
	//		JSONObject jo=null;
	//		try {
	//			if (getProperty("stats_recent", "{}")==null) {
	//				return updateStatsRecent();
	//			}
	//			return new JSONObject(this.getProperty("stats_recent", "{}"));
	//		} 
	//		catch (JSONException e2) {
	//			e2.printStackTrace();
	//			return jo;
	//		}
	//	}

	//	private JSONObject updateStatsActive() {
	//		JSONObject jo = new JSONObject();
	//		try {	
	//			@SuppressWarnings("unchecked")
	//			List<String> inboxes = db.getCollection(TABLE_NAME).distinct(MessageImpl.INBOX);
	//			long count = 0;
	//			String inbox = "";
	//			for (String currInbox : inboxes) {
	//				//				long t = getMailCount(currInbox, MessageImpl.State.NORMAL);
	//				long t = db.getCollection(TABLE_NAME).count(new BasicDBObject(MessageImpl.INBOX, currInbox));
	//				if (t>count) {
	//					count = t;
	//					inbox = currInbox;
	//				}
	//			}
	//
	//			jo.put(MessageImpl.COUNT, count);
	//			jo.put(MessageImpl.TO, inbox);
	//		} 
	//		catch (Throwable e) {
	//			e.printStackTrace();
	//			try {
	//				jo.put(MessageImpl.COUNT, 0);
	//				jo.put(MessageImpl.TO, "");
	//			} 
	//			catch (JSONException e1) {
	//				e1.printStackTrace();
	//			}			
	//		}
	//		setProperty("stats_active",jo.toString());
	//		return jo;
	//	}


	//	@Override
	//	public JSONObject getStatsActive() {
	//		JSONObject jo=null;
	//		try {
	//			if (getProperty("stats_active","{}")==null) {
	//				return updateStatsActive();
	//			}
	//			return new JSONObject(this.getProperty("stats_active", "{}"));
	//		} 
	//		catch (JSONException e2) {
	//			e2.printStackTrace();
	//			return jo;
	//		}
	//	}

	//	@Override
	//	public long getStatsGlobalCount() {
	//		return Long.parseLong(getProperty(GLOBAL_COUNT_NODE,"0"));
	//	}
	//
	//	private void incrementGlobalCount() {
	//		long newCount = getStatsGlobalCount()+1;
	//		setProperty(GLOBAL_COUNT_NODE,Long.toString(newCount));
	//	}	
	//
	//	@Override
	//	public void setStatsGlobalCount(long count) {
	//		setProperty(GLOBAL_COUNT_NODE,Long.toString(count));
	//	}

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

	@SuppressWarnings("unchecked")
	@Override
	public List<String> listUniqueInboxes() {
		List<String> inboxes = db.getCollection(TABLE_NAME).distinct(BlueboxMessage.INBOX);
		return inboxes;
	}

	@Override
	public void runMaintenance() throws Exception {
		List<DBObject> indexes = db.getCollection(TABLE_NAME).getIndexInfo();
		for (DBObject index : indexes) {
			db.getCollection(TABLE_NAME).remove(index);
		}
		createIndexes();
	}

}
