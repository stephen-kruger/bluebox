package com.bluebox.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.internet.MimeMessage;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

@Path(HeaderResource.PATH)
@MultipartConfig
public class HeaderResource extends AbstractResource {
	private static final Logger log = LoggerFactory.getLogger(HeaderResource.class);

	public static final String PATH = "/header";
	public static final String UID = "uid";

	public HeaderResource(Inbox inbox) {
		super(inbox);
	}

	@GET
	@Path("detail/{uid}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response detail(
			@Context HttpServletRequest request,
			@Context HttpServletResponse response,
			@DefaultValue("") @PathParam(UID) String uid) throws IOException {
		try {
			log.debug("Serving headers for {}",uid);
			BlueboxMessage message = Inbox.getInstance().retrieve(uid);
			MimeMessage bbm = message.getBlueBoxMimeMessage();
			response.setContentType(MediaType.TEXT_PLAIN);
			response.setCharacterEncoding(Utils.UTF8);
			StringWriter pw = new StringWriter();
			@SuppressWarnings("rawtypes")
			Enumeration headers = bbm.getAllHeaders();
			while (headers.hasMoreElements()) {
				Header header = (Header) headers.nextElement();
				pw.append("<b>");
				pw.append(header.getName());
				pw.append("</b>");
				pw.append("=");
				pw.append(StringEscapeUtils.escapeHtml(header.getValue()));
				pw.append("<br/>");
			}
			pw.close();
			return Response.ok(pw.toString(), MediaType.TEXT_PLAIN).build();
		}
		catch (Throwable t) {
			log.error("Problem listing headers",t);
			return error(t.getMessage());
		}
	}

}
