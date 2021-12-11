package com.bluebox.rest;

import com.bluebox.WorkerThread;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path(StatsResource.PATH)
public class StatsResource extends AbstractResource {
    public static final String MPH_STAT = "stats_mph";
    public static final String GLOBAL_STAT = "stats_global";
    public static final String RECENT_STAT = "stats_recent";
    public static final String ACTIVE_STAT = "stats_active";
    public static final String SENDER_STAT = "stats_sender";
    public static final String COMBINED_STAT = "stats_combined";
    public static final String PATH = "/stats";
    private static final Logger log = LoggerFactory.getLogger(StatsResource.class);

    public StatsResource(Inbox inbox) {
        super(inbox);
    }

    @GET
    @Path("mph/{email: .*}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mph(
            @PathParam(EMAIL) String emailStr) throws IOException {

        try {
            JSONObject result = Inbox.getInstance().getMPH(new InboxAddress(emailStr));
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing mails per hour", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("combined")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response combined() {
        try {
            JSONObject result = new JSONObject();
            Inbox inbox = Inbox.getInstance();
            result.put(BlueboxMessage.COUNT, inbox.getStatsGlobalCount());
            result.put("countAll", inbox.getMailCount(BlueboxMessage.State.ANY));
            result.put("recent", inbox.getStatsRecent());
            result.put("active", inbox.getStatsActiveInbox());
            result.put("sender", inbox.getStatsActiveSender());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing combined stats", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("sender")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sender() {
        try {
            JSONObject result = new JSONObject();
            Inbox inbox = Inbox.getInstance();
            result.put("sender", inbox.getStatsActiveSender());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing combined stats", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("active")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response active() {
        try {
            JSONObject result = new JSONObject();
            Inbox inbox = Inbox.getInstance();
            result.put("active", inbox.getStatsActiveInbox());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing active stats", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("recent")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recent() {
        try {
            JSONObject result = new JSONObject();
            Inbox inbox = Inbox.getInstance();
            result.put("recent", inbox.getStatsRecent());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing recent stats", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("global")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response global() {
        try {
            JSONObject result = new JSONObject();
            Inbox inbox = Inbox.getInstance();
            result.put(BlueboxMessage.COUNT, inbox.getStatsGlobalCount());
            result.put("countAll", inbox.getMailCount(BlueboxMessage.State.ANY));
            result.put("countError", inbox.errorCount());
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing recent stats", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("workerstatus")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response workerStatus() throws IOException {

        try {
            return Response.ok(WorkerThread.getWorkerStatus().toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem retrieving worker status", t);
            return error(t.getMessage());
        }
    }
}
