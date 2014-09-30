package com.bluebox;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.chart.Charts;
import com.bluebox.rest.json.JSONAttachmentHandler;
import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.rest.json.JSONErrorHandler;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.rest.json.JSONInboxHandler;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.rest.json.JSONMessageHandler;
import com.bluebox.rest.json.JSONMessageUtilHandler;
import com.bluebox.rest.json.JSONRawMessageHandler;
import com.bluebox.rest.json.JSONSearchHandler;
import com.bluebox.rest.json.JSONStatsHandler;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.Inbox;

public class BlueBoxServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxServlet.class);
	private static final long serialVersionUID = 1015755960967873612L;
	public static final String VERSION = Config.getInstance().getString(Config.BLUEBOX_VERSION);
	private BlueBoxSMTPServer smtpServer;
	private Map<String,WorkerThread> workers = new HashMap<String,WorkerThread>();
	private static Date started = new Date();

	@Override
	public void init() throws ServletException {
		log.info("Initialising BlueBox "+getServletContext().getContextPath());
		Inbox inbox = Inbox.getInstance();

		log.info("Starting SMTP server");
		smtpServer = new BlueBoxSMTPServer(new SimpleMessageListenerAdapter(inbox));
		smtpServer.start();
	}
	
	@Override
	public void destroy() {
		super.destroy();
		log.info("Stopping servlet");
		smtpServer.stop();
		Inbox.getInstance().stop();
	}



	@Override
	protected void doGet(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		log.info(req.getRequestURI());
		if (req.getRequestURI().indexOf(JSONMessageHandler.JSON_ROOT)>=0){
			log.debug("doGetMessageDetail");
			new JSONMessageHandler().doGetMessageDetail(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONAutoCompleteHandler.JSON_ROOT)>=0){
			log.debug("JSONAutoCompleteHandler");
			new JSONAutoCompleteHandler().doAutoComplete(Inbox.getInstance(), req, resp);
			return;
		}	
		if (req.getRequestURI().indexOf(JSONInlineHandler.JSON_ROOT)>=0){
			log.debug("doGetInlineAttachment");
			new JSONInlineHandler().doGetInlineAttachment(Inbox.getInstance(),req,resp);
			return;
		}		
		if (req.getRequestURI().indexOf(JSONAttachmentHandler.JSON_ROOT)>=0){
			log.debug("doGetMessageAttachment");
			new JSONAttachmentHandler().doGetMessageAttachment(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONRawMessageHandler.JSON_ROOT)>=0){
			log.debug("doGetRawDetail");
			new JSONRawMessageHandler().doGetRawDetail(Inbox.getInstance(),req,resp);
			return;
		}		
		if (req.getRequestURI().indexOf(JSONInboxHandler.JSON_ROOT)>=0){
			log.debug("doGetInbox");
			new JSONInboxHandler().doGetInbox(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONSearchHandler.JSON_ROOT)>=0){
			log.debug("doSearchInbox");
			new JSONSearchHandler().doSearchInbox(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONFolderHandler.JSON_ROOT)>=0){
			log.debug("doGetFolder");
			new JSONFolderHandler().doGetFolder(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONStatsHandler.JSON_ROOT)>=0){
			log.debug("doGetStats");
			new JSONStatsHandler().doGet(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONErrorHandler.JSON_DETAIL_ROOT)>=0){
			log.debug("doGetErrorDetail");
			new JSONErrorHandler().doGetDetail(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONErrorHandler.JSON_ROOT)>=0){
			log.debug("doGetErrors");
			new JSONErrorHandler().doGet(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONMessageUtilHandler.JSON_ROOT)>=0){
			log.debug("doGetErrors");
			new JSONMessageUtilHandler().doGet(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(Charts.CHART_ROOT)>=0){
			log.debug("doCharts");
			new Charts().renderChart(req, resp);
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/generate")>=0){
			WorkerThread wt = Utils.generate(req.getSession().getServletContext(), Integer.parseInt(req.getParameter("count")));
			startWorker(wt, resp);
			resp.flushBuffer();	
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/setbasecount")>=0){
			Inbox.getInstance().setStatsGlobalCount(Long.parseLong(req.getParameter("count")));
			resp.getWriter().print("Set to "+req.getParameter("count"));	
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/rebuildsearchindexes")>=0){
			WorkerThread wt = Inbox.getInstance().rebuildSearchIndexes();
			startWorker(wt, resp);
			resp.flushBuffer();
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/prune")>=0){
			log.debug("Prune");
			try {
				WorkerThread wt = Inbox.getInstance().cleanUp();
				startWorker(wt, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}							
		if (req.getRequestURI().indexOf("rest/admin/errors")>=0){
			try {
				Inbox.getInstance().clearErrors();
				resp.getWriter().print("Cleared errors");	
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/clear")>=0){
			try {
				Inbox.getInstance().deleteAll();
				resp.getWriter().print("Cleaned");			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/dbmaintenance")>=0){
			try {
				WorkerThread wt = Inbox.getInstance().runMaintenance();
				startWorker(wt, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/backup")>=0){
			try {
				File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
				f.mkdir();
				WorkerThread wt = Inbox.getInstance().backup(f);
				startWorker(wt, resp);

				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/restore")>=0){
			try {
				File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
				WorkerThread wt = Inbox.getInstance().restore(f);
				startWorker(wt, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/clean")>=0){
			try {
				File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
				FileUtils.deleteDirectory(f);
				resp.getWriter().print("Cleaned "+f.getCanonicalPath());
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/settoblacklist")>=0){
			String blacklist = req.getParameter("blacklist");
			Config.getInstance().setString(Config.BLUEBOX_TOBLACKLIST, blacklist);
			Inbox.getInstance().loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/setfromblacklist")>=0){
			String blacklist = req.getParameter("blacklist");
			Config.getInstance().setString(Config.BLUEBOX_FROMBLACKLIST, blacklist);
			Inbox.getInstance().loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}

		if (req.getRequestURI().indexOf("rest/admin/settowhitelist")>=0){
			String whitelist = req.getParameter("whitelist");
			Config.getInstance().setString(Config.BLUEBOX_TOWHITELIST, whitelist);
			Inbox.getInstance().loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/setfromwhitelist")>=0){
			String whitelist = req.getParameter("whitelist");
			Config.getInstance().setString(Config.BLUEBOX_FROMWHITELIST, whitelist);
			Inbox.getInstance().loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/workerstats")>=0){
			try {
				resp.getWriter().print(getWorkerStatus().toString());
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}
		if (req.getRequestURI().indexOf("rest/updateavailable")>=0) {
			try {
				resp.getWriter().print(Utils.updateAvailable().toString());
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}
		log.warn("No handler for "+req.getRequestURI()+" expected :"+req.getContextPath());
		super.doGet(req, resp);
	}

	private void startWorker(WorkerThread wt, HttpServletResponse resp) throws IOException {
		// check for running or expired works under this id
		if (workers.containsKey(wt.getId())) {
			WorkerThread old = workers.get(wt.getId());
			if (old.getProgress()>=100) {
				workers.remove(old.getId());
			}
		}
		if (!workers.containsKey(wt.getId())) {
			workers.put(wt.getId(),wt);
			new Thread(wt).start();
			resp.getWriter().print("Task started");
		}
		else {
			resp.getWriter().print("Task aborted - already running");					
		}

	}



	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.warn("Unimplemented doPut :"+req.getRequestURI());
		super.doPut(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.warn("Unimplemented doPost :"+req.getRequestURI());
		super.doPost(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.debug("doDelete :"+req.getRequestURI());
		if (req.getRequestURI().indexOf(JSONMessageHandler.JSON_ROOT)>=0){
			new JSONMessageHandler().doDelete(Inbox.getInstance(),req,resp);
			return;
		}
	}

	public JSONObject getWorkerStatus() throws JSONException {
		JSONObject jo = new JSONObject();
		for (WorkerThread tw : workers.values()) {
			if (tw.getProgress()<=100) {
				jo.put(tw.getId(), tw.getProgress());
			}
		}
//		log.info(jo.toString());
		return jo;
	}


	/*
	 * hOW MUCH TIME HAS ELAPSED SINCE THE SERVLET WAS STARTED
	 */
	public static String getUptime(String format) {
		long duration = new Date().getTime()-started.getTime();
		return DurationFormatUtils.formatDuration(duration, format, true);
	}


}
