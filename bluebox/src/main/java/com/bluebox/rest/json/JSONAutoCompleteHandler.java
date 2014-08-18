package com.bluebox.rest.json;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.smtp.Inbox;

public class JSONAutoCompleteHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/autocomplete";
	private String START = "start";
	private String COUNT = "count";
	private String NAME = "label";
	
	public void doAutoComplete(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);
		
		String hint = ""+req.getParameter(NAME); // Stev*
		String start = req.getParameter(START);
		String count = req.getParameter(COUNT);
		
		log.info("doAutoComplete "+hint+" "+start+" "+count);
		
		try {
			JSONObject result = new JSONObject();
			result.put("identifier","identifier");
			result.put("label","name");

			JSONArray children = doAutoComplete(inbox, hint, start, count);
			
			result.put("items", children);
			
			resp.setContentType("text/x-json;charset=UTF-8");
			Writer writer = resp.getWriter();
			writer.write(result.toString(3));
			writer.flush();
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
	
	public JSONArray doAutoComplete(Inbox inbox, String hint, String start, String count) {
		log.fine("doAutoComplete2 "+hint+" "+start+" "+count);
		long startI=0, countI=15;
		try {
			try {
				startI = Long.parseLong(start);
			}
			catch (Throwable t) {
				log.fine("Invalid type-ahead start value passed :"+start);
			}
			
			try {
				countI = Long.parseLong(count);
			}
			catch (Throwable t) {
				log.fine("Invalid type-ahead count value passed :"+count);
			}
			
			JSONArray children = inbox.autoComplete(hint, startI, countI);
			return children;
		}
		catch (Throwable t) {
			log.severe(t.getMessage());
			t.printStackTrace();
			return new JSONArray();
		}		
	}
}
