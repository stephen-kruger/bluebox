package com.bluebox.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;


public class BlueboxMessageHandlerFactory extends SimpleMessageListenerAdapter implements MessageHandlerFactory {
	private static final Logger log = LoggerFactory.getLogger(BlueboxMessageHandlerFactory.class);

	private List<String> smtpBlackList;
//	private static BlueboxMessageHandlerFactory instance;

//	public static BlueboxMessageHandlerFactory getInstance() {
//		if (instance==null) {
//			instance = new BlueboxMessageHandlerFactory(new Inbox());
//		}
//		return instance;
//	}

	public void stop() {
//		instance = null;
	}
	
	public BlueboxMessageHandlerFactory(Inbox inbox) {
		super(inbox);
		smtpBlackList = Config.getInstance().getStringList(Config.BLUEBOX_SMTPBLACKLIST);
		inbox.setBlueboxMessageHandlerFactory(this);
	}

	public void addSMTPBlackList(String domain) {
		if (domain!=null) {
			if (domain.trim().length()>0) {
				log.info("Blacklisting smtp server {}",domain);
				smtpBlackList.remove(domain);
				smtpBlackList.add(domain);
				Config.getInstance().setStringList(Config.BLUEBOX_SMTPBLACKLIST,smtpBlackList);
			}
		}
	}

	public void removeSMTPBlackList(String domain) {
		if (domain!=null) {
			if (domain.trim().length()>0) {
				log.info("Un-blacklisting smtp server {}",domain);
				smtpBlackList.remove(domain);
				Config.getInstance().setStringList(Config.BLUEBOX_SMTPBLACKLIST,smtpBlackList);
			}
		}
	}

	public boolean isBlackListed(MessageContext ctx) {
		return isBlackListed(ctx.getHelo(),ctx.getRemoteAddress().toString());
	}
	
	public boolean isBlackListed(String helo, String address) {
		try {
			for (String bdomain : smtpBlackList) {
				if ((helo.contains(bdomain))||(address.contains(bdomain))) {
					return true;
				}
			}
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return false;
	}

	@Override
	public MessageHandler create(MessageContext ctx) {
		log.debug("Message from {} with address {}",ctx.getHelo(),ctx.getRemoteAddress().toString());
		if (isBlackListed(ctx)) {
			return new SpamMessageHandler(ctx);
		}
		return super.create(ctx);
	}

	public class SpamMessageHandler implements MessageHandler {

		private String from, recipient;
		private MessageContext ctx;

		public SpamMessageHandler(MessageContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void from(String from) throws RejectException {
			this.from = from;
			throw new RejectException("From not allowed");			
		}

		@Override
		public void recipient(String recipient) throws RejectException {
			this.recipient = recipient;
			throw new RejectException("Recipient not allowed");						
		}

		@Override
		public void data(InputStream data) throws RejectException, TooMuchDataException, IOException {
			throw new RejectException("Data not allowed");						
		}

		@Override
		public void done() {
			log.warn("Rejecting spam to {} from {} based on blacklisted SMTP server {} {}",recipient,from,ctx.getHelo(),ctx.getRemoteAddress());
		}

	}
}
