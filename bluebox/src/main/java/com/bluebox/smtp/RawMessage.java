package com.bluebox.smtp;

import java.io.ByteArrayInputStream;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * This class wraps a received message and provides
 * a way to generate a JavaMail MimeMessage from the data.
 *
 */
public class RawMessage {
	private byte[] messageData;
	private String envelopeSender;
	private String envelopeReceiver;
//	private static Session session;
	
	RawMessage(String envelopeSender, String envelopeReceiver, byte[] messageData) {
		this.envelopeSender = envelopeSender;
		this.envelopeReceiver = envelopeReceiver;
		this.messageData = messageData;
	}

	/**
	 * Generate a JavaMail MimeMessage.
	 * @throws MessagingException
	 */
	public MimeMessage getMimeMessage() throws MessagingException {
		return new MimeMessage(null, new ByteArrayInputStream(this.messageData));
	}

//	private static Session getSession() {
//		if (session==null) {
//			session = Utils.getSession();;
//		}
//		return session;
//	}

	/**
	 * Get's the raw message DATA.
	 */
	public byte[] getData() {
		return this.messageData;
	}

	/**
	 * Get's the RCPT TO:
	 */
	public String getEnvelopeReceiver()
	{
		return this.envelopeReceiver;
	}

	/**
	 * Get's the MAIL FROM:
	 */
	public String getEnvelopeSender() {
		return this.envelopeSender;
	}

	/**
	 * Dumps the rough contents of the message for debugging purposes
	 */
	public void dumpMessage(Logger log) throws MessagingException {
		log.info("===== Dumping message =====");

		log.info("Envelope sender: " + this.getEnvelopeSender());
		log.info("Envelope recipient: " + this.getEnvelopeReceiver());

		// It should all be convertible with ascii or utf8
		String content = new String(this.getData());
		log.info(content);

		log.info("===== End message dump =====");
	}

	/**
	 * Implementation of toString()
	 *
	 * @return getData() as a string or an empty string if getData is null
	 */
	@Override
	public String toString() {
		if (this.getData() == null)
			return "";

		return new String(this.getData());
	}
}
