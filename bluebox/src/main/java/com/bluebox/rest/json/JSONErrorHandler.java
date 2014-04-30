package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;

import com.bluebox.smtp.Inbox;

public class JSONErrorHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/errors";
	public static final String JSON_DETAIL_ROOT = "rest/json/errors/detail";

	public void doGet(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		
		// process the paging params "Range: items=0-24"
		String contentHeader = req.getHeader("Range");
		int first = getStart(contentHeader);
		int last = getEnd(contentHeader);

		try {
			// tell the grid how many items we have
			long totalCount = inbox.errorCount();
			resp.setHeader("Content-Range", "items "+first+"-"+last+"/"+totalCount);//Content-Range: items 0-24/66
			log.info("Sending JSON error view first="+first+" last="+last);
			Writer writer = resp.getWriter();
			JSONArray result = inbox.errorCount(first, last-first+1);
			writer.write(result.toString());
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}

	private int getStart(String contentHeader) {
		try {
			// items=0-24, return 0
			int s = contentHeader.indexOf('=')+1;
			int e = contentHeader.indexOf("-", s);
			return Integer.parseInt(contentHeader.substring(s,e));
		}
		catch (Throwable t) {
			log.warning("Invalid Content header :"+contentHeader);
			return 0;
		}
	}

	private int getEnd(String contentHeader) {
		try {
			// items=0-24, return 24
			int s = contentHeader.indexOf('-')+1;
			int e = contentHeader.length();
			return Integer.parseInt(contentHeader.substring(s,e));
		}
		catch (Throwable t) {
			log.warning("Invalid Content header :"+contentHeader);
			return Integer.MAX_VALUE;
		}
	}

	public void doGetDetail(Inbox instance, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String id = extractFragment(req.getRequestURI(),0);
		log.info("Serving error detail for id="+id);
		resp.setContentType("application/text");
		Writer writer = resp.getWriter();
		writer.write(Inbox.getInstance().errorDetail(id));
		writer.flush();
		writer.close();
	}

}
