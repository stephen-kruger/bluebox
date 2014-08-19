package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONErrorHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONErrorHandler.class);
	public static final String JSON_ROOT = "rest/json/errors";
	public static final String JSON_DETAIL_ROOT = "rest/json/errors/detail";

	public void doGet(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		
		DojoPager pager = new DojoPager(req,BlueboxMessage.RECEIVED);

		try {
			// tell the grid how many items we have
			long totalCount = inbox.errorCount();
			pager.setRange(resp, totalCount);
			log.info("Sending JSON error view first="+pager.getFirst()+" last="+pager.getLast());
			Writer writer = resp.getWriter();
			JSONArray result = inbox.errorCount(pager.getFirst(), pager.getCount());
			writer.write(result.toString());
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error("Problem serving errors",t);
			t.printStackTrace();
		}
		resp.flushBuffer();
	}

	public void doGetDetail(Inbox instance, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String id = extractFragment(req.getRequestURI(),JSON_ROOT,0);
		log.info("Serving error detail for id="+id);
		resp.setContentType("application/text");
		Writer writer = resp.getWriter();
		writer.write(Inbox.getInstance().errorDetail(id));
		writer.flush();
		writer.close();
	}

}
