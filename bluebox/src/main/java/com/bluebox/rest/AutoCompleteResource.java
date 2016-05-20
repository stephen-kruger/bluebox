package com.bluebox.rest;

import java.io.IOException;
import java.util.Date;

import javax.servlet.annotation.MultipartConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;

@Path(AutoCompleteResource.PATH)
@MultipartConfig
public class AutoCompleteResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(AutoCompleteResource.class);

	public static final String PATH = "/autocomplete";

	public AutoCompleteResource(Inbox inbox) {
		super(inbox);
	}

	@GET
	@Path("inboxes")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listInbox(
			@DefaultValue("") @QueryParam(EMAIL) String emailStr,
			@DefaultValue("0") @QueryParam(START) int start,
			@DefaultValue("10") @QueryParam(COUNT) int count) throws IOException {

		long startTime = new Date().getTime();
			try {
			JSONArray children = Inbox.getInstance().autoComplete(emailStr, start, count);
			log.debug("Autocomplete returned {} first={} last={} in {}ms",emailStr,start,count,(new Date().getTime()-startTime));
			return Response.ok(children.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
		}
	}

}
