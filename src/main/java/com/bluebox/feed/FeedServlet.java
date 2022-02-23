package com.bluebox.feed;

import com.bluebox.Config;
import com.bluebox.Utils;
import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.bluebox.smtp.storage.StorageFactory;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample Servlet that serves a feed created with Rome.
 * <p>
 * The feed type is determined by the 'type' request parameter, if the parameter is missing it defaults
 * to the 'default.feed.type' servlet init parameter, if the init parameter is missing it defaults to 'atom_0.3'
 * <p>
 *
 * @author Alejandro Abdelnur
 */
@WebServlet(
        displayName = "FeedServlet",
        name = "feedservlet",
        urlPatterns = {"/feed/*"},
        initParams = {@WebInitParam(name = "default.feed.type", value = "rss_2.0")}
)
public class FeedServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(FeedServlet.class);
    private static final long serialVersionUID = 596015600474539028L;
    private static final String DEFAULT_FEED_TYPE = "default.feed.type";
    private static final String FEED_TYPE = "type";
    private static final String MIME_TYPE = "application/xml; charset=UTF-8";
    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

    private String _defaultFeedType;

    public void init() {
        _defaultFeedType = getServletConfig().getInitParameter(DEFAULT_FEED_TYPE);
        _defaultFeedType = (_defaultFeedType != null) ? _defaultFeedType : "atom_0.3";
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            SyndFeed feed = getFeed(null, req);
            String feedType = req.getParameter(FEED_TYPE);
            feedType = (feedType != null) ? feedType : _defaultFeedType;
            feed.setFeedType(feedType);
            res.setContentType(MIME_TYPE);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, res.getWriter());
        } catch (FeedException ex) {
            String msg = COULD_NOT_GENERATE_FEED_ERROR;
            log.error(msg, ex);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    protected SyndFeed getFeed(Inbox inbox, HttpServletRequest req) throws IOException, FeedException {
        String email = req.getParameter("email");
        SyndFeed feed = new SyndFeedImpl();

        feed.setDescription("Bluebox Inbox Syndication Feed");
        SyndImageImpl image = new SyndImageImpl();
        image.setUrl(Utils.getServletBase(req) + "/app/" + Config.getInstance().getString("bluebox_theme") + "/message.png");
        image.setLink(Utils.getServletBase(req) + "/app/" + Config.getInstance().getString("bluebox_theme") + "/message.png");
        image.setTitle(feed.getDescription());
        feed.setImage(image);
        feed.setTitle("Inbox for " + email);
        feed.setAuthor(email);
        feed.setLink(Utils.getServletBase(req) + "/app/inbox.jsp?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8));

        try {
            InboxAddress inboxA = new InboxAddress(email);

            List<BlueboxMessage> messages = StorageFactory.getInstance().listMail(inboxA, BlueboxMessage.State.NORMAL, 0, 15, BlueboxMessage.RECEIVED, false);
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            SyndEntry entry;
            SyndContent description;
            for (BlueboxMessage message : messages) {
                MimeMessage msg = message.getBlueBoxMimeMessage();
                entry = new SyndEntryImpl();
                entry.setTitle(message.getBlueBoxMimeMessage().getSubject());
                entry.setLink(Utils.getServletBase(req) + "/app/inbox.jsp?" + URLEncoder.encode(message.getInbox().toString(), StandardCharsets.UTF_8));
                entry.setPublishedDate(message.getReceived());
                entry.setUpdatedDate(message.getReceived());
                if (msg.getFrom() != null)
                    entry.setAuthor(msg.getFrom()[0].toString());

                description = new SyndContentImpl();
                if (message.getHtml(req).length() > 0) {
                    description.setType("text/html");
                    description.setValue(MimeUtility.encodeText(message.getHtml(req)));
                } else {
                    description.setType("text/plain");
                    description.setValue(MimeUtility.encodeText(message.getText()));
                }

                entry.setDescription(description);
                entries.add(entry);
            }

            feed.setEntries(entries);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return feed;
    }

}