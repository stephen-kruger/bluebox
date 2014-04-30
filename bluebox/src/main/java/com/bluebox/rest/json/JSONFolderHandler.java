package com.bluebox.rest.json;

/*
 * 
 * {
 * 	identifier: 'id',
 * 	label: 'name',
 * 	items: [
 * 
 * 		        { 
 * 					id: 'Overview', 
 * 					name:'Mail for stephen.johnson@ie.xxx.com', 
 * 					type:'folder',
 * 		        	children:[
 * 		        			{ 
 * 								id: 'Inbox', 
 * 								name:'Inbox (56)', 
 * 								url:'country' 
 * 							}, 
 * 		        			{ 
 * 								id: 'Trash', 
 * 								name:'Trash (43)', 
 * 								url:'country' 
 * 							}
 * 		        		] 
 * 		        }	        	
 * 			]
 * }
 */
import java.io.IOException;
import java.io.Writer;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONFolderHandler extends AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();
	public static final String JSON_ROOT = "rest/json/folder";

	protected static String extractEmail(String uri, String fragment) {
		// /bluebox/rest/json/folder/bluemail%20team%20%3Cbluemail@us.xxx.com%3E
		// /bluebox/rest/json/folder/Stephen_johnson/Iceland/XXX%25XXXIE
		// /bluebox/rest/json/folder/Anna johnson <anna.johnson@test.com>/
		try {
			String email;
			if (uri.endsWith(fragment)) {
				return "";
			}
			else {
				int pos1 = uri.indexOf(fragment)+fragment.length()+1;
				int pos2 = uri.length();
				email = uri.substring(pos1,pos2);
				if (email.endsWith("/")) {
					email = email.substring(0, email.length()-1);
				}
				return extractEmail(email);
			}
		}
		catch (Throwable e) {
			log.fine("No email specified in "+uri);
			return "";
		}
	}

	public void doGetFolder(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);

		// get the desired email
		//		String uri = URLDecoder.decode(req.getRequestURI(),"UTF-8");
		String uri = req.getRequestURI();
		try {
			InboxAddress email = new InboxAddress(extractEmail(uri,JSON_ROOT));
			if (email.getAddress().startsWith("*@"))
				email = null;
			log.info("Serving folder count for "+email);
			ResourceBundle rb = ResourceBundle.getBundle("folderDetails",req.getLocale());

			JSONObject result = new JSONObject();
			result.put("identifier","id");
			result.put("label","name");
			JSONObject id = new JSONObject();
			id.put("id", "Overview");
			id.put("name", rb.getString("inboxfor")+" "+email);
			id.put("type", "folder");
			id.put("style", "rootFolder");
			JSONArray children = new JSONArray();
			JSONObject child;

			child = new JSONObject();
			child.put("id", rb.getString("inbox"));
			long normalCount = inbox.getMailCount(email, BlueboxMessage.State.NORMAL);
			child.put("name", rb.getString("inbox")+" ("+normalCount+")");
			child.put("count", normalCount);
			child.put("email", email);
			child.put("state", BlueboxMessage.State.NORMAL.toString());
			child.put("style", "inboxFolder");
			children.put(child);

			child = new JSONObject();
			child.put("id", rb.getString("trash"));
			long trashCount = inbox.getMailCount(email, BlueboxMessage.State.DELETED);
			child.put("name", rb.getString("trash")+" ("+trashCount+")");
			child.put("count", trashCount);
			child.put("email", email);
			child.put("state", BlueboxMessage.State.DELETED.toString());
			child.put("style", "trashFolder");
			children.put(child);			

			child = new JSONObject();
			child.put("id", rb.getString("all"));
			long allCount = inbox.getMailCount(email, BlueboxMessage.State.ANY);//normalCount+trashCount;
			child.put("name", rb.getString("allDocuments")+" ("+allCount+")");
			child.put("count", allCount);
			child.put("email", email);
			child.put("state", BlueboxMessage.State.ANY.toString());
			child.put("style", "allFolder");
			children.put(child);

			id.put("children", children);
			JSONArray folders = new JSONArray();
			folders.put(id);
			result.put("items", folders);

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
}
