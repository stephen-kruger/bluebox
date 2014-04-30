package com.bluebox.feed;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.codehaus.jettison.json.JSONObject;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.MimeMessageWrapper;
import com.bluebox.smtp.storage.BlueboxMessage;


@Path("/")
public class MailATOM {
	private static final Logger log = Logger.getAnonymousLogger();

	@GET
	@Produces({MediaType.APPLICATION_ATOM_XML,MediaType.APPLICATION_JSON})
	@Path("/inbox")
	public String getInbox(@QueryParam("email")String email, @Context UriInfo uri) {
		try {
			// remove any funky characters
			InboxAddress inbox = new InboxAddress(email);
			
			log.info("Serving feed for "+email);

			Feed feed = Abdera.getInstance().newFeed();
			// NOTE - RFC 4287 requires a feed to have id, title, and updated properties
			feed.setId("bluebox");
			feed.setLogo(uri.getBaseUri().toString()+"../theme/images/message.png");
			feed.setIcon(uri.getBaseUri().toString()+"../theme/images/favicon.ico");
			feed.setTitle("Inbox for "+email);
			feed.addAuthor(email);
			feed.addLink(uri.getBaseUri().toString()+"../app/inbox.jsp?email="+URLEncoder.encode(email,"UTF-8"), "self");

			List<BlueboxMessage> mlist = Inbox.getInstance().listInbox(inbox, BlueboxMessage.State.NORMAL, 0, 10, BlueboxMessage.RECEIVED, false);
			feed.setUpdated(new Date());
			for (BlueboxMessage message : mlist) {
				Entry entry = Abdera.getInstance().newEntry();
				entry.setId(message.getIdentifier());
				entry.setTitle(message.getProperty(BlueboxMessage.SUBJECT));
				entry.setUpdated(new Date(Long.parseLong(message.getProperty(BlueboxMessage.RECEIVED))));
				entry.addAuthor(Utils.decodeRFC2407(message.getProperty(BlueboxMessage.FROM)));
				entry.setEdited(entry.getUpdated());
				JSONObject jo = new JSONObject(message.toJSON(false));
				entry.setContentAsHtml(jo.getString(MimeMessageWrapper.HTML_BODY));
				entry.setSummary(jo.getString(MimeMessageWrapper.TEXT_BODY));
				entry.addCategory("email");
				entry.addLink(uri.getBaseUri().toString()+"message?uid="+message.getIdentifier(), "self");
				entry.addLink(uri.getBaseUri().toString()+"../app/inbox.jsp?email="+URLEncoder.encode(email,"UTF-8"));
				feed.addEntry(entry);
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			feed.writeTo(bos);
			return bos.toString();
		} 
		catch (Throwable e) {
			log.severe("Error listing users :"+e.getMessage());
			e.printStackTrace();
			return "{\"error\":\""+e.getMessage()+"\"}";
		}
		// Return some cliched textual content
	}

	@GET
	@Produces({MediaType.TEXT_PLAIN})
	@Path("/message")
	public String getMessage(@QueryParam("uid") String uid) {
		try {
			BlueboxMessage message = Inbox.getInstance().retrieve(uid);
			if (message!=null) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				message.getBlueBoxMimeMessage().writeTo(bos);
				return bos.toString();
			}
			else {
				return "Error - message "+uid+" not found";
			}
		} 
		catch (Throwable e) {
			e.printStackTrace();
			return "Error - error accessing message "+uid+" :"+e.getMessage();
		}
	}
}
