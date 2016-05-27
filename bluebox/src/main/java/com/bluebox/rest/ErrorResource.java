package com.bluebox.rest;

import java.io.IOException;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.rest.json.DojoPager;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

@Path(ErrorResource.PATH)
@MultipartConfig
public class ErrorResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(ErrorResource.class);

	public static final String PATH = "/error";
	private static final String UID = "uid";

	public ErrorResource(Inbox inbox) {
		super(inbox);
	}

	@GET
	@Path("list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response list(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response) throws IOException {
		try {
			DojoPager pager = new DojoPager(request,BlueboxMessage.RECEIVED);
			// tell the grid how many items we have
			long totalCount = Inbox.getInstance().errorCount();
			pager.setRange(response, totalCount);
			log.debug("Sending JSON error view first={} last={}",pager.getFirst(),pager.getLast());
			JSONArray result = Inbox.getInstance().errorCount(pager.getFirst(), pager.getCount());
			return Response.ok(result.toString()).build();
		}
		catch (Throwable t) {
			log.error("Problem listing errors",t);
			return error(t.getMessage());
		}
	}
	
	@GET
	@Path("detail/{uid}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response detail(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			@PathParam(UID) String uid) throws IOException {
		try {
			log.debug("Serving error detail for id={}",uid);
			response.setContentType(MediaType.TEXT_PLAIN);
			return Response.ok(Inbox.getInstance().errorDetail(uid)).build();
		}
		catch (Throwable t) {
			log.error("Problem getting error detail",t);
			return error(t.getMessage());
		}
	}
}
