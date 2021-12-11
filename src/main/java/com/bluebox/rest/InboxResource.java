package com.bluebox.rest;

import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.search.SearchUtils;
import com.bluebox.search.SearchUtils.SearchFields;
import com.bluebox.search.SearchUtils.SortFields;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

@Path(InboxResource.PATH)
@MultipartConfig
public class InboxResource extends AbstractResource {
    public static final String PATH = "/inbox";
    public static final String UID = "uid";
    private static final Logger log = LoggerFactory.getLogger(InboxResource.class);

    public InboxResource(Inbox inbox) {
        super(inbox);
    }

    private static BlueboxMessage.State extractState(String state) {
        try {
            return BlueboxMessage.State.values()[Integer.parseInt(state)];
        } catch (Throwable t) {
            return BlueboxMessage.State.NORMAL;
        }
    }

    @GET
    @Path("list/{" + EMAIL + ": .*}/{" + STATE + "}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listInbox(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @PathParam(EMAIL) String emailStr,
            @PathParam(STATE) String stateStr
    ) throws IOException {
        // check for form list/email/state or list/state
        if (stateStr == "") {
            stateStr = emailStr;
            emailStr = "";
        }
        DojoPager pager = new DojoPager(request, BlueboxMessage.RECEIVED);

        long startTime = new Date().getTime();
        // get the desired email
        InboxAddress inboxAddress;
        try {
            inboxAddress = new InboxAddress(emailStr);
        } catch (Throwable e) {
            inboxAddress = null;
        }

        // check if State was specified, else default to NORMAL
        BlueboxMessage.State state = extractState(stateStr);

        try {
            // tell the grid how many items we have
            long totalCount = getInbox().getMailCount(inboxAddress, state);
            pager.setRange(response, totalCount);
            StringWriter writer = new StringWriter();
            getInbox().listInbox(inboxAddress, state, writer, pager.getFirst(), pager.getCount(), pager.getOrderBy().get(0), pager.isAscending(0), request.getLocale());
            writer.flush();
            log.debug("Served inbox contents for {} first={} last={} in {}ms", inboxAddress, pager.getFirst(), pager.getCount(), (new Date().getTime() - startTime));
            return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing inbox", t);
            return error(t.getMessage());
        }
    }

    @DELETE
    @Path("spam/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response spam(
            @Context HttpServletRequest request,
            @PathParam(UID) String uidList) {
        try {
            StringTokenizer uidArray = new StringTokenizer(uidList, ",");
            List<String> uids = new ArrayList<String>();
            while (uidArray.hasMoreTokens()) {
                uids.add(uidArray.nextToken());
            }
            log.debug("Marking {} mails as SPAM", uids.size());
            WorkerThread wt = Inbox.getInstance().toggleSpam(uids);
            JSONObject result = new JSONObject();
            result.put("message", startWorker(wt, request));
            return Response.ok(result.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Error marking spam", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("search/{searchScope}/{searchString}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @PathParam("searchScope") String searchScopeStr,
            @PathParam("searchString") String searchStr
    ) throws IOException {
        DojoPager pager = new DojoPager(request, BlueboxMessage.RECEIVED);
        try {
            // tell the grid how many items we have
            SearchFields searchScope;
            try {
                searchScope = SearchUtils.SearchFields.valueOf(searchScopeStr);
            } catch (Throwable t) {
                log.error("Invalid search scope :{}", searchScopeStr);
                searchScope = SearchUtils.SearchFields.ANY;
            }
            StringWriter writer = new StringWriter();
            log.info("----->{} {}", searchScope, SortFields.getEnum(pager.getOrderBy().get(0)));
            long totalCount = Inbox.getInstance().searchInbox(searchStr, writer, pager.getFirst(), pager.getCount(), searchScope, SortFields.getEnum(pager.getOrderBy().get(0)), pager.isAscending(0));
            log.debug("Total result set was length {}", totalCount);
            pager.setRange(response, totalCount);
            return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
        } catch (Throwable t) {
            log.error("Problem listing inbox", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("updateavailable")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response updateavailable(
            @Context HttpServletRequest request) throws IOException {
        try {
            return Response.ok(Utils.updateAvailable().toString()).build();
        } catch (Throwable t) {
            log.error("Problem checking for updates", t);
            return error(t.getMessage());
        }
    }

    @GET
    @Path("test")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public String getTest() {
        log.info("Entering test method");
        return "{'XXX':'XXXX'}";
    }
}
