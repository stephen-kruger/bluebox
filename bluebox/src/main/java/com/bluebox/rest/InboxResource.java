package com.bluebox.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.rest.json.DojoPager;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

@Path(InboxResource.PATH)
@MultipartConfig
public class InboxResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(InboxResource.class);

	public static final String PATH = "/inbox";

	public InboxResource(Inbox inbox) {
		super(inbox);
	}
	
	@GET
	@Path("list/{"+EMAIL+": .*}/{"+STATE+"}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listInbox(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			@PathParam(EMAIL) String emailStr,
			@PathParam(STATE) String stateStr
			) throws IOException {
		// check for form list/email/state or list/state
		if (stateStr=="") {
			stateStr=emailStr;
			emailStr="";
		}
		DojoPager pager = new DojoPager(request,BlueboxMessage.RECEIVED);

		long startTime = new Date().getTime();
		// get the desired email
		InboxAddress inboxAddress;
		try {
			inboxAddress = new InboxAddress(emailStr);
		}
		catch (Throwable e) {
			inboxAddress = null;
		}

		// check if State was specified, else default to NORMAL
		BlueboxMessage.State state = extractState(stateStr);

		try {
			// tell the grid how many items we have
			long totalCount = getInbox().getMailCount(inboxAddress, state);
			pager.setRange(response, totalCount);
			StringWriter writer = new StringWriter();
			getInbox().listInbox(inboxAddress, state, writer, pager.getFirst(), pager.getCount(), pager.getOrderBy().get(0), pager.isAscending(0), request.getLocale());
			writer.flush();
			log.debug("Served inbox contents for {} first={} last={} in {}ms",inboxAddress,pager.getFirst(),pager.getCount(),(new Date().getTime()-startTime));
			return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
		}
	}

	private static BlueboxMessage.State extractState(String state) {
		try {
			return BlueboxMessage.State.values()[Integer.parseInt(state)];
		}
		catch (Throwable t) {
			return BlueboxMessage.State.NORMAL;
		}
	}

	@GET
	@Path("test")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTest() {
		log.info("Entering test method");
		return "{'XXX':'XXXX'}";
	}
}
