package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONInboxHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONInboxHandler.class);
	public static final String JSON_ROOT = "rest/json/inbox";

	public void doGetInbox(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		long startTime = new Date().getTime();
		setDefaultHeaders(resp);
		// get the desired email
		InboxAddress inboxAddress;
		try {
			String emailStr = extractEmail(extractFragment(req.getRequestURI(),JSON_ROOT,0));
			inboxAddress = new InboxAddress(emailStr);
		}
		catch (Throwable e) {
			inboxAddress = null;
		}

		// check if State was specified, else default to NORMAL
		BlueboxMessage.State state = extractState(req.getRequestURI(),JSON_ROOT);
		DojoPager pager = new DojoPager(req,BlueboxMessage.RECEIVED);
		try {
			// tell the grid how many items we have
			long totalCount = inbox.getMailCount(inboxAddress, state);
			pager.setRange(resp, totalCount);
			Writer writer = resp.getWriter();
			inbox.listInbox(inboxAddress, state, writer, pager.getFirst(), pager.getCount(), pager.getOrderBy().get(0), pager.isAscending(0), resp.getLocale());
			writer.flush();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			t.printStackTrace();
		}
		resp.flushBuffer();
		log.info("Served inbox contents in for {} first={} last={} in {}ms",inboxAddress,pager.getFirst(),pager.getLast(),(new Date().getTime()-startTime));
	}

	private BlueboxMessage.State extractState(String uri, String jsonRoot) {
		try {
			return BlueboxMessage.State.values()[Integer.parseInt(extractFragment(uri,JSON_ROOT, 1))];
		}
		catch (Throwable t) {
			return BlueboxMessage.State.NORMAL;
		}
	}
}
