package com.bluebox.rest.json;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONMessageUtilHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONMessageUtilHandler.class);
	public static final String JSON_ROOT = "rest/json/messageutils";
	public static final String LINKS = "links";

	public void doGet(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String method = extractFragment(req.getRequestURI(), JSON_ROOT, 0);
		if (LINKS.equals(method)) {
			doGetLinks(inbox, req, resp);
		}
	}

	/*
	 * REST rest/json/messageutils/26e3a411-f456-4c5f-a531-3b73f43ecf7f/links
	 */
	public void doGetLinks(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uid = extractFragment(req.getRequestURI(), JSON_ROOT, 1);
			log.debug("Serving links for {}",uid);
			BlueboxMessage message = inbox.retrieve(uid);
			JSONArray links = getLinks(message.getHtml(req));
			resp.setContentType(JSON_CONTENT_TYPE);
			resp.setCharacterEncoding(Utils.UTF8);
			JSONObject result = new JSONObject();
			result.put(LINKS, links);
			resp.getWriter().print(result.toString());

		}
		catch (Throwable t) {
			log.error("Problem serving links",t);
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

	public JSONArray getLinks(String html) throws IOException {
		JSONArray res = new JSONArray();
		Document doc = Jsoup.parse(html);
		Elements links = doc.select("a[href]");
		for (Element link : links) {
			try {
				JSONObject currLink = new JSONObject();
				currLink.put("text", link.text());
				currLink.put("data", link.data());
				currLink.put("href", link.attr("abs:href"));
				res.put(currLink);
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return res;
	}

}
