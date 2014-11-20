package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;

public class JSONSPAMHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONSPAMHandler.class);
	public static final String JSON_ROOT = "rest/json/inbox/spam";

	public WorkerThread doDelete(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uidList = extractFragment(req.getRequestURI(),JSON_ROOT,0);
			log.info("Marking spam {}",uidList);
			StringTokenizer uidArray = new StringTokenizer(uidList,",");
			String uid;
//			while (uidArray.hasMoreTokens()) {
				uid = uidArray.nextToken();
				log.debug("Marking as SPAM:{}",uid);
				WorkerThread wt = inbox.toggleSpam(uid);
//			}
			JSONObject result = new JSONObject();
			result.put("message", "ok");
			Writer out = resp.getWriter();
			out.write(result.toString(3));
			return wt;
		}
		catch (Throwable t) {
			log.error(t.getMessage(),t);
			t.printStackTrace();
			try {
				JSONObject error = new JSONObject();
				error.put("message", t.getMessage());
				resp.sendError(404, error.toString(3));
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

}
