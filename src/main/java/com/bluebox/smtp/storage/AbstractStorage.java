package com.bluebox.smtp.storage;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchFactory;
import com.bluebox.search.SearchIf;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage.State;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public abstract class AbstractStorage implements StorageIf {
    public static final String DB_NAME = "bluebox_db";
    //	public static long MAX_SPOOL_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(AbstractStorage.class);

    public static boolean mongoDetected() {
        try {
            log.info("Checking for MongoDB instance");
            Socket socket = new Socket(); // Unconnected socket, with the  system-default type of SocketImpl.
            InetSocketAddress endPoint = new InetSocketAddress(Config.getInstance().getString(Config.BLUEBOX_STORAGE_HOST), 27017);
            socket.connect(endPoint, 100);
            socket.close();
            return true;
        } catch (Throwable t) {
            log.debug("Mongo not detected");
        }
        return false;
    }

    @Override
    public void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer, int start, int count, String orderBy, boolean ascending, Locale locale) throws Exception {
        List<LiteMessage> mail = listMailLite(inbox, state, start, count, orderBy, ascending);
        int index = 0;
        writer.write("[");
        for (LiteMessage message : mail) {
            writer.write(message.prettyJSON(locale));
            if ((index++) < mail.size() - 1) {
                writer.write(",");
            }
        }
        writer.write("]");
        writer.flush();
    }

    @Override
    public abstract List<LiteMessage> listMailLite(InboxAddress inbox, State state, int start, int count, String orderBy, boolean ascending) throws Exception;

    public abstract String getDBOString(Object dbo, String key, String def);

    public abstract int getDBOInt(Object dbo, String key, int def);

    public abstract long getDBOLong(Object dbo, String key, long def);

    public abstract boolean getDBOBoolean(Object dbo, String key, boolean def);

    public abstract Date getDBODate(Object dbo, String key, Date def);

    public abstract List<String> getDBOArray(Object dbo, String key);
    //	public abstract InputStream getDBORaw(Object dbo, String key);

    public BlueboxMessage loadMessage(Object dbo) throws Exception {
        JSONObject json = loadMessageJSON(dbo);
        return new BlueboxMessage(json, getSpooledStream(json.getString(BlueboxMessage.RAWUID)));
    }

    /*
     * Light-weight method of loading only the extracted properties for a message
     * to allow efficient listing of inbox contents without re-loading the entire MimeMessage.
     */
    public JSONObject loadMessageJSON(Object dbo) throws Exception {
        JSONObject message = new JSONObject();
        message.put(BlueboxMessage.UID, getDBOString(dbo, BlueboxMessage.UID, UUID.randomUUID().toString()));
        try {
            message.put(BlueboxMessage.FROM, new JSONArray(getDBOArray(dbo, BlueboxMessage.FROM)));
        } catch (JSONException je) {
            log.warn("Fixing bum FROM field {}", je.getMessage());
            // hack to fix this error, not sure where it comes from
            //org.codehaus.jettison.json.JSONException: Expected a ',' or ']' at character 37 of ["PPAUser.20111201.120331 TestUser" <no-reply@servsvt1.xxx.yyy.zzz.com>]
            List<String> res = getDBOArray(dbo, BlueboxMessage.FROM);
            JSONArray ja = new JSONArray();
            for (String s : res) {
                s = s.substring(1, s.length() - 1);
                ja.put(s.substring(1, s.length() - 1));
            }
            message.put(BlueboxMessage.FROM, ja);
        }
        message.put(BlueboxMessage.RAWUID, getDBOString(dbo, BlueboxMessage.RAWUID, ""));
        message.put(BlueboxMessage.SUBJECT, Utils.decodeRFC2407(StringEscapeUtils.unescapeJava(getDBOString(dbo, BlueboxMessage.SUBJECT, ""))));
        // don't ever need the text and html content, it's only used for searching
        //		message.put(BlueboxMessage.HTML_BODY,getDBOString(dbo,BlueboxMessage.HTML_BODY,""));
        //		message.put(BlueboxMessage.TEXT_BODY,getDBOString(dbo,BlueboxMessage.TEXT_BODY,""));
        message.put(BlueboxMessage.RECIPIENT, getDBOString(dbo, BlueboxMessage.RECIPIENT, ""));
        message.put(BlueboxMessage.RECEIVED, getDBODate(dbo, BlueboxMessage.RECEIVED, getUTCTime()).getTime());
        message.put(BlueboxMessage.STATE, getDBOInt(dbo, BlueboxMessage.STATE, BlueboxMessage.State.NORMAL.ordinal()));
        message.put(BlueboxMessage.INBOX, getDBOString(dbo, BlueboxMessage.INBOX, "bluebox@bluebox.com"));
        message.put(BlueboxMessage.SIZE, getDBOLong(dbo, BlueboxMessage.SIZE, 0));
        message.put(BlueboxMessage.HIDEME, getDBOBoolean(dbo, BlueboxMessage.HIDEME, false));
        return message;
    }

    @Override
    public BlueboxMessage store(String from, InboxAddress recipient, Date received, MimeMessage bbmm, String spooledUid) throws Exception {
        String uid = UUID.randomUUID().toString();
        BlueboxMessage message = new BlueboxMessage(uid, recipient);
        message.setBlueBoxMimeMessage(from, recipient, received, bbmm, spooledUid);
        // now store in underlying db
        store(message.toJSON(), spooledUid);
        return message;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, "");
    }

    @Override
    public void setLongProperty(String key, long value) {
        setProperty(key, Long.toString(value));
    }

    @Override
    public long getLongProperty(String key) {
        return getLongProperty(key, 0);
    }

    @Override
    public long getLongProperty(String key, long defaultValue) {
        return Long.parseLong(getProperty(key, Long.toString(defaultValue)));
    }

    @Override
    public boolean hasProperty(String key) {
        String r = Long.toString(new Random().nextLong());
        return !getProperty(key, r).equals(r);
    }

    @Override
    public WorkerThread runMaintenance() throws Exception {
        WorkerThread wt = new WorkerThread(Inbox.DBMAINTENANCE_WORKER) {

            @Override
            public void run() {
                int issues = 0;
                setProgress(50);
                // check all messages are indexed
                try {
                    SearchIf indexer = SearchFactory.getInstance();
                    LiteMessageIterator messages = new LiteMessageIterator(null, BlueboxMessage.State.NORMAL);
                    while (messages.hasNext()) {
                        LiteMessage msg = messages.next();
                        if (!indexer.containsUid(msg.getIdentifier())) {
                            log.warn("Message not indexed " + msg.getIdentifier());
                            indexer.indexMail(retrieve(msg.getIdentifier()), false);
                            issues++;
                        }
                        setProgress(messages.getProgress());
                        setStatus(" Indexed " + issues + " messages");
                    }
                    indexer.commit(true);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    setProgress(100);
                    setStatus("Completed, with " + issues + " unindexed messages commited to search indexes");
                }
            }

        };
        return wt;
    }


}
