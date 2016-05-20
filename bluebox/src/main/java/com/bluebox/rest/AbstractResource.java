package com.bluebox.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;

abstract class AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(AbstractResource.class);
	public static final String EMAIL = "email";
	public static final String STATE = "state";
	public static final String ORDER = "order";
	public static final String ASCENDING = "ascending";
	public static final String COUNT = "count";
	public static final String START = "start";
	
	private Inbox inbox;

	public AbstractResource(Inbox inbox) {
		this.inbox = inbox;
	}
	
	public Inbox getInbox() {
		return inbox;
	}

	public void setInbox(Inbox inbox) {
		this.inbox = inbox;
	}
	
	/**
	 * Error json returns a json containing response object.
	 *
	 * @param jo the jo
	 * @return the response
	 */
	public static Response error(String msg) {
		log.error("{}",msg);
		return Response.status(HttpStatus.SC_OK)
				.entity(msg)
				.type(MediaType.APPLICATION_JSON).
				build();
	}
}
