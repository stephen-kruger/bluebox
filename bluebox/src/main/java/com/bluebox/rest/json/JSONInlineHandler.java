package com.bluebox.rest.json;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONInlineHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/inline";
	
	/*
	 * REST rest/json/inline/<name>/<uid>
	 */
	public void doGetInlineAttachment(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);

		try {
			String uri = req.getRequestURI();
			String uid = extractFragment(uri,1);
			String name = extractFragment(uri,0);
			log.info("Serving inline attachment for uid "+uid+" with name "+name);
			BlueboxMessage message = inbox.retrieve(uid);
			message.writeInlineAttachment(name, resp);
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
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
