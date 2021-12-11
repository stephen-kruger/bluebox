package com.bluebox.smtp.storage;

import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.search.SearchUtils.SortFields;
import com.bluebox.smtp.InboxAddress;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public interface StorageIf {

    String RAWCLEAN = "rawclean";

    void start() throws Exception;

    void stop() throws Exception;

    BlueboxMessage store(String from, InboxAddress recipient, Date received, MimeMessage bbmm, String spooledUid) throws Exception;

    /*
     * Implementations must ensure all the fields in the Props object are persisted.
     */
    void store(JSONObject props, String spooledUid) throws Exception;

    BlueboxMessage retrieve(String uid) throws Exception;

    boolean contains(String uid);

    void deleteAll() throws Exception;

    long getMailCount(BlueboxMessage.State state)
            throws Exception;

    long getMailCount(InboxAddress inbox, BlueboxMessage.State state)
            throws Exception;

    List<BlueboxMessage> listMail(InboxAddress inbox, BlueboxMessage.State state,
                                  int start, int count, String orderBy, boolean ascending)
            throws Exception;

    List<LiteMessage> listMailLite(InboxAddress inbox, BlueboxMessage.State state, int start, int count, String orderBy, boolean ascending) throws Exception;

    /*
     * This method writes directly into the server stream, isntead of creating the entire structure in memory, and
     * then sending it over the wire at the end.
     * It should allow for larger streams to be sent, more efficiently.
     */
    void listInbox(InboxAddress inbox, BlueboxMessage.State state, Writer writer,
                   int start, int count, String orderBy, boolean ascending, Locale locale)
            throws Exception;

    void setState(String uid, BlueboxMessage.State state)
            throws Exception;

    void logError(String title, InputStream content);

    void logError(String title, String content);

    int logErrorCount();

    void logErrorClear();

    JSONArray logErrorList(int start, int count);

    String logErrorContent(String id);

    /*
     * Return a JSONObject with the most active inbox as follows :
     * {
     * 	Count : <value>,
     * 	Inbox : <email>
     * }
     */
    JSONObject getMostActiveInbox();

    /*
     * Return a JSONObject with the most active sender as follows :
     * {
     * 	Count : <value>,
     * 	Sender : <email>
     * }
     */
    JSONObject getMostActiveSender();

    void delete(String uid, String rawId) throws Exception;

    void delete(List<LiteMessage> bulkList) throws Exception;

    /*
     * Called to perform housekeep tasks on the underlying storage, such as rebuilding indexes.
     */
    WorkerThread runMaintenance() throws Exception;

    /*
     * Return a JSON view of the count of email received per day of month
     * {
     *  "1" : 200,
     *  ...
     *  "31" : 100
     * }
     */
    JSONObject getCountByDay();

    JSONObject getCountByHour();

    JSONObject getCountByDayOfWeek();

    /*
     * Return number of mails per hour
     */
    JSONObject getMPH(InboxAddress inbox);

    void setProperty(String key, String value);

    void setLongProperty(String key, long value);

    String getProperty(String key);

    String getProperty(String key, String defaultValue);

    long getLongProperty(String key);

    long getLongProperty(String key, long defaultValue);

    boolean hasProperty(String key);

    String spoolStream(InputStream is) throws Exception;

    MimeMessage getSpooledStream(String spooledUid) throws Exception;

    void removeSpooledStream(String spooledUid) throws Exception;

    long getSpooledStreamSize(String spooledUid) throws Exception;

    long getSpoolCount() throws Exception;

    /*
     * Remove any spooled messages which are not referenced by a mailbox entry, and any mail entries which have dangling spool
     * references.
     */
    WorkerThread cleanOrphans() throws Exception;

    /*
     * Return the current time
     */
    Date getUTCTime();

    // time functions are db specific, so expose these mthods here

    Object[] search(String querystr, SearchFields fields, int start, int count, SortFields orderBy, boolean ascending) throws Exception;

    enum Props {Uid, RawUid, Inbox, Recipient, Sender, Subject, Received, State, Size, Hideme}

}