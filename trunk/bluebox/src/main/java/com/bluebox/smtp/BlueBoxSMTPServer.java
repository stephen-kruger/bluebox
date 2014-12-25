package com.bluebox.smtp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import com.bluebox.Config;
import com.bluebox.Utils;

public class BlueBoxSMTPServer extends SMTPServer {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxSMTPServer.class);

	public BlueBoxSMTPServer(BlueboxMessageHandlerFactory mhf) {
		super(mhf);
		Config bbconfig = Config.getInstance();
		setHostName(Utils.getHostName());
		setPort(bbconfig.getInt(Config.BLUEBOX_PORT));
		log.debug("Starting SMTP server on {} and port {}",Utils.getHostName(),bbconfig.getInt(Config.BLUEBOX_PORT));
		setMaxConnections(bbconfig.getInt(Config.BLUEBOX_MAXCONNECTIONS));
		setHideTLS(true);
		setRequireTLS(false);
		setSoftwareName("BlueBox V"+bbconfig.getString(Config.BLUEBOX_VERSION));
		setConnectionTimeout(30000); // wait 10sec before abandoning connection
	}

	public BlueBoxSMTPServer(BlueboxMessageHandlerFactory mhf, AuthenticationHandlerFactory ahf) {
		super(mhf, ahf);
	}

	@Override
	public synchronized void stop() {
		super.stop();
	}


}
