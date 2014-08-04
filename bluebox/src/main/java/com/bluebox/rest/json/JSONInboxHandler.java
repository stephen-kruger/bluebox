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
		// /bluebox/rest/json/inbox/bluemail%20team%20%3Cbluemail@us.xxx.com%3E/undefined/
		// /bluebox/rest/json/inbox/Stephen_johnson/Iceland/XXX%25XXXIE/undefined/
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

		// check if State was specified
		BlueboxMessage.State state = extractState(req.getRequestURI(),JSON_ROOT);

		// check sort order, which comes in a strange format "sort(-Subject)=null"
//		String orderBy = BlueboxMessage.RECEIVED;
//		boolean ascending = false;
//		String n;
//		for (
//		@SuppressWarnings("unchecked")
//		Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
//			n = names.nextElement();
//			if (n.contains("sort")) {
//				if (n.contains("-")) {
//					ascending = false;
//				}
//				else {
//					ascending = true;
//				}
//			}
//			if (n.contains(BlueboxMessage.RECEIVED)) {
//				orderBy = BlueboxMessage.RECEIVED;
//				break;
//			}
//			if (n.contains(BlueboxMessage.FROM)) {
//				orderBy = BlueboxMessage.FROM;
//				break;
//			}
//			if (n.contains(BlueboxMessage.SUBJECT)) {
//				orderBy = BlueboxMessage.SUBJECT;
//				break;
//			}
//			if (n.contains(BlueboxMessage.SIZE)) {
//				orderBy = BlueboxMessage.SIZE;
//				break;
//			}			
//		}
//
//		// process the paging params "Range: items=0-24"
//		String contentHeader = req.getHeader("Range");
//		int first = getStart(contentHeader);
//		int last = getEnd(contentHeader);
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
		if (uri.indexOf(BlueboxMessage.State.NORMAL.toString())>=0) {
			return BlueboxMessage.State.NORMAL;
		}
		if (uri.indexOf(BlueboxMessage.State.DELETED.toString())>=0) {
			return BlueboxMessage.State.DELETED;
		}
		if (uri.indexOf(BlueboxMessage.State.ANY.toString())>=0) {
			return BlueboxMessage.State.ANY;
		}
		return BlueboxMessage.State.NORMAL;
	}

//	private int getStart(String contentHeader) {
//		try {
//			// items=0-24, return 0
//			int s = contentHeader.indexOf('=')+1;
//			int e = contentHeader.indexOf("-", s);
//			return Integer.parseInt(contentHeader.substring(s,e));
//		}
//		catch (Throwable t) {
//			log.warning("Invalid Content header :"+contentHeader);
//			return 0;
//		}
//	}
//
//	private int getEnd(String contentHeader) {
//		try {
//			// items=0-24, return 24
//			int s = contentHeader.indexOf('-')+1;
//			int e = contentHeader.length();
//			return Integer.parseInt(contentHeader.substring(s,e));
//		}
//		catch (Throwable t) {
//			log.warning("Invalid Content header :"+contentHeader);
//			return Integer.MAX_VALUE;
//		}
//	}

}
