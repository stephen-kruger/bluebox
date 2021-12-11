package com.bluebox.search;

import com.bluebox.smtp.storage.BlueboxMessage;
import org.codehaus.jettison.json.JSONArray;

import java.io.Writer;

public interface SearchIf {

    void stop();

    Object[] search(String querystr, SearchUtils.SearchFields fields, int start, int count, SearchUtils.SortFields orderBy, boolean ascending) throws Exception;

    long searchInboxes(String search, Writer writer, int start, int count, SearchUtils.SearchFields fields, SearchUtils.SortFields orderBy, boolean ascending) throws Exception;

    void indexMail(BlueboxMessage message, boolean commit) throws Exception;

    /*
     * Basically stall the commit unless a certain timeout has been reached,
     * to prevent multiple, consecutive commits
     */
    void commit(boolean force) throws Exception;

    void deleteDoc(String uid) throws Exception;

    void deleteDoc(String value, SearchUtils.SearchFields field) throws Exception;

    void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received) throws Exception;

    void addDoc(String uid, String inbox, String from, String subject, String text, String html, String recipients, long size, long received, boolean commit) throws Exception;

    void deleteIndexes() throws Exception;

    boolean containsUid(String uid);

    JSONArray autoComplete(String hint, long start, long count);

}
