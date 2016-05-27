package com.bluebox;

import java.io.IOException;
import java.util.Date;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.rest.json.JSONAdminHandler;
import com.bluebox.rest.json.JSONHeadersMessageHandler;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.rest.json.JSONMessageUtilHandler;
import com.bluebox.rest.json.JSONRawMessageHandler;
import com.bluebox.rest.json.JSONSPAMHandler;
import com.bluebox.rest.json.JSONSearchHandler;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;

public class BlueBoxServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxServlet.class);
	private static final long serialVersionUID = 1015755960967873612L;
	public static final String VERSION = Config.getInstance().getString(Config.BLUEBOX_VERSION);
	private BlueBoxSMTPServer smtpServer;
	private Inbox inbox;
	private static Date started = new Date();

	@Override
	public void init() throws ServletException {
		log.debug("Initialising BlueBox "+getServletContext().getContextPath());
		log.debug("Starting SMTP server");
		inbox = Inbox.getInstance();
		smtpServer = new BlueBoxSMTPServer(new BlueboxMessageHandlerFactory(inbox));
		smtpServer.start();
	}

	@Override
	public void destroy() {
		super.destroy();
		log.debug("Stopping servlet");
		// shut down any worker threads
		WorkerThread.stopWorkers();

		// shut down the SMTP server
		smtpServer.stop();

		// shut down the inbox
		inbox.stop();

		try {
			StorageFactory.getInstance().stop();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		log.info("Stopped");
	}

	public Inbox getInbox() {
		return inbox;
	}

	@Override
	protected void doGet(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		String uri = req.getRequestURI(); 

//		if (uri.indexOf(JSONMessageHandler.JSON_ROOT)>=0){
//			log.debug("doGetMessageDetail");
//			new JSONMessageHandler().doGetMessageDetail(inbox,req,resp);
//			return;
//		}
//		if (uri.indexOf(JSONAutoCompleteHandler.JSON_ROOT)>=0){
//			log.debug("JSONAutoCompleteHandler");
//			new JSONAutoCompleteHandler().doAutoComplete(inbox, req, resp);
//			return;
//		}	
		if (uri.indexOf(JSONInlineHandler.JSON_ROOT)>=0){
			log.debug("doGetInlineAttachment");
			new JSONInlineHandler().doGetInlineAttachment(inbox,req,resp);
			return;
		}		
//		if (uri.indexOf(JSONAttachmentHandler.JSON_ROOT)>=0){
//			log.debug("doGetMessageAttachment");
//			new JSONAttachmentHandler().doGetMessageAttachment(inbox,req,resp);
//			return;
//		}
		if (uri.indexOf(JSONRawMessageHandler.JSON_ROOT)>=0){
			log.debug("doGetRawDetail");
			new JSONRawMessageHandler().doGetRawDetail(inbox,req,resp);
			return;
		}	
		if (uri.indexOf(JSONHeadersMessageHandler.JSON_ROOT)>=0){
			log.debug("doGetHeadersDetail");
			new JSONHeadersMessageHandler().doGetHeadersDetail(inbox,req,resp);
			return;
		}	
//		if (uri.indexOf(JSONInboxHandler.JSON_ROOT)>=0){
//			log.debug("doGetInbox");
//			new JSONInboxHandler().doGetInbox(inbox,req,resp);
//			return;
//		}
		if (uri.indexOf(JSONSearchHandler.JSON_ROOT)>=0){
			log.debug("doSearchInbox");
			new JSONSearchHandler().doSearchInbox(inbox,req,resp);
			return;
		}
//		if (uri.indexOf(JSONFolderHandler.JSON_ROOT)>=0){
//			log.debug("doGetFolder");
//			new JSONFolderHandler().doGetFolder(inbox,req,resp);
//			return;
//		}
//		if (uri.indexOf(JSONStatsHandler.JSON_ROOT)>=0){
//			log.debug("doGetStats");
//			new JSONStatsHandler().doGet(inbox,req,resp);
//			return;
//		}
//		if (uri.indexOf(JSONErrorHandler.JSON_DETAIL_ROOT)>=0){
//			log.debug("doGetErrorDetail");
//			new JSONErrorHandler().doGetDetail(inbox,req,resp);
//			return;
//		}
//		if (uri.indexOf(JSONErrorHandler.JSON_ROOT)>=0){
//			log.debug("doGetErrors");
//			new JSONErrorHandler().doGet(inbox,req,resp);
//			return;
//		}
		if (uri.indexOf(JSONMessageUtilHandler.JSON_ROOT)>=0){
			log.debug("doGetMessageUtils");
			new JSONMessageUtilHandler().doGet(inbox,req,resp);
			return;
		}
//		if (uri.indexOf(JSONChartHandler.JSON_ROOT)>=0){
//			log.debug("doGetCharts");
//			new JSONChartHandler().doGet(inbox,req,resp);
//			return;
//		}
		if (uri.indexOf(JSONAdminHandler.JSON_ROOT)>=0){
			log.debug("doGetAdmin");
			new JSONAdminHandler().doGet(this,inbox,req,resp);
			return;
		}

		if (uri.indexOf("rest/updateavailable")>=0) {
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

		log.warn("No handler for "+uri+" expected :"+req.getContextPath());
		super.doGet(req, resp);
	}



	public void startWorker(WorkerThread wt, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// check for running or expired works under this id
		ResourceBundle rb = ResourceBundle.getBundle("admin",req.getLocale());
		WorkerThread.startWorker(wt);
		resp.getWriter().print(rb.getString("taskStarted")+":"+wt.getId());
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
		req.setCharacterEncoding("UTF-8");
		log.debug("doDelete :{}",req.getRequestURI());
//		if (req.getRequestURI().indexOf(JSONMessageHandler.JSON_ROOT)>=0){
//			new JSONMessageHandler().doDelete(inbox,req,resp);
//			return;
//		}
		if (req.getRequestURI().indexOf(JSONSPAMHandler.JSON_ROOT)>=0){
			WorkerThread wt = new JSONSPAMHandler().doDelete(inbox,req,resp);

			startWorker(wt, req, resp);

			resp.flushBuffer();
			return;
		}
	}

	public JSONObject getWorkerStatus() throws JSONException {
		return WorkerThread.getWorkerStatus();
	}


	/*
	 * hOW MUCH TIME HAS ELAPSED SINCE THE SERVLET WAS STARTED
	 */
	public static String getUptime(String format) {
		long duration = new Date().getTime()-started.getTime();
		return DurationFormatUtils.formatDuration(duration, format, true);
	}


}
