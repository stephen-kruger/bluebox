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
import org.codehaus.jettison.json.JSONObject;
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
	@Path("list")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response listInbox(
			@DefaultValue("") @QueryParam("label") String emailStr,
			@QueryParam("start") String start,
			@QueryParam("count") String count) throws IOException {
//		log.info(">>>>>>>>>{} {} {}",emailStr,start,count);
		long startTime = new Date().getTime();
		long startI=0, countI=15;
		try {
			try {
				startI = Long.parseLong(start);
			}
			catch (Throwable t) {
				log.debug("Invalid type-ahead start value passed :{}",start);
			}
			
			try {
				countI = Long.parseLong(count);
			}
			catch (Throwable t) {
				log.debug("Invalid type-ahead count value passed :{}",count);
			}
			
			JSONArray children = Inbox.getInstance().autoComplete(emailStr, startI, countI);
			JSONObject result = new JSONObject();
			result.put("identifier","identifier");
			result.put("label","name");			
			result.put("items", children);
			
			log.info("Autocomplete returned {} first={} last={} in {}ms",emailStr,startI,countI,(new Date().getTime()-startTime));
			return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
		}
	}

}
