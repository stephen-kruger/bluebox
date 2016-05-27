package com.bluebox.rest;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.StorageFactory;

@Path(ChartResource.PATH)
@MultipartConfig
public class ChartResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(ChartResource.class);

	public static final String PATH = "/charts";
	public static final String TYPE = "type";

	public ChartResource(Inbox inbox) {
		super(inbox);
	}

	@GET
	@Path("render/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detail(
			@Context HttpServletRequest request,
			@PathParam(TYPE) String chartType) throws IOException {
		try {

			String s = "";
			if (chartType.equals("monthly")) {
				s = convertToArray(StorageFactory.getInstance().getCountByDay(),"value").toString();
			}
			if (chartType.equals("weekly")) {
				s = convertToArrayPie(StorageFactory.getInstance().getCountByDayOfWeek()).toString();
			}
			if (chartType.equals("hourly")) {
				s = convertToArray(StorageFactory.getInstance().getCountByHour(),"value").toString();
			}
			return Response.ok(s).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
		}
	}

	private JSONArray convertToArray(JSONObject jo, String key) throws JSONException {
		@SuppressWarnings("unchecked")
		Iterator<String> keys = jo.keys();
		JSONArray ja = new JSONArray();
		while (keys.hasNext()) {
			JSONObject val = new JSONObject();
			val.put(key,jo.getInt(keys.next()));
			ja.put(val);
		}
		return ja;
	}
	
	private static String getDayOfWeek(int day) {
		switch (day) {
		case 1 : return "Sun";
		case 2 : return "Mon";
		case 3 : return "Tue";
		case 4 : return "Wed";
		case 5 : return "Thu";
		case 6 : return "Fri";
		case 7 : return "Sat";
		default : return "What?";
		}
	}
	
	
	public static JSONArray convertToArrayPie(JSONObject jo) throws JSONException {
	//		chartData = [
	//		             { x: 1, y: 19021 },
	//		             { x: 1, y: 12837 },
	//		             { x: 1, y: 12378 },
	//		             { x: 1, y: 21882 },
	//		             { x: 1, y: 17654 },
	//		             { x: 1, y: 15833 },
	//		             { x: 1, y: 16122 }
	//		         ];		
//		log.info(jo.toString(3));
		@SuppressWarnings("unchecked")
		Iterator<String> keys = jo.keys();
		JSONArray ja = new JSONArray();
		JSONObject data;
		String key;
		while (keys.hasNext()) {
			key = keys.next();
			data = new JSONObject();
			data.put("x",Integer.parseInt(key));
			data.put("text",getDayOfWeek(Integer.parseInt(key)));
			data.put("y",jo.getInt(key));
			ja.put(data);
		}
		return ja;
	}
}
