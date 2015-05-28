package com.bluebox.rest.json;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.BlueBoxServlet;
import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;

public class JSONAdminHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONAdminHandler.class);
	public static final String JSON_ROOT = "rest/admin";

	public void doGet(BlueBoxServlet bbservlet, Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getRequestURI().indexOf(JSON_ROOT+"/generate")>=0){
			try {
				WorkerThread wt = Utils.generate(req.getSession().getServletContext(), inbox, Integer.parseInt(req.getParameter("count")));
				bbservlet.startWorker(wt, req, resp);
			}
			catch (Throwable t) {
				log.error("Task already running ",t.getMessage());
				resp.getWriter().print("Aborting: "+t.getMessage());
			}
			resp.flushBuffer();	
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/setbasecount")>=0){
			inbox.setStatsGlobalCount(Long.parseLong(req.getParameter("count")));
			resp.getWriter().print("Set to "+req.getParameter("count"));	
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/rebuildsearchindexes")>=0){
			WorkerThread wt;
			try {
				wt = inbox.rebuildSearchIndexes();
				bbservlet.startWorker(wt, req, resp);
			} 
			catch (Exception e) {
				log.error("Could not start task",e);
				resp.getWriter().print("Error: task already running ("+e.getMessage()+")");
			}
			resp.flushBuffer();
			return;
		}
		if (req.getRequestURI().indexOf(JSON_ROOT+"/prune")>=0){
			log.debug("Prune");
			try {
				WorkerThread wt = inbox.cleanUp();
				bbservlet.startWorker(wt, req, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}							
		if (req.getRequestURI().indexOf(JSON_ROOT+"/errors")>=0){
			try {
				inbox.clearErrors();
				resp.getWriter().print("Cleared errors");	
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/clear")>=0){
			try {
				inbox.deleteAll();
				resp.getWriter().print("Cleaned");			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/purge_deleted")>=0){
			try {
				inbox.purge();
				resp.getWriter().print("Purged");			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/dbmaintenance")>=0){
			try {
				WorkerThread wt = inbox.runMaintenance();
				bbservlet.startWorker(wt, req, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/rawclean")>=0){
			try {
				WorkerThread wt = inbox.cleanRaw();
				bbservlet.startWorker(wt, req, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());	
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/backup")>=0){
			try {
				File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
				f.mkdir();
				WorkerThread wt = inbox.backup(f);
				bbservlet.startWorker(wt, req, resp);

				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/restore")>=0){
			try {
				File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
				WorkerThread wt = inbox.restore(f);
				bbservlet.startWorker(wt, req, resp);
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}	
		if (req.getRequestURI().indexOf(JSON_ROOT+"/clean")>=0){
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
		if (req.getRequestURI().indexOf(JSON_ROOT+"/settoblacklist")>=0){
			String blacklist = req.getParameter("blacklist");
			Config.getInstance().setString(Config.BLUEBOX_TOBLACKLIST, blacklist);
			inbox.loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf(JSON_ROOT+"/setfromblacklist")>=0){
			String blacklist = req.getParameter("blacklist");
			Config.getInstance().setString(Config.BLUEBOX_FROMBLACKLIST, blacklist);
			inbox.loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}

		if (req.getRequestURI().indexOf(JSON_ROOT+"/settowhitelist")>=0){
			String whitelist = req.getParameter("whitelist");
			Config.getInstance().setString(Config.BLUEBOX_TOWHITELIST, whitelist);
			inbox.loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf(JSON_ROOT+"/setfromwhitelist")>=0){
			String whitelist = req.getParameter("whitelist");
			Config.getInstance().setString(Config.BLUEBOX_FROMWHITELIST, whitelist);
			inbox.loadConfig();
			resp.sendRedirect(req.getContextPath()+"/app/admin.jsp");
			return;
		}
		if (req.getRequestURI().indexOf(JSON_ROOT+"/workerstats")>=0){
			try {
				resp.getWriter().print(bbservlet.getWorkerStatus().toString());
				resp.flushBuffer();
			} 
			catch (Exception e) {
				e.printStackTrace();
				resp.getWriter().print(e.getMessage());
			}
			return;
		}		
		if (req.getRequestURI().indexOf(JSON_ROOT+"/setsmtpblacklist")>=0){
			log.info("Setting smtp blacklist :{}",req.getParameter("value"));
			inbox.setSMTPBlacklist(req.getParameter("value"));
			return;
		}	
		resp.sendError(HttpStatus.SC_BAD_REQUEST, "No such command");
	}


}
