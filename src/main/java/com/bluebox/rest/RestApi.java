package com.bluebox.rest;

import com.bluebox.smtp.Inbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath(RestApi.APPLICATION_PATH)
//@MultipartConfig(location="c:/Temp", fileSizeThreshold=1024*1024, maxFileSize=1024*1024*5, maxRequestSize=1024*1024*5*5)
public class RestApi extends Application {

    public static final String APPLICATION_PATH = "/jaxrs";

    /**
     * The Constant log.
     */
    private static final Logger log = LoggerFactory.getLogger(RestApi.class);


    /**
     * Instantiates a new ws api.
     */
    public RestApi() {
        log.info("Instantiating REST api");
        //javax.xml.bind.annotation.XmlRootElement xxx;
    }

    @Override
    public Set<Object> getSingletons() {
        log.debug("Returning singletons");
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new AdminResource(Inbox.getInstance()));
        singletons.add(new AttachmentResource(Inbox.getInstance()));
        singletons.add(new AutoCompleteResource(Inbox.getInstance()));
        singletons.add(new ChartResource(Inbox.getInstance()));
        singletons.add(new ErrorResource(Inbox.getInstance()));
        singletons.add(new FolderResource(Inbox.getInstance()));
        singletons.add(new HeaderResource(Inbox.getInstance()));
        singletons.add(new InboxResource(Inbox.getInstance()));
        singletons.add(new InlineResource(Inbox.getInstance()));
        singletons.add(new StatsResource(Inbox.getInstance()));
        singletons.add(new MessageResource(Inbox.getInstance()));
        return singletons;
    }
}