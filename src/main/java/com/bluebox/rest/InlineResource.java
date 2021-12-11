package com.bluebox.rest;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;

@Path(InlineResource.PATH)
@MultipartConfig
public class InlineResource extends AbstractResource {
    public static final String PATH = "/inline";
    public static final String UID = "uid";
    public static final String NAME = "name";
    private static final Logger log = LoggerFactory.getLogger(InlineResource.class);

    public InlineResource(Inbox inbox) {
        super(inbox);
    }

    @GET
    @Path("get/{uid}/{name:.*}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.WILDCARD)
    public Response get(
            @Context HttpServletRequest request,
            @PathParam(UID) String uid,
            @PathParam(NAME) String name) throws IOException {
        log.debug("Serving inline resource uid={} name={}", uid, name);
        try {
            BlueboxMessage message = Inbox.getInstance().retrieve(uid);
            ResponseBuilder response = Response.ok();
            message.writeInlineAttachment(name, response);
            return response.build();
        } catch (Throwable t) {
            log.error("Problem serving attachment", t);
            return error(t.getMessage());
        }
    }

}
