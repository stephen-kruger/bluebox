package com.bluebox.smtp.storage.mongodb;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.search.SearchUtils.SortFields;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.*;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class MongoImpl extends AbstractStorage implements StorageIf {
    public static final String DB_NAME = "bluebox43";
    private static final Logger log = LoggerFactory.getLogger(MongoImpl.class);
    private static final String DB_ERR_NAME = "bluebox_errors";
    private static final String TABLE_NAME = "inbox"; // name of collection AND blob database
    private static final String BLOB_DB_NAME = "blob";
    private static final String PROPS_DB_NAME = "properties";
    private MongoClient mongoClient;
    private MongoDatabase db;
    private MongoCollection<Document> errorFS, propsFS, mailFS;
    private GridFS blobFS;

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
        blobFS = new GridFS(mongoClient.getDB(BLOB_DB_NAME), BLOB_DB_NAME);

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
            } catch (Throwable t) {
                log.error(t.getMessage());
            }

            try {
                db.getCollection(TABLE_NAME).createIndex(new BasicDBObject(index.name(), -1));
            } catch (Throwable t) {
                log.error(t.getMessage());
            }
        }

        // create some compound indexes
        try {
            BasicDBObject obj = new BasicDBObject();
            obj.put(BlueboxMessage.STATE, 1);
            obj.put(BlueboxMessage.RECEIVED, -1);
            db.getCollection(TABLE_NAME).createIndex(obj);
        } catch (Throwable t) {
            log.error(t.getMessage());
        }
    }

    @Override
    public void store(JSONObject props, String spooledUid) throws Exception {
        props.put(BlueboxMessage.RAWUID, spooledUid);
        try {
            Document bson = Document.parse(props.toString());
            // little hack for int getting converted to longs when going from JSON to BSON
            bson.put(BlueboxMessage.SIZE, Long.parseLong(bson.get(BlueboxMessage.SIZE).toString()));
            Date d = Utils.getUTCDate(getUTCTime(), props.getLong(StorageIf.Props.Received.name()));
            bson.put(StorageIf.Props.Received.name(), d);
            //			GridFSInputFile gfsFile = rawFS.createFile(content,true);
            //			gfsFile.setFilename(props.getString(StorageIf.Props.Uid.name()));
            //			gfsFile.save();
            mailFS.insertOne(bson);
        } catch (Throwable t) {
            log.error("Error storing message :{}", t.getMessage());
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
        return mailFS.countDocuments(Filters.eq(StorageIf.Props.Uid.name(), uid)) > 0;
    }

    @Override
    public void deleteAll() throws Exception {
        mailFS.drop();
        blobFS.getDB().dropDatabase();
    }

    @Override
    public long getMailCount(State state) throws Exception {
        if (state == BlueboxMessage.State.ANY) {
            return mailFS.countDocuments();
        }
        return mailFS.countDocuments(Filters.eq(StorageIf.Props.State.name(), state.ordinal()));
    }

    @Override
    public long getMailCount(InboxAddress inbox, State state) throws Exception {
        if (state == BlueboxMessage.State.ANY) {
            if (inbox == null) {
                return mailFS.countDocuments();
            } else {
                return mailFS.countDocuments(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()));
            }
        } else {
            if (inbox == null) {
                return mailFS.countDocuments(Filters.eq(StorageIf.Props.State.name(), state.ordinal()));
            } else {
                return mailFS.countDocuments(Filters.and(Filters.eq(StorageIf.Props.Inbox.name(), inbox.getAddress()),
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
                Document dbo = cursor.next();
                try {
                    BlueboxMessage m = loadMessage(dbo);
                    results.add(m);
                } catch (Throwable t) {
                    //					t.printStackTrace();
                    log.error("Nasty problem loading message:{}", dbo, t);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
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
            logError(title, IOUtils.toString(content, StandardCharsets.UTF_8));
        } catch (IOException ioe) {
            log.error("Error saving log", ioe);
        }
    }

    @Override
    public void logError(String title, String content) {
        Document document = new Document("title", title);
        document.put("date", getUTCTime());
        document.put("uid", UUID.randomUUID().toString());
        document.put("content", content);
        errorFS.insertOne(document);
    }

    @Override
    public int logErrorCount() {
        return (int) errorFS.countDocuments();
    }

    @Override
    public void logErrorClear() {
        errorFS.drop();
    }

    @Override
    public JSONArray logErrorList(int start, int count) {
        FindIterable<Document> errors = errorFS.find(Filters.exists("uid"));
        errors.skip(start);
        errors.limit(count);
        try {
            JSONArray result = new JSONArray();
            JSONObject logError;
            for (Document error : errors) {
                logError = new JSONObject();
                logError.put("title", error.getString("title"));
                logError.put("date", error.getDate("date").toString());
                logError.put("id", error.getString("uid"));
                result.put(logError);
            }
            return result;
        } catch (Throwable t) {
            log.error("Problem getting errors", t);
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
            jo.put(Inbox.EMAIL, "");
            jo.put(StorageIf.Props.Recipient.name(), "");
            jo.put(BlueboxMessage.COUNT, 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Document sum = new Document();
        sum.put("$sum", 1);

        Document group = new Document();
        group.put("_id", "$" + StorageIf.Props.Recipient.name());
        group.put(BlueboxMessage.COUNT, sum);

        Document all = new Document();
        all.put("$group", group);

        Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
        List<Document> pipeline = Arrays.asList(all, sort);
        MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();

        while (output.hasNext()) {
            try {
                Document result = output.next();
                InboxAddress ia = new InboxAddress(result.get("_id").toString());

                jo.put(Inbox.EMAIL, ia.getFullAddress());
                jo.put(StorageIf.Props.Recipient.name(), ia.getDisplayName());
                jo.put(BlueboxMessage.COUNT, result.get(BlueboxMessage.COUNT));
                break;// only care about first result
            } catch (Throwable e) {
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
            jo.put(StorageIf.Props.Inbox.name(), "");
            jo.put(BlueboxMessage.COUNT, 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        DBObject sum = new BasicDBObject();
        sum.put("$sum", 1);
        DBObject group = new BasicDBObject();
        group.put("_id", "$" + StorageIf.Props.Sender.name());
        group.put(BlueboxMessage.COUNT, sum);
        Document all = new Document();
        all.put("$group", group);
        Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
        List<Document> pipeline = Arrays.asList(all, sort);
        MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();

        while (output.hasNext()) {
            try {
                Document result = output.next();
                if (result.containsKey("_id")) {
                    jo.put(StorageIf.Props.Sender.name(), new JSONArray(result.get("_id").toString()).get(0));
                    jo.put(BlueboxMessage.COUNT, result.get(BlueboxMessage.COUNT));
                    break;// only care about first result
                }
            } catch (JSONException e) {
                log.debug("Looking for stats");
            }
        }
        output.close();
        return jo;
    }

    /*
     * Delete only the JSON portion of the mail entry
     */
    private void delete(String uid) throws Exception {
        DeleteResult res = mailFS.deleteMany(Filters.eq(StorageIf.Props.Uid.name(), uid));
        if (res.getDeletedCount() <= 0) {
            log.warn("Nothing deleted for uid {}", uid);
        }
    }

    @Override
    public void delete(String uid, String rawId) throws Exception {
        delete(uid);
        if (spoolReferenced(rawId)) {
            log.debug("Leaving still referenced spooled message {}", rawId);
        } else {
            log.debug("Deleting last instance of spooled message {}", rawId);
            removeSpooledStream(rawId);
        }
    }

    @Override
    public void delete(List<LiteMessage> bulkList) throws Exception {
        List<String> uidList = new ArrayList<String>(bulkList.size());
        for (LiteMessage m : bulkList) {
            uidList.add(m.getIdentifier());
        }
        DeleteResult res = mailFS.deleteMany(Filters.in(StorageIf.Props.Uid.name(), uidList));
        if (res.getDeletedCount() != uidList.size()) {
            log.warn("Bulk deleted only removed {} of expected {} documents", res.getDeletedCount(), uidList.size());
        }

        List<String> rawList = new ArrayList<String>(bulkList.size());
        for (LiteMessage m : bulkList) {
            if (!spoolReferenced(m.getRawIdentifier())) {
                rawList.add(m.getRawIdentifier());
            }
        }
        removeSpooledStream(rawList);
    }

    @Override
    public JSONObject getCountByDay() {
        JSONObject resultJ = new JSONObject();
        try {
            // init stats with empty values
            for (int i = 1; i < 32; i++) {
                resultJ.put(i + "", 0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // now fill in query results
        String json = "{$group : { _id : { day: { $dayOfMonth: \"$" + StorageIf.Props.Received.name() + "\" }}, count: { $sum: 1 }}}";
        Document sum = Document.parse(json);
        Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, -1));
        List<Document> pipeline = Arrays.asList(sum, sort);
        MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
        //{ "_id" : { "day" : 30} , "count" : 10}
        Document row;
        Document result;
        while (output.hasNext()) {
            try {
                result = output.next();
                row = (Document) result.get("_id");
                resultJ.put(row.get("day").toString(), result.get("count").toString());
            } catch (Throwable e) {
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
                resultJ.put(i + "", 0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // now fill in query results
        String json = "{$group : { _id : { hour: { $hour: \"$" + StorageIf.Props.Received.name() + "\" }}, count: { $sum: 1 }}}";
        Document sum = Document.parse(json);
        Document sort = new Document("$sort", new Document(BlueboxMessage.COUNT, 1));
        List<Document> pipeline = Arrays.asList(sum, sort);
        MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
        //{ "hour" : 10}
        int hour;
        Document row;
        Document result;
        while (output.hasNext()) {
            try {
                result = output.next();
                row = (Document) result.get("_id");
                hour = Integer.parseInt(row.get("hour").toString());
                //if (hour==24) hour = 0;
                resultJ.put("" + hour, result.get("count").toString());
            } catch (Throwable e) {
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
                resultJ.put(i + "", 0);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // now fill in query results
        String json = "{$group : { _id : { dayOfWeek: { $dayOfWeek: \"$" + StorageIf.Props.Received.name() + "\" }}, count: { $sum: 1 }}}";
        Document sum = Document.parse(json);
        Document sort = new Document("$sort", new BasicDBObject(BlueboxMessage.COUNT, 1));
        List<Document> pipeline = Arrays.asList(sum, sort);
        MongoCursor<Document> output = mailFS.aggregate(pipeline).iterator();
        //{ "dayOfWeek" : 2}, 1 = Sunday, 2 = monday etc
        int dayOfWeek;
        Document row;
        Document doc;
        while (output.hasNext()) {
            try {
                doc = output.next();
                row = (Document) doc.get("_id");
                dayOfWeek = Integer.parseInt(row.get("dayOfWeek").toString());
                if (dayOfWeek == 24) dayOfWeek = 0;
                resultJ.put("" + dayOfWeek, doc.get("count").toString());
            } catch (Throwable e) {
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
        Date lastHour = Utils.getUTCDate(getUTCTime(), getUTCTime().getTime() - 60 * 60 * 1000);// one hour ago
        BasicDBObject query = new BasicDBObject();

        // calculate mph for last hour
        query.put(StorageIf.Props.Received.name(), new BasicDBObject("$gte", lastHour));
        if ((inbox != null) && (inbox.getFullAddress().trim().length() > 0)) {
            query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());

        }
        mph = (int) mailFS.countDocuments(query);

        try {
            resultJ.put("mph", mph);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resultJ;
    }

    @Override
    public void setProperty(String key, String value) {
        key = key.replace('.', '_');
        Document document = new Document(key, value);
        if (propsFS.findOneAndReplace(Filters.exists(key), document) == null)
            propsFS.insertOne(document);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        try {
            key = key.replace('.', '_');
            FindIterable<Document> result = propsFS.find(Filters.exists(key));
            if (result != null) {
                if (result.first() != null) {
                    return result.first().getString(key);
                }
            }
        } catch (Throwable t) {
            log.error("Something bad happened", t);
        }
        return defaultValue;
    }

    @SuppressWarnings("deprecation")
    @Override
    public String spoolStream(InputStream blob) throws Exception {
        boolean enableSpoolReuse = true;
        try {
            GridFSInputFile temp = blobFS.createFile(blob, true);
            String spoolName = UUID.randomUUID().toString();
            temp.setFilename(spoolName);
            temp.saveChunks();
            spoolName = temp.getMD5();
            if (enableSpoolReuse) {
                // check if it already exists
                if (containsSpool(spoolName)) {
                    log.debug("Removing duplicated spool {} with md5 {}", temp.getFilename(), spoolName);
                    temp.save();
                    removeSpooledStream(temp.getFilename());
                } else {
                    log.debug("Saving spool {}", spoolName);
                    temp.setFilename(spoolName);
                    temp.save();
                }
            } else {
                temp.save();
                return temp.getFilename();
            }

            return temp.getMD5();

        } catch (Throwable t) {
            log.error("Error storing blob", t);
        } finally {
            blob.close();
        }
        return null;
    }

    @Override
    public MimeMessage getSpooledStream(String spooledUid) throws Exception {
        return Utils.loadEML(getSpooledInputStream(spooledUid));
    }

    public InputStream getSpooledInputStream(String spooledUid) throws Exception {
        GridFSDBFile blob = blobFS.findOne(spooledUid);
        return blob.getInputStream();
    }

    public boolean containsSpool(String spooledUid) {
        try {
            return blobFS.findOne(spooledUid) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void removeSpooledStream(String spooledUid) throws Exception {
        log.debug("Removing spool {}", spooledUid);
        try {
            blobFS.remove(spooledUid);
        } catch (Throwable t) {
            log.error("Could not delete specified spooled stream {}", spooledUid);
        }
    }

    private void removeSpooledStream(List<String> rawList) throws Exception {
        for (String rawId : rawList) {
            removeSpooledStream(rawId);
        }
    }

    @Override
    public long getSpooledStreamSize(String spooledUid) {
        log.debug("Looking for spool {}", spooledUid);
        GridFSDBFile blob = blobFS.findOne(spooledUid);
        return blob.getLength();
    }

    @Override
    public long getSpoolCount() throws Exception {
        DBCursor cursor = blobFS.getFileList();
        long count = 0;
        try {
            count = cursor.count();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            cursor.close();
        }
        return count;
    }

    @Override
    public WorkerThread cleanOrphans() throws Exception {
        WorkerThread wt = new WorkerThread(StorageIf.RAWCLEAN) {

            @Override
            public void run() {
                setProgress(0);
                int issues = 0;
                long count = 0;
                try {
                    long totalCount = getSpoolCount() + getMailCount(BlueboxMessage.State.ANY);
                    log.info("Looking for orphaned blobs");
                    DBCursor cursor = blobFS.getFileList();
                    while (cursor.hasNext()) {
                        GridFSDBFile blob = (GridFSDBFile) cursor.next();
                        if (!spoolReferenced(blob.getFilename())) {
                            issues++;
                            log.debug("Deleted orphaned spooled message {} ", issues);
                            setStatus("Deleted orphaned spooled message " + issues);
                            removeSpooledStream(blob.getFilename());
                        }
                        setProgress((int) ((100 * count++) / totalCount) / 2);
                    }
                    log.info("Finished looking for orphaned blobs (found " + issues + ")");

                    log.info("Looking for orphaned messages");
                    List<LiteMessage> list = listMailLite(null, BlueboxMessage.State.ANY, 0, (int) getMailCount(BlueboxMessage.State.ANY), BlueboxMessage.RECEIVED, true);

                    for (LiteMessage m : list) {
                        setProgress((int) ((100 * count++) / totalCount) / 2);
                        try {
                            if (!containsSpool(m.getRawIdentifier())) {
                                // delete this mail entry
                                delete(m.getIdentifier());
                                issues++;
                                setStatus("Deleting orphaned mail entry (" + issues + ")");
                                log.info("Deleting orphaned mail entry ({})", issues);
                            }
                        } catch (Throwable t) {
                            log.warn("Issue with mail entry", t);
                            // delete it anyway
                            delete(m.getIdentifier());
                            issues++;
                            setStatus("Deleting orphaned mail entry (" + issues + ")");
                            log.info("Deleting orphaned mail entry ({})", issues);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    //					cursor.close();
                    setProgress(100);
                    setStatus(issues + " issues fixed");
                    log.info("Finished looking for orphaned messages");
                }
            }
        };
        return wt;
    }

    /*
     * Return true if there are mail entries referencing this blob, else return false;
     */
    private boolean spoolReferenced(String spoolUid) {
        FindIterable<Document> rawres = mailFS.find(Filters.eq(StorageIf.Props.RawUid.name(), spoolUid));
        return rawres.first() != null;
    }

    @Override
    public Date getUTCTime() {
        //return Utils.getUTCCalendar().getTime();
        final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        Date dateToReturn = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

        try {
            dateToReturn = dateFormat.parse(utcTime);
        } catch (ParseException e) {
            log.error("Problem getting utc time", e);
        }

        return dateToReturn;
    }

    private FindIterable<Document> listMailCommon(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception {
        BasicDBObject query = new BasicDBObject();
        if (state != BlueboxMessage.State.ANY)
            query.append(StorageIf.Props.State.name(), state.ordinal());
        if ((inbox != null) && (inbox.getFullAddress().length() > 0)) {
            query.append(StorageIf.Props.Inbox.name(), inbox.getAddress());
        } else {
            // we are doing a wildcard search, so don't show the hidden mails
            query.append(BlueboxMessage.HIDEME, new BasicDBObject("$not", new BasicDBObject("$eq", true)));
        }
        int sortBit;
        if (ascending) sortBit = 1;
        else sortBit = -1;
        // limit search results t0 5000 entries
        if ((count < 0) || (count > 5000))
            count = 5000;
        log.debug("listMailCommon query {}", query);
        return mailFS.find(query).sort(new BasicDBObject(orderBy, sortBit)).skip(start).limit(count);
    }

    @Override
    public List<LiteMessage> listMailLite(InboxAddress inbox, State state,
                                          int start, int count, String orderBy, boolean ascending)
            throws Exception {
        List<LiteMessage> results = new ArrayList<LiteMessage>();
        if (count <= 0) return results;
        MongoCursor<Document> cursor = listMailCommon(inbox, state, start, count, orderBy, ascending).iterator();
        try {
            while (cursor.hasNext()) {
                Document dbo = cursor.next();
                try {
                    JSONObject m = loadMessageJSON(dbo);
                    results.add(new LiteMessage(m));
                } catch (Throwable t) {
                    log.error("Error loading message {}", t);
                }
            }
        } catch (Throwable t) {
            log.error("Problem listing mail", t);
        } finally {
            cursor.close();
        }
        return results;
    }

    @Override
    public String getDBOString(Object dbo, String key, String def) {
        try {
            Document doc = (Document) dbo;
            if (doc.containsKey(key))
                return doc.getString(key);
        } catch (Throwable t) {
            log.warn("Problem getting key {}", key);
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    public List<String> getDBOArray(Object dbo, String key) {

        Document doc = (Document) dbo;
        if (doc.containsKey(key)) {
            return (List<String>) doc.get(key);
        }
        return new ArrayList<String>();
    }

    @Override
    public int getDBOInt(Object dbo, String key, int def) {
        Document doc = (Document) dbo;
        return doc.getInteger(key, def);
    }

    @Override
    public boolean getDBOBoolean(Object dbo, String key, boolean def) {
        Document doc = (Document) dbo;
        return doc.getBoolean(key, def);
    }

    @Override
    public long getDBOLong(Object dbo, String key, long def) {
        Document doc = (Document) dbo;
        if (doc.containsKey(key)) {
            try {
                return doc.getLong(key);
            } catch (Throwable t) {
                return (long) doc.getInteger(key);
            }
        }
        return def;
    }

    @Override
    public Date getDBODate(Object dbo, String key, Date def) {
        Document doc = (Document) dbo;
        if (doc.containsKey(key))
            return doc.getDate(key);
        return def;
    }

    @Override
    public Object[] search(String querystr, SearchFields fields, int start, int count, SortFields orderBy, boolean ascending) {
        querystr = querystr.toLowerCase();
        if (querystr == "*")
            querystr = "";
        Bson query = null;
        switch (fields) {
            case INBOX:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.INBOX, querystr),
                        Filters.ne(BlueboxMessage.HIDEME, true)
                );
                break;
            case SUBJECT:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.SUBJECT, Pattern.compile(querystr, Pattern.CASE_INSENSITIVE))
                );
                break;
            case TEXT_BODY:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.TEXT_BODY, querystr.toLowerCase())
                );
                break;
            case HTML_BODY:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.HTML_BODY, querystr.toLowerCase())
                );
                break;
            case BODY:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.or(Filters.regex(BlueboxMessage.HTML_BODY, querystr.toLowerCase()), Filters.regex(BlueboxMessage.TEXT_BODY, querystr.toLowerCase()))
                );
                break;
            case FROM:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.FROM, querystr.toLowerCase())
                );
                break;
            case RECIPIENT:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.RECIPIENT, Pattern.compile(querystr, Pattern.CASE_INSENSITIVE)),
                        Filters.ne(BlueboxMessage.HIDEME, true)
                );
                break;
            case RECIPIENTS:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.regex(BlueboxMessage.RECIPIENT, Pattern.compile(querystr, Pattern.CASE_INSENSITIVE)),
                        Filters.ne(BlueboxMessage.HIDEME, true)
                );
                break;
            case ANY:
            default:
                query = Filters.and(
                        Filters.eq(BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()),
                        Filters.ne(BlueboxMessage.HIDEME, true),
                        Filters.or(
                                Filters.regex(BlueboxMessage.INBOX, querystr),
                                Filters.regex(BlueboxMessage.SUBJECT, Pattern.compile(querystr, Pattern.CASE_INSENSITIVE)),
                                Filters.regex(BlueboxMessage.FROM, querystr),
                                Filters.regex(BlueboxMessage.RECIPIENT, querystr),
                                Filters.regex(BlueboxMessage.HTML_BODY, querystr.toLowerCase()),
                                Filters.regex(BlueboxMessage.TEXT_BODY, querystr.toLowerCase()))
                );
        }
        Bson sortOrder = null;
        String sortKey;
        switch (orderBy) {
            case SORT_RECEIVED:
                sortKey = BlueboxMessage.RECEIVED;
                break;
            case SORT_SIZE:
                sortKey = BlueboxMessage.SIZE;
                break;
            default:
                sortKey = BlueboxMessage.RECEIVED;
        }
        if (ascending) {
            sortOrder = Sorts.ascending(sortKey);
        } else {
            sortOrder = Sorts.descending(sortKey);
        }
        FindIterable<Document> result = mailFS.find(query).sort(sortOrder).skip(start).limit(count);
        List<Document> res = new ArrayList<Document>();
        for (Document d : result) {
            res.add(d);
        }
        return res.toArray();
    }

}
