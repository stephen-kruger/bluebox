package com.bluebox.smtp;

import java.util.List;

import org.mortbay.log.Log;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;

import com.bluebox.Config;


public class BlueboxMessageHandlerFactory extends SimpleMessageListenerAdapter implements MessageHandlerFactory {

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
		Log.info("Message from {} with address {}",ctx.getHelo(),ctx.getRemoteAddress().toString());
		if ((isBlackListed(ctx.getHelo())||(isBlackListed(ctx.getRemoteAddress().toString())))) {
			return null;
		}
		return super.create(ctx);
	}

}
