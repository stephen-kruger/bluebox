package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONInboxHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/inbox";

	protected static String extractEmail(String uri, String fragment) {
		try {
			String email;
			if (uri.endsWith(fragment)) {
				log.info("No email found in uri :"+uri);
				return "";
			}
			else {
				int pos1 = uri.indexOf(fragment)+fragment.length()+1;
				int pos2 = uri.lastIndexOf('/');
				uri = uri.substring(pos1,pos2);
				pos2 = uri.lastIndexOf('/');
				email = uri.substring(0,pos2);
				return extractEmail(email);
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
			log.severe("No email specified in "+uri);
			return "";
		}
	}

	public void doGetInbox(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		long startTime = new Date().getTime();
		setDefaultHeaders(resp);
		// get the desired email
		InboxAddress inboxAddress;
		try {
			inboxAddress= new InboxAddress(extractEmail(req.getRequestURI(),JSON_ROOT));
		}
		catch (AddressException e) {
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
			log.severe(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
		log.info("Served inbox contents in for "+inboxAddress+" first="+pager.getFirst()+" last="+pager.getLast()+" in "+(new Date().getTime()-startTime)+"ms");
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
