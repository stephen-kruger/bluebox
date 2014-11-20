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

	public BlueboxMessageHandlerFactory(Inbox inbox) {
		super(inbox);
		smtpBlackList = Config.getInstance().getStringList(Config.BLUEBOX_SMTPBLACKLIST);
	}

	public boolean isBlackListed(String domain) {
		try {
			for (String bdomain : smtpBlackList) {
				if (domain.contains(bdomain)) {
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
		if ((isBlackListed(ctx.getHelo())||(isBlackListed(ctx.getRemoteAddress().toString())))) {
			return new SpamMessageHandler();
		}
		return super.create(ctx);
	}

	public class SpamMessageHandler implements MessageHandler {

		private String from, recipient;
		
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
			log.info("Handled spab from={} to={}",from,recipient);
		}
		
	}
}
