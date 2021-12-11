package com.bluebox.rest;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

@Path(FolderResource.PATH)
@MultipartConfig
public class FolderResource extends AbstractResource {
    public static final String PATH = "/folder";
    private static final Logger log = LoggerFactory.getLogger(FolderResource.class);

    public FolderResource(Inbox inbox) {
        super(inbox);
    }

    @GET
    @Path("counts/{email: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listInbox(
            @Context HttpServletRequest request,
            @DefaultValue("") @PathParam(EMAIL) String emailStr) throws IOException {
        try {
            JSONObject result = doGetFolderJson(emailStr, request.getLocale());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing inbox", t);
            return error(t.getMessage());
        }
    }

    public JSONObject doGetFolderJson(String emailStr, Locale locale) throws IOException {
        try {
            InboxAddress email = null;
            if (emailStr.trim().length() > 0) {
                if (emailStr.startsWith("*@")) {
                    emailStr = "";
                } else {
                    email = new InboxAddress(emailStr);
                    emailStr = email.getAddress();
                }
            }

            log.debug("Serving folder count for {}", emailStr);
            ResourceBundle rb = ResourceBundle.getBundle("folderDetails", locale);

            JSONObject folders = new JSONObject();
            folders.put("id", "Overview");
            folders.put("name", rb.getString("inboxfor") + " " + emailStr);
            folders.put("type", "folder");
            folders.put("style", "rootFolder");

            JSONObject normal = new JSONObject();
            normal.put("id", rb.getString("inbox"));
            long normalCount = StorageFactory.getInstance().getMailCount(email, BlueboxMessage.State.NORMAL);
            normal.put("name", rb.getString("inbox") + " (" + normalCount + ")");
            normal.put("count", normalCount);
            normal.put("email", emailStr);
            normal.put("state", BlueboxMessage.State.NORMAL.name());
            normal.put("style", "inboxFolder");
            folders.put(BlueboxMessage.State.NORMAL.name(), normal);

            JSONObject deleted = new JSONObject();
            deleted.put("id", rb.getString("trash"));
            long trashCount = StorageFactory.getInstance().getMailCount(email, BlueboxMessage.State.DELETED);
            deleted.put("name", rb.getString("trash") + " (" + trashCount + ")");
            deleted.put("count", trashCount);
            deleted.put("email", emailStr);
            deleted.put("state", BlueboxMessage.State.DELETED.name());
            deleted.put("style", "trashFolder");
            folders.put(BlueboxMessage.State.DELETED.name(), deleted);

            JSONObject all = new JSONObject();
            all.put("id", rb.getString("all"));
            long allCount = StorageFactory.getInstance().getMailCount(email, BlueboxMessage.State.ANY);//normalCount+trashCount;
            all.put("name", rb.getString("allDocuments") + " (" + allCount + ")");
            all.put("count", allCount);
            all.put("email", emailStr);
            all.put("state", BlueboxMessage.State.ANY.name());
            all.put("style", "allFolder");
            folders.put(BlueboxMessage.State.ANY.name(), all);

            return folders;
        } catch (Throwable t) {
            log.error(t.getMessage());
            t.printStackTrace();
            return null;
        }
    }
}
