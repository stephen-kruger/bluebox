package com.bluebox.rest.json;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONHeadersMessageHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONHeadersMessageHandler.class);
	public static final String JSON_ROOT = "rest/json/inbox/headers";

	/*
	 * REST rest/json/inbox/raw/26e3a411-f456-4c5f-a531-3b73f43ecf7f
	 */
	public void doGetHeadersDetail(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
//			String uri = req.getRequestURI();
			String uid = extractFragment(req.getRequestURI(), JSON_ROOT, 0);
			//uri.substring(uri.lastIndexOf("/")+1,uri.length());
			log.debug("Serving raw message for {}",uid);
			BlueboxMessage message = inbox.retrieve(uid);
			MimeMessage bbm = message.getBlueBoxMimeMessage();
			resp.setContentType("text/plain");
			resp.setCharacterEncoding(Utils.UTF8);
			PrintWriter pw = new PrintWriter(resp.getOutputStream());
			@SuppressWarnings("rawtypes")
			Enumeration headers = bbm.getAllHeaders();
			while (headers.hasMoreElements()) {
				Header header = (Header) headers.nextElement();
				pw.print("<b>");
				pw.print(header.getName());
				pw.print("</b>");
				pw.print("=");
				pw.print(StringEscapeUtils.escapeHtml(header.getValue()));
				pw.println("<br/>");
			}
			pw.close();
			
		}
		catch (Throwable t) {
			log.error(t.getMessage());
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

}
