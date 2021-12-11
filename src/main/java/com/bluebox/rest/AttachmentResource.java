package com.bluebox.rest;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.IOException;

@Path(AttachmentResource.PATH)
@MultipartConfig
public class AttachmentResource extends AbstractResource {
    public static final String PATH = "/attachment";
    public static final String UID = "uid";
    private static final Logger log = LoggerFactory.getLogger(AttachmentResource.class);
    private static final String INDEX = "index";
    private static final String NAME = "name";

    public AttachmentResource(Inbox inbox) {
        super(inbox);
    }

    @GET
    @Path("get/{uid}/{index}/{name:.*}")
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response detail(
            @Context HttpServletRequest request,
            @PathParam(UID) String uid,
            @PathParam(INDEX) String index,
            @PathParam(NAME) String name) throws IOException {
        try {

            log.debug("Serving file attachment at index {} for message {}" + index, uid);
            BlueboxMessage message = Inbox.getInstance().retrieve(uid);
            ResponseBuilder response = Response.ok();
            message.writeAttachment(index, response);
            Response built = response.build();
            return built;
        } catch (Throwable t) {
            log.error("Problem listing inbox", t);
            return error(t.getMessage());
        }
    }

}
