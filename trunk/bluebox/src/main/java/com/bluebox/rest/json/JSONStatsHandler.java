package com.bluebox.rest.json;

/*
 * 
 * {
 * 	identifier: 'id',
 * 	label: 'name',
 * 	items: [
 * 
 * 		        { 
 * 					id: 'Overview', 
 * 					name:'Mail for stephen.johnson@ice.xxx.com', 
 * 					type:'folder',
 * 		        	children:[
 * 		        			{ 
 * 								id: 'Inbox', 
 * 								name:'Inbox (56)', 
 * 								url:'country' 
 * 							}, 
 * 		        			{ 
 * 								id: 'Trash', 
 * 								name:'Trash (43)', 
 * 								url:'country' 
 * 							}
 * 		        		] 
 * 		        }	        	
 * 			]
 * }
 */
import java.io.IOException;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONStatsHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONStatsHandler.class);
	public static final String JSON_ROOT = "rest/json/stats";
	public static final String MPH_STAT = "stats_mph";
	public static final String GLOBAL_STAT = "stats_global";
	public static final String RECENT_STAT = "stats_recent";
	public static final String ACTIVE_STAT = "stats_active";
	public static final String SENDER_STAT = "stats_sender";
	public static final String COMBINED_STAT = "stats_combined";

	public void doGet(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {

		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(GLOBAL_STAT)) {
			log.debug("Process global stat");
			doGetGlobalStats(inbox,req,resp);
			return;
		}
		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(RECENT_STAT)) {
			log.debug("Process recent stat");
			doGetRecentStats(inbox,req,resp);
			return;
		}
		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(ACTIVE_STAT)) {
			log.debug("Process active stat");
			doGetActiveStats(inbox,req,resp);
			return;
		}
		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(SENDER_STAT)) {
			log.debug("Process active senderstat");
			doGetSenderStats(inbox,req,resp);
			return;
		}
		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(COMBINED_STAT)) {
			log.debug("Process combined stat");
			doGetCombinedStats(inbox,req,resp);
			return;
		}
		if (extractFragment(req.getRequestURI(), JSON_ROOT,0).equals(MPH_STAT)) {
			log.debug("Process combined stat");
			doGetMphStats(inbox,req,resp);
			return;
		}
		log.error("Unknown request :"+req.getRequestURI());
	}

	private void doGetCombinedStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		try {
			JSONObject result = new JSONObject();
			result.put(BlueboxMessage.COUNT,inbox.getStatsGlobalCount());
			result.put("countAll",inbox.getMailCount(BlueboxMessage.State.ANY));
			result.put("recent",inbox.getStatsRecent());
			result.put("active",inbox.getStatsActiveInbox());
			result.put("sender",inbox.getStatsActiveSender());
			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
	
	private void doGetMphStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {

		setDefaultHeaders(resp);
		try {
			JSONObject result = inbox.getMPH(new InboxAddress(extractFragment(req.getRequestURI(),MPH_STAT,1)));
			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
	
	private void doGetGlobalStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		try {
			JSONObject result = new JSONObject();
			result.put(BlueboxMessage.COUNT,inbox.getStatsGlobalCount());
			result.put("countAll",inbox.getMailCount(BlueboxMessage.State.ANY));
			result.put("countError",inbox.errorCount());

			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}

	private void doGetRecentStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		try {
			JSONObject result = new JSONObject();
			result.put("recent",inbox.getStatsRecent());

			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}

	private void doGetActiveStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		try {
			JSONObject result = new JSONObject();
			result.put("active",inbox.getStatsActiveInbox());

			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
	
	private void doGetSenderStats(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		try {
			JSONObject result = new JSONObject();
			result.put("sender",inbox.getStatsActiveSender());

			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
			writer.close();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
}
