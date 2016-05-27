package com.bluebox;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		log.warn("No handler for "+uri+" expected :"+req.getContextPath());
		super.doGet(req, resp);
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

	/*
	 * hOW MUCH TIME HAS ELAPSED SINCE THE SERVLET WAS STARTED
	 */
	public static String getUptime(String format) {
		long duration = new Date().getTime()-started.getTime();
		return DurationFormatUtils.formatDuration(duration, format, true);
	}


}
