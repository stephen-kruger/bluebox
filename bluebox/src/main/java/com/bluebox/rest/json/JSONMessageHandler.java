package com.bluebox.rest.json;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONMessageHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/inbox/detail";
	public static final String SECURITY = "Security";

	/*
	 * REST  rest/json/inbox/detail/uid
	 */
	public void doGetMessageDetail(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		log.info("doGetMessageDetail");
		Writer out = resp.getWriter();
		try {
			String uid = extractUid(req.getRequestURI(),JSON_ROOT);
			BlueboxMessage message = inbox.retrieve(uid);
			JSONObject json = new JSONObject(message.toJSON(req.getLocale(),false));
			json = securityScan(req.getSession().getServletContext(),json);
			out.write(json.toString());
			out.flush();
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

	private JSONObject securityScan(ServletContext context, JSONObject json) {
		try {
			// perform security scan on html content
			InputStream p = context.getResourceAsStream("WEB-INF/antisamy-anythinggoes-1.4.4.xml");
			if (p==null)
				p = new FileInputStream("src/main/webapp/WEB-INF/antisamy-anythinggoes-1.4.4.xml");
			String html = json.getString(BlueboxMessage.HTML_BODY);
			
			System.out.println("before:"+html);
			Policy policy = Policy.getInstance(p);
			AntiSamy as = new AntiSamy();
			CleanResults cr = as.scan(html, policy);
			System.out.println("after:"+cr.getCleanHTML());
			System.out.println("errors:"+cr.getNumberOfErrors());
			StringBuffer sec = new StringBuffer();
			for (String error : cr.getErrorMessages())
				sec.append(error+"\n");
			json.put(BlueboxMessage.HTML_BODY, cr.getCleanHTML());
			json.put(SECURITY, sec.toString());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return json;
	}

	///bluebox/rest/json/inbox/f0f3bd84-0ccf-4cf1-839c-22d43f9e20c4
	public void doDelete(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uidList = extractFragment(req.getRequestURI(),0);
			StringTokenizer uidArray = new StringTokenizer(uidList,",");
			String uid;
			while (uidArray.hasMoreTokens()) {
				uid = uidArray.nextToken();
				BlueboxMessage msg = inbox.retrieve(uid);
				if (msg.getLongProperty(BlueboxMessage.STATE)==BlueboxMessage.State.DELETED.ordinal()) {
					inbox.delete(uid);
				}
				else {
					inbox.setState(uid, BlueboxMessage.State.DELETED);
				}
			}
			JSONObject result = new JSONObject();
			result.put("message", "ok");
			Writer out = resp.getWriter();
			out.write(result.toString(3));
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
	}

}
