package com.bluebox.rest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import javax.mail.Message.RecipientType;
import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.LiteMessage;

@Path(MessageResource.PATH)
@MultipartConfig
public class MessageResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(MessageResource.class);

	public static final String PATH = "/message";
	public static final String UID = "uid";
	public static final String SECURITY = "Security";

	public MessageResource(Inbox inbox) {
		super(inbox);
	}

	@GET
	@Path("detail/{uid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response detail(
			@Context HttpServletRequest request,
			@PathParam(UID) String uid) throws IOException {
		try {
			BlueboxMessage message = Inbox.getInstance().retrieve(uid);
			if (message == null) {
				log.error("Could not find message with uid="+uid);
			}
			JSONObject json = message.toJSON();
			
			json.put(BlueboxMessage.RECEIVED, LiteMessage.dateToString(new Date(json.getLong(BlueboxMessage.RECEIVED)),request.getLocale()));
			// add in the TO, CC and BCC
			json.put(BlueboxMessage.TO,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.TO)));	
			json.put(BlueboxMessage.CC,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.CC)));
			json.put(BlueboxMessage.BCC,BlueboxMessage.toJSONArray(message.getBlueBoxMimeMessage().getRecipients(RecipientType.BCC)));
			// note: BCC can be populated if no TO or CC is detected, by using the INBOX metadata
			if ((json.getJSONArray(BlueboxMessage.TO).length()==0)&&
					(json.getJSONArray(BlueboxMessage.CC).length()==0)&&
					(json.getJSONArray(BlueboxMessage.BCC).length()==0)) {
				JSONArray bcc = new JSONArray();
				bcc.put(message.getInbox().getFullAddress());
				json.put(BlueboxMessage.BCC,bcc);
			}
			json = securityScan(request,message,json);
			return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error("Problem listing inbox",t);
			return error(t.getMessage());
		}
	}
	
	@DELETE
	@Path("delete/{uid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam(UID) String uidList) {
		try {
			StringTokenizer uidArray = new StringTokenizer(uidList,",");
			String uid;
			while (uidArray.hasMoreTokens()) {
				uid = uidArray.nextToken();
				BlueboxMessage msg = Inbox.getInstance().retrieve(uid);
				if (msg.getState()==BlueboxMessage.State.DELETED) {
					Inbox.getInstance().delete(uid,msg.getRawUid());
				}
				else {
					Inbox.getInstance().softDelete(uid);
				}
			}
			JSONObject result = new JSONObject();
			result.put("message", "ok");
			return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
		}
		catch (Throwable t) {
			log.error(t.getMessage(),t);
			return error(t.getMessage());
		}
	}

	private JSONObject securityScan(HttpServletRequest request, BlueboxMessage message, JSONObject json) {
		ResourceBundle mailDetailsResource = ResourceBundle.getBundle("mailDetails",request.getLocale());
		ServletContext context = request.getSession().getServletContext();
		try {
			// perform security scan on html content
			InputStream p = context.getResourceAsStream("WEB-INF/antisamy-anythinggoes-1.4.4.xml");
			if (p==null)
				p = new FileInputStream("src/main/webapp/WEB-INF/antisamy-anythinggoes-1.4.4.xml");

			// scan html for malicious content
			String html = message.getHtml(request);
			Policy policy = Policy.getInstance(p);
			AntiSamy as = new AntiSamy();
			CleanResults cr = as.scan(html, policy);
			StringBuffer sec = new StringBuffer();
			
			int count = 1;
			for (String error : cr.getErrorMessages())
				sec.append((count++)+"(body) "+error+"\n");
			json.put(BlueboxMessage.HTML_BODY, cr.getCleanHTML());

			// scan subject for malicious content
			cr = as.scan(message.getSubject(), policy);
			json.put(BlueboxMessage.SUBJECT, cr.getCleanHTML());
			for (String error : cr.getErrorMessages())
				sec.append((count++)+"(subject) "+error+"\n");

			// don't scan text body for malicious content, it renders as text only
			json.put(BlueboxMessage.TEXT_BODY, message.getText());
			
			StringBuffer title = new StringBuffer();
			title.append(MessageFormat.format(mailDetailsResource.getString("scantitle"), count-1)+"\n");
			title.append(mailDetailsResource.getString("scantitleunderline")+"\n");
			json.put(SECURITY, title.toString()+sec.toString());

		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return json;
	}

}
