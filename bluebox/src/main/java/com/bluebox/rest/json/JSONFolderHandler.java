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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class JSONFolderHandler extends AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(JSONFolderHandler.class);
	public static final String JSON_ROOT = "rest/json/folder";

	public void doGetFolder(Inbox inbox, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		setDefaultHeaders(resp);

		// get the desired email
		//		String uri = URLDecoder.decode(req.getRequestURI(),"UTF-8");
		String uri = req.getRequestURI();
		//		uri = uri.substring(uri.indexOf(JSON_ROOT)+JSON_ROOT.length());
		try {
			InboxAddress email=null;
			String emailStr = extractEmail(extractFragment(uri,JSON_ROOT,0));
			if (emailStr.trim().length()>0) {
				if (emailStr.startsWith("*@")) {
					emailStr="";
				}
				else {
					email = new InboxAddress(emailStr);
					emailStr=email.getAddress();
				}
			}

			log.debug("Serving folder count for {}",emailStr);
			ResourceBundle rb = ResourceBundle.getBundle("folderDetails",req.getLocale());

			JSONObject folders = new JSONObject();
			folders.put("id", "Overview");
			folders.put("name", rb.getString("inboxfor")+" "+emailStr);
			folders.put("type", "folder");
			folders.put("style", "rootFolder");

			JSONObject normal = new JSONObject();
			normal.put("id", rb.getString("inbox"));
			long normalCount = inbox.getMailCount(email, BlueboxMessage.State.NORMAL);
			normal.put("name", rb.getString("inbox")+" ("+normalCount+")");
			normal.put("count", normalCount);
			normal.put("email", emailStr);
			normal.put("state", BlueboxMessage.State.NORMAL.name());
			normal.put("style", "inboxFolder");
			folders.put(BlueboxMessage.State.NORMAL.name(),normal);

			JSONObject deleted = new JSONObject();
			deleted.put("id", rb.getString("trash"));
			long trashCount = inbox.getMailCount(email, BlueboxMessage.State.DELETED);
			deleted.put("name", rb.getString("trash")+" ("+trashCount+")");
			deleted.put("count", trashCount);
			deleted.put("email", emailStr);
			deleted.put("state", BlueboxMessage.State.DELETED.name());
			deleted.put("style", "trashFolder");
			folders.put(BlueboxMessage.State.DELETED.name(),deleted);			

			JSONObject all = new JSONObject();
			all.put("id", rb.getString("all"));
			long allCount = inbox.getMailCount(email, BlueboxMessage.State.ANY);//normalCount+trashCount;
			all.put("name", rb.getString("allDocuments")+" ("+allCount+")");
			all.put("count", allCount);
			all.put("email", emailStr);
			all.put("state", BlueboxMessage.State.ANY.name());
			all.put("style", "allFolder");
			folders.put(BlueboxMessage.State.ANY.name(),all);

			Writer writer = resp.getWriter();
			writer.write(folders.toString(3));
			writer.flush();
		}
		catch (Throwable t) {
			log.error(t.getMessage());
			t.printStackTrace();
		}
		resp.flushBuffer();
	}
}
