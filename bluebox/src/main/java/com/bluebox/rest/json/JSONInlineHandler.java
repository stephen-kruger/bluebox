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

public class JSONInlineHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONInboxHandler.class);
	public static final String JSON_ROOT = "rest/json/inline";
	
	/*
	 * REST rest/json/inline/<name>/<uid>
	 */
	public void doGetInlineAttachment(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);

		try {
			String uri = req.getRequestURI();
			String uid = extractFragment(uri,JSON_ROOT,0);
			String name = extractFragment(uri,JSON_ROOT,1);
			log.debug("Serving inline attachment for uid {} with name {}",uid,name);
			BlueboxMessage message = inbox.retrieve(uid);
			message.writeInlineAttachment(name, resp);
		}
		catch (Throwable t) {
			log.error("Problem serving attachment",t);
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
