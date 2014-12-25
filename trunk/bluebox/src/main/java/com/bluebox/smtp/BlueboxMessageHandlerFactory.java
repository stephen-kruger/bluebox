package com.bluebox.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.Config;
import com.bluebox.smtp.storage.StorageFactory;


public class BlueboxMessageHandlerFactory extends SimpleMessageListenerAdapter implements MessageHandlerFactory {
	private static final Logger log = LoggerFactory.getLogger(BlueboxMessageHandlerFactory.class);

	private List<String> smtpBlackList;
	
	public BlueboxMessageHandlerFactory(Inbox inbox) {
		super(inbox);
		setSMTPBlacklist(Config.toList(StorageFactory.getInstance().getProperty(Config.BLUEBOX_SMTPBLACKLIST, 
				Config.getInstance().getString(Config.BLUEBOX_SMTPBLACKLIST))));
		inbox.setBlueboxMessageHandlerFactory(this);
	}
	
	public void setSMTPBlacklist(String props) {
		setSMTPBlacklist(Config.toList(props));
	}
	
	public void setSMTPBlacklist(List<String> list) {
		Config.getInstance().setStringList(Config.BLUEBOX_SMTPBLACKLIST, list);
		
		// persist to storage
		StorageFactory.getInstance().setProperty(Config.BLUEBOX_SMTPBLACKLIST,Config.toString(Config.getInstance().getStringList(Config.BLUEBOX_SMTPBLACKLIST)));
		
		smtpBlackList = list;
	}

	public void addSMTPBlackList(String domain) {
		if (domain!=null) {
			if (domain.trim().length()>0) {
				log.info("Blacklisting smtp server {}",domain);
				smtpBlackList.remove(domain);
				smtpBlackList.add(domain);
				setSMTPBlacklist(smtpBlackList);
			}
		}
	}

	public void removeSMTPBlackList(String domain) {
		if (domain!=null) {
			if (domain.trim().length()>0) {
				log.info("Un-blacklisting smtp server {}",domain);
				smtpBlackList.remove(domain);
				setSMTPBlacklist(smtpBlackList);
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
			throw new RejectException("From not allowed ("+from+")");			
		}

		@Override
		public void recipient(String recipient) throws RejectException {
			this.recipient = recipient;
			throw new RejectException("Recipient not allowed ("+recipient+")");						
		}

		@Override
		public void data(InputStream data) throws RejectException, TooMuchDataException, IOException {
			data.close();
			throw new RejectException("Data not allowed");						
		}

		@Override
		public void done() {
			log.warn("Rejecting spam to {} from {} based on blacklisted SMTP server {} {}",recipient,from,ctx.getHelo(),ctx.getRemoteAddress());
		}

	}
}
