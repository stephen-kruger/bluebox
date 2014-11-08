package com.bluebox.rest.json;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.mail.Message.RecipientType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.AbstractStorage;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONMessageHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONMessageHandler.class);
	public static final String JSON_ROOT = "rest/json/inbox/detail";
	public static final String SECURITY = "Security";

	/*
	 * REST  rest/json/inbox/detail/uid
	 */
	public void doGetMessageDetail(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		log.debug("doGetMessageDetail:{}",req.getRequestURI());
		Writer out = resp.getWriter();
		try {
			String uid = extractFragment(req.getRequestURI(),JSON_ROOT,0);
			BlueboxMessage message = inbox.retrieve(uid);
			if (message == null) {
				log.error("Could not find message with uid="+uid);
			}
			JSONObject json = message.toJSON(req.getLocale());
			
			json.put(BlueboxMessage.RECEIVED, AbstractStorage.dateToString(new Date(json.getLong(BlueboxMessage.RECEIVED)),req.getLocale()));
			// add in the TO, CC and BCC
			json.put(BlueboxMessage.TO,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.TO)));	
			json.put(BlueboxMessage.CC,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.CC)));
			json.put(BlueboxMessage.BCC,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.BCC)));
			// note: BCC can be populated if no TO or CC is detected, by using the INBOX metadata
			if ((json.getJSONArray(BlueboxMessage.TO).length()==0)&&
					(json.getJSONArray(BlueboxMessage.CC).length()==0)&&
					(json.getJSONArray(BlueboxMessage.BCC).length()==0)) {
				JSONArray bcc = new JSONArray();
				bcc.put(message.getInbox().getFullAddress());
				json.put(BlueboxMessage.BCC,bcc);
			}
			json = securityScan(req,message,json);
			out.write(json.toString());
			out.flush();
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
		}
		resp.flushBuffer();
	}

	private JSONObject securityScan(HttpServletRequest request, BlueboxMessage message, JSONObject json) {
		ResourceBundle mailDetailsResource = ResourceBundle.getBundle("mailDetails",request.getLocale());
		ServletContext context = request.getSession().getServletContext();
		try {
			// perform security scan on html content
			InputStream p = context.getResourceAsStream("WEB-INF/antisamy-anythinggoes-1.4.4.xml");
			if (p==null)
				p = new FileInputStream("src/main/webapp/WEB-INF/antisamy-anythinggoes-1.4.4.xml");

			// scan html for malicious content
			String html = message.getHtml(request);
			Policy policy = Policy.getInstance(p);
			AntiSamy as = new AntiSamy();
			CleanResults cr = as.scan(html, policy);
			StringBuffer sec = new StringBuffer();
			
			int count = 1;
			for (String error : cr.getErrorMessages())
				sec.append((count++)+"(body) "+error+"\n");
			json.put(BlueboxMessage.HTML_BODY, cr.getCleanHTML());

			// scan subject for malicious content
			cr = as.scan(message.getSubject(), policy);
			json.put(BlueboxMessage.SUBJECT, cr.getCleanHTML());
			for (String error : cr.getErrorMessages())
				sec.append((count++)+"(subject) "+error+"\n");

			// don't scan text body for malicious content, it renders as text only
			json.put(BlueboxMessage.TEXT_BODY, message.getText());
			
			StringBuffer title = new StringBuffer();
			title.append(MessageFormat.format(mailDetailsResource.getString("scantitle"), count-1)+"\n");
			title.append(mailDetailsResource.getString("scantitleunderline")+"\n");
			json.put(SECURITY, title.toString()+sec.toString());

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return json;
	}

	///bluebox/rest/json/inbox/f0f3bd84-0ccf-4cf1-839c-22d43f9e20c4
	public void doDelete(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uidList = extractFragment(req.getRequestURI(),JSON_ROOT,0);
			StringTokenizer uidArray = new StringTokenizer(uidList,",");
			String uid;
			while (uidArray.hasMoreTokens()) {
				uid = uidArray.nextToken();
				BlueboxMessage msg = inbox.retrieve(uid);
				if (msg.getState()==BlueboxMessage.State.DELETED) {
					inbox.delete(uid);
				}
				else {
					inbox.softDelete(uid);
				}
			}
			JSONObject result = new JSONObject();
			result.put("message", "ok");
			Writer out = resp.getWriter();
			out.write(result.toString(3));
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
		}
	}

}
