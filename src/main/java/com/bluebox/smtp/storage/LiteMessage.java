package com.bluebox.smtp.storage;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LiteMessage {

    private final JSONObject data;

    public LiteMessage(JSONObject data) {
        this.data = data;
    }

    public static String dateToString(Date date, Locale locale) {
        return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale).format(date);
    }

    public String getIdentifier() throws JSONException {
        return data.getString(BlueboxMessage.UID);
    }

    public String getRawIdentifier() throws JSONException {
        return data.getString(BlueboxMessage.RAWUID);
    }

    public String getSubject() throws JSONException {
        return data.getString(BlueboxMessage.SUBJECT);
    }

    public long getSize() throws JSONException {
        return data.getLong(BlueboxMessage.SIZE);
    }

    public BlueboxMessage.State getState() throws JSONException {
        return BlueboxMessage.State.valueOf(data.getString(BlueboxMessage.STATE));
    }

//	public InboxAddress getInbox() throws JSONException {
//		return new InboxAddress(data.getString(BlueboxMessage.INBOX));		
//	}

    public Date getReceived() throws JSONException {
        return new Date(data.getLong(BlueboxMessage.RECEIVED));
    }

    /*
     * Convert the date and size to something pretty
     */
    public String prettyJSON(Locale locale) throws JSONException {
        JSONObject tmp = new JSONObject(data.toString());
        tmp.put(BlueboxMessage.RECEIVED, dateToString(new Date(data.getLong(BlueboxMessage.RECEIVED)), locale));
        long size = data.getLong(BlueboxMessage.SIZE) / 1000;
        if (size == 0)
            size = 1;
        tmp.put(BlueboxMessage.SIZE, size + "K");
        return tmp.toString();
    }
}
