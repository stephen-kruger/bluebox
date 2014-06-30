package com.bluebox;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.rest.json.JSONAttachmentHandler;
import com.bluebox.rest.json.JSONAutoCompleteHandler;
import com.bluebox.rest.json.JSONErrorHandler;
import com.bluebox.rest.json.JSONFolderHandler;
import com.bluebox.rest.json.JSONInboxHandler;
import com.bluebox.rest.json.JSONInlineHandler;
import com.bluebox.rest.json.JSONMessageHandler;
import com.bluebox.rest.json.JSONRawMessageHandler;
import com.bluebox.rest.json.JSONSearchHandler;
import com.bluebox.rest.json.JSONStatsHandler;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.Inbox;

public class BlueBoxServlet extends HttpServlet {
	private static final Logger log = Logger.getAnonymousLogger();
	private static final long serialVersionUID = 1015755960967873612L;
	public static final String VERSION = Config.getInstance().getString(Config.BLUEBOX_VERSION);
	private BlueBoxSMTPServer smtpServer;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.info(req.getRequestURI());
		if (req.getRequestURI().indexOf(JSONMessageHandler.JSON_ROOT)>=0){
			log.info("doGetMessageDetail");
			new JSONMessageHandler().doGetMessageDetail(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONAutoCompleteHandler.JSON_ROOT)>=0){
			log.info("JSONAutoCompleteHandler");
			new JSONAutoCompleteHandler().doAutoComplete(Inbox.getInstance(), req, resp);
			return;
		}	
		if (req.getRequestURI().indexOf(JSONInlineHandler.JSON_ROOT)>=0){
			log.info("doGetInlineAttachment");
			new JSONInlineHandler().doGetInlineAttachment(Inbox.getInstance(),req,resp);
			return;
		}		
		if (req.getRequestURI().indexOf(JSONAttachmentHandler.JSON_ROOT)>=0){
			log.info("doGetMessageAttachment");
			new JSONAttachmentHandler().doGetMessageAttachment(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONRawMessageHandler.JSON_ROOT)>=0){
			log.info("doGetRawDetail");
			new JSONRawMessageHandler().doGetRawDetail(Inbox.getInstance(),req,resp);
			return;
		}		
		if (req.getRequestURI().indexOf(JSONInboxHandler.JSON_ROOT)>=0){
			log.info("doGetInbox");
			new JSONInboxHandler().doGetInbox(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONSearchHandler.JSON_ROOT)>=0){
			log.info("doSearchInbox");
			new JSONSearchHandler().doSearchInbox(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONFolderHandler.JSON_ROOT)>=0){
			log.info("doGetFolder");
			new JSONFolderHandler().doGetFolder(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONStatsHandler.JSON_ROOT)>=0){
			log.info("doGetStats");
			new JSONStatsHandler().doGet(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONErrorHandler.JSON_DETAIL_ROOT)>=0){
			log.info("doGetErrorDetail");
			new JSONErrorHandler().doGetDetail(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf(JSONErrorHandler.JSON_ROOT)>=0){
			log.info("doGetErrors");
			new JSONErrorHandler().doGet(Inbox.getInstance(),req,resp);
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/test")>=0){
			Utils.test(Inbox.getInstance(), req.getParameter("count"));
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/setbasecount")>=0){
			Inbox.getInstance().setStatsGlobalCount(Long.parseLong(req.getParameter("count")));
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/rebuildsearchindexes")>=0){
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			Inbox.getInstance().rebuildSearchIndexes();
			return;
		}
		if (req.getRequestURI().indexOf("rest/admin/prune")>=0){
			log.info("Prune");
			try {
				Inbox.getInstance().cleanUp();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp?"+e.getMessage());
			}
			return;
		}							
		if (req.getRequestURI().indexOf("rest/admin/errors")>=0){
			try {
				Inbox.getInstance().clearErrors();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp?"+e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/clear")>=0){
			try {
				Inbox.getInstance().deleteAll();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp?"+e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf("rest/admin/dbmaintenance")>=0){
			try {
				Inbox.getInstance().runMaintenance();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.sendRedirect(req.getContextPath()+"/app/admin.jsp?"+e.getMessage());
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
		log.warning("No handler for "+req.getRequestURI()+" expected :"+req.getContextPath());
		super.doGet(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		log.warning("Unimplemented doPut :"+req.getRequestURI());
		super.doPut(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		log.warning("Unimplemented doPost :"+req.getRequestURI());
		super.doPost(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		log.fine("doDelete :"+req.getRequestURI());
		if (req.getRequestURI().indexOf(JSONMessageHandler.JSON_ROOT)>=0){
			new JSONMessageHandler().doDelete(Inbox.getInstance(),req,resp);
			return;
		}
	}


//	public static final void main(String[] args) {
//		try {
//			StorageImpl.backup(new File("C:\\eclipse.helios\\backup\\repository"), new File("C:\\eclipse.helios\\repository"));
//		} catch (RepositoryException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//
//	}


}
