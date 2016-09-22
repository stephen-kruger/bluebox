package com.bluebox.rest;

import java.io.File;
import java.io.IOException;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.WorkerThread;
import com.bluebox.smtp.BlueBoxSMTPServer;
import com.bluebox.smtp.Inbox;

@Path(AdminResource.PATH)
@DeclareRoles({"bluebox"})
@RolesAllowed({"bluebox"})
public class AdminResource extends AbstractResource {
    private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

    public static final String PATH = "/admin";
    public static final String COUNT = "count";
    public static final String BLACKLIST = "blacklist";
    public static final String WHITELIST = "whitelist";

    public AdminResource(Inbox inbox) {
	super(inbox);
    }

    @GET
    @Path("generate/{count}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response generate(
	    @Context HttpServletRequest request,
	    @PathParam(COUNT) int count) throws IOException {

	try {
	    log.info("Generating {} test emails",count);
	    WorkerThread wt = Utils.generate(request.getSession().getServletContext(), Inbox.getInstance(), count);
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem listing mails per hour",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("setbasecount/{count}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setbasecount(
	    @Context HttpServletRequest request,
	    @PathParam(COUNT) int count) throws IOException {

	try {
	    Inbox.getInstance().setStatsGlobalCount(count);
	    return Response.ok("Set global count to "+count).build();
	}
	catch (Throwable t) {
	    log.error("Problem setting count",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("rebuildsearchindexes")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response rebuildsearchindexes(
	    @Context HttpServletRequest request) throws IOException {

	try {
	    WorkerThread wt = Inbox.getInstance().rebuildSearchIndexes();
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem rebuilding indexes",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("trim")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response trim(
	    @Context HttpServletRequest request) throws IOException {

	try {
	    WorkerThread wt = Inbox.getInstance().trimThread();
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem trimming mails",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("startsmtp")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response startSMTP(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    log.info("Starting smtp...");
	    BlueBoxSMTPServer.getInstance(null).start();
	    return Response.ok("Started").build();
	}
	catch (Throwable t) {
	    log.error("Problem starting smtp server",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("stopsmtp")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopSMTP(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    log.info("Stopping smtp...");
	    BlueBoxSMTPServer.getInstance(null).stop();
	    return Response.ok("Stopped").build();
	}
	catch (Throwable t) {
	    log.error("Problem stopping smtp server",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("expire")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response expire(
	    @Context HttpServletRequest request) throws IOException {

	try {
	    WorkerThread wt = Inbox.getInstance().expireThread();
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem expiring mails",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("clearerrors")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clearErrors(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    Inbox.getInstance().clearErrors();
	    return Response.ok("Cleared errors").build();
	}
	catch (Throwable t) {
	    log.error("Problem clearing errors",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("clear")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clear(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    Inbox.getInstance().deleteAll();
	    return Response.ok("Cleared all mail").build();
	}
	catch (Throwable t) {
	    log.error("Problem clearing all mail",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("purge")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response purge(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    Inbox.getInstance().purge();
	    return Response.ok("Purged all mail").build();
	}
	catch (Throwable t) {
	    log.error("Problem purging all mail",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("maintenance")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response maintenance(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    WorkerThread wt = Inbox.getInstance().runMaintenance();
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem running maintenance",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("rawclean")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response rawClean(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    WorkerThread wt = Inbox.getInstance().cleanOrphans();
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem clearing raw orphans",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("backup")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response backup(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
	    f.mkdir();
	    WorkerThread wt = Inbox.getInstance().backup(f);
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem backing up mail",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("restore")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response restore(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
	    WorkerThread wt = Inbox.getInstance().restore(f);
	    return Response.ok(startWorker(wt, request)).build();
	}
	catch (Throwable t) {
	    log.error("Problem restoring up mail",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("clean")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clean(
	    @Context HttpServletRequest request) throws IOException {
	try {
	    File f = new File(System.getProperty("java.io.tmpdir")+File.separator+"bluebox.backup");
	    FileUtils.deleteDirectory(f);
	    return Response.ok("Cleaned "+f.getCanonicalPath()).build();
	}
	catch (Throwable t) {
	    log.error("Problem cleaning backups",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("settoblacklist/{blacklist}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response settoblacklist(
	    @Context HttpServletRequest request,
	    @PathParam(BLACKLIST) String blacklist) throws IOException {
	try {
	    Config.getInstance().setString(Config.BLUEBOX_TOBLACKLIST, blacklist);
	    Inbox.getInstance().loadConfig();
	    return Response.ok("Set To blacklist").build();
	}
	catch (Throwable t) {
	    log.error("Problem setting blacklist",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("setfromblacklist/{blacklist}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setfromblacklist(
	    @Context HttpServletRequest request,
	    @PathParam(BLACKLIST) String blacklist) throws IOException {
	try {
	    Config.getInstance().setString(Config.BLUEBOX_FROMBLACKLIST, blacklist);
	    Inbox.getInstance().loadConfig();
	    return Response.ok("Set To blacklist").build();
	}
	catch (Throwable t) {
	    log.error("Problem setting blacklist",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("settowhitelist/{whitelist}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response settowhitelist(
	    @Context HttpServletRequest request,
	    @PathParam(WHITELIST) String whitelist) throws IOException {
	try {
	    Config.getInstance().setString(Config.BLUEBOX_TOWHITELIST, whitelist);
	    Inbox.getInstance().loadConfig();
	    return Response.ok("Set settowhitelist").build();
	}
	catch (Throwable t) {
	    log.error("Problem setting settowhitelist",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("setfromwhitelist/{whitelist}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setfromwhitelist(
	    @Context HttpServletRequest request,
	    @PathParam(WHITELIST) String whitelist) throws IOException {
	try {
	    Config.getInstance().setString(Config.BLUEBOX_FROMWHITELIST, whitelist);
	    Inbox.getInstance().loadConfig();
	    return Response.ok("Set setfromwhitelist").build();
	}
	catch (Throwable t) {
	    log.error("Problem setting setfromwhitelist",t);
	    return error(t.getMessage());
	}
    }

    @GET
    @Path("setsmtpblacklist/{blacklist}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setsmtpblacklist(
	    @Context HttpServletRequest request,
	    @PathParam(BLACKLIST) String blacklist) throws IOException {
	try {
	    Inbox.getInstance().setSMTPBlacklist(blacklist);
	    return Response.ok("Set setsmtpblacklist").build();
	}
	catch (Throwable t) {
	    log.error("Problem setting setsmtpblacklist",t);
	    return error(t.getMessage());
	}
    }
}
