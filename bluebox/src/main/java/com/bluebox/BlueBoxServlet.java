package com.bluebox;

import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.BlueboxMessageHandlerFactory;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;

@WebServlet(
		displayName="BlueBox",
		name="bluebox", 
		urlPatterns={"/bluebox/*"},
		loadOnStartup=1
		)
public class BlueBoxServlet extends HttpServlet {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxServlet.class);
	private static final long serialVersionUID = 1015755960967873612L;
	public static final String VERSION = Config.getInstance().getString(Config.BLUEBOX_VERSION);
	private BlueBoxSMTPServer smtpServer;
	private static Date started = new Date();

	@Override
	public void init() throws ServletException {
		log.debug("Initialising BlueBox "+getServletContext().getContextPath());
		log.debug("Starting SMTP server");
		smtpServer = new BlueBoxSMTPServer(new BlueboxMessageHandlerFactory(Inbox.getInstance()));
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
		Inbox.getInstance().stop();

		try {
			StorageFactory.getInstance().stop();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		log.info("Stopped");
	}

	/*
	 * hOW MUCH TIME HAS ELAPSED SINCE THE SERVLET WAS STARTED
	 */
	public static String getUptime(String format) {
		long duration = new Date().getTime()-started.getTime();
		return DurationFormatUtils.formatDuration(duration, format, true);
	}


}
