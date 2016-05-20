package com.bluebox.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;

import javax.servlet.annotation.MultipartConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	@Path("list")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listInbox(
			@QueryParam(EMAIL) String emailStr,
			@DefaultValue("ANY") @QueryParam(STATE) String stateStr,
			@DefaultValue("RECEIVED") @QueryParam(ORDER) String orderStr,
			@DefaultValue("false") @QueryParam(ASCENDING) boolean ascending,
			@DefaultValue("10") @QueryParam(COUNT) int count,
			@DefaultValue("0") @QueryParam(START) int start) throws IOException {

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
		BlueboxMessage.State state = BlueboxMessage.State.valueOf(stateStr);
		try {
			// tell the grid how many items we have
//			long totalCount = inbox.getMailCount(inboxAddress, state);
//			pager.setRange(resp, totalCount);
			StringWriter writer = new StringWriter();
			getInbox().listInbox(inboxAddress, state, writer, start, count, orderStr, ascending, Locale.getDefault());
			writer.flush();
			log.debug("Served inbox contents for {} first={} last={} in {}ms",inboxAddress,start,count,(new Date().getTime()-startTime));
			return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
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
