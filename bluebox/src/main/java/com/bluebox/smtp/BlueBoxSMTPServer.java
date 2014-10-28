package com.bluebox.smtp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.server.SMTPServer;

import com.bluebox.Config;
import com.bluebox.Utils;

public class BlueBoxSMTPServer extends SMTPServer {
	private static final Logger log = LoggerFactory.getLogger(BlueBoxSMTPServer.class);

	public BlueBoxSMTPServer(MessageHandlerFactory mhf) {
		super(mhf);
		Config bbconfig = Config.getInstance();
		setHostName(Utils.getHostName());
		setPort(bbconfig.getInt(Config.BLUEBOX_PORT));
		log.info("Starting SMTP server on {} and port {}",Utils.getHostName(),bbconfig.getInt(Config.BLUEBOX_PORT));
		setMaxConnections(bbconfig.getInt(Config.BLUEBOX_MAXCONNECTIONS));
		setHideTLS(true);
		setRequireTLS(false);
		setSoftwareName("BlueBox");
		setConnectionTimeout(10000); // wait 10sec before abandoning connection
	}

	public BlueBoxSMTPServer(MessageHandlerFactory mhf, AuthenticationHandlerFactory ahf) {
		super(mhf, ahf);
	}
}
