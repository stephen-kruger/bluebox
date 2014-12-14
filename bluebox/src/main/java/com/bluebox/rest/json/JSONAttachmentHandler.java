package com.bluebox.rest.json;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONAttachmentHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONAttachmentHandler.class);
	public static final String JSON_ROOT = "rest/json/inbox/attachment";
	/*
	 * REST rest/json/inbox/attachment/26e3a411-f456-4c5f-a531-3b73f43ecf7f/1
	 */
	public void doGetMessageAttachment(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);

		try {
			String uri = req.getRequestURI();
			String uid = extractFragment(uri,JSON_ROOT,0);
			String index = extractFragment(uri,JSON_ROOT,1);
			String name = extractFragment(uri,JSON_ROOT,2);
			log.debug("Serving file attachment {} at index {} for message {}"+name,index,uid);
			BlueboxMessage message = inbox.retrieve(uid);
			message.writeAttachment(index, resp);
		}
		catch (Throwable t) {
			log.error("Problem loading attachment",t);
			t.printStackTrace();
			try {
				JSONObject error = new JSONObject();
				error.put("message", t.getMessage());
				resp.sendError(404, error.toString(3));
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		resp.flushBuffer();
	}

//	public String extractIndex(String uri) {
//		int start = uri.lastIndexOf('/')+1;
//		int end = uri.length();
//		return uri.substring(start,end);
//	}

}
