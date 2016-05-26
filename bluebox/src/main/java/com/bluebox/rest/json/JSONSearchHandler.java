package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.search.SearchUtils;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONSearchHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONSearchHandler.class);
	public static final String JSON_ROOT = "rest/json/search";
	
	public void doSearchInbox(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		// get the desired email
//		String search = extractSearch(req.getRequestURI(),JSON_ROOT);
		String search = extractFragment(req.getRequestURI(), JSON_ROOT, 1);
		String searchScopeStr = extractFragment(req.getRequestURI(), JSON_ROOT, 0);
		// check sort order, which comes in a strange format "sort(-Subject)=null"
		SearchUtils.SortFields orderBy = SearchUtils.SortFields.SORT_RECEIVED;
		boolean ascending = true;
		String n;
		for (
		Enumeration<String> names = req.getParameterNames(); names.hasMoreElements();) {
			n = names.nextElement();
			if (n.contains("-")) {
				ascending = false;
			}
			if (n.contains(BlueboxMessage.RECEIVED)) {
				orderBy = SearchUtils.SortFields.SORT_RECEIVED;
				break;
			}
//			if (n.contains(BlueboxMessage.FROM)) {
//				orderBy = SearchUtils.SearchFields.FROM;
//				break;
//			}
//			if (n.contains(BlueboxMessage.SUBJECT)) {
//				orderBy = SearchUtils.SearchFields.SUBJECT;
//				break;
//			}
			if (n.contains(BlueboxMessage.SIZE)) {
				orderBy = SearchUtils.SortFields.SORT_SIZE;
				break;
			}			
		}

		// process the paging params "Range: items=0-24"
		String contentHeader = req.getHeader("Range");
		int first = getStart(contentHeader);
		int last = getEnd(contentHeader);
			
		try {
			// tell the grid how many items we have
			log.info("Sending JSON search view for {} first={} last={} orderby={}",search,first,last,orderBy.name());
			Writer writer = resp.getWriter();
			SearchFields searchScope;
			try {
				searchScope = SearchUtils.SearchFields.valueOf(searchScopeStr);
			}
			catch (Throwable t) {
				log.error("Invalid search scope :{}",searchScopeStr);
				searchScope = SearchUtils.SearchFields.ANY;
			}
			long totalCount = inbox.searchInbox(search, writer, first, last-first, searchScope , orderBy, ascending);
			log.debug("Total result set was length {}",totalCount);
			resp.setHeader("Content-Range", "items "+first+"-"+last+"/"+totalCount);//Content-Range: items 0-24/66
			writer.flush();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
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
			log.warn("Invalid Content header :{}",contentHeader);
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
			log.warn("Invalid Content header :{}",contentHeader);
			return Integer.MAX_VALUE;
		}
	}

}
