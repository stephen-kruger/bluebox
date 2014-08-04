package com.bluebox.feed;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

/**
 * Sample Servlet that serves a feed created with Rome.
 * <p>
 * The feed type is determined by the 'type' request parameter, if the parameter is missing it defaults
 * to the 'default.feed.type' servlet init parameter, if the init parameter is missing it defaults to 'atom_0.3'
 * <p>
 * @author Alejandro Abdelnur
 *
 */
public class FeedServlet extends HttpServlet {

	private static final long serialVersionUID = 596015600474539028L;
	private static final String DEFAULT_FEED_TYPE = "default.feed.type";
	private static final String FEED_TYPE = "type";
	private static final String MIME_TYPE = "application/xml; charset=UTF-8";
	private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

	private String _defaultFeedType;

	public void init() {
		_defaultFeedType = getServletConfig().getInitParameter(DEFAULT_FEED_TYPE);
		_defaultFeedType = (_defaultFeedType!=null) ? _defaultFeedType : "atom_0.3";
	}

	public void doGet(HttpServletRequest req,HttpServletResponse res) throws IOException {
		try {
			SyndFeed feed = getFeed(req);

			String feedType = req.getParameter(FEED_TYPE);
			feedType = (feedType!=null) ? feedType : _defaultFeedType;
			feed.setFeedType(feedType);

			res.setContentType(MIME_TYPE);
			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed,res.getWriter());
		}
		catch (FeedException ex) {
			String msg = COULD_NOT_GENERATE_FEED_ERROR;
			log(msg,ex);
			res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,msg);
		}
	}

	protected SyndFeed getFeed(HttpServletRequest req) throws IOException,FeedException {
		String email = req.getParameter("email");
		SyndFeed feed = new SyndFeedImpl();

		feed.setDescription("Bluebox Inbox Syndication Feed");
		//		feed.setLogo(uri.getBaseUri().toString()+"../app/"+Config.getInstance().getString("bluebox_theme")+"/message.png");
		//		feed.setIcon(uri.getBaseUri().toString()+"../app/"+Config.getInstance().getString("bluebox_theme")+"/favicon.ico");
		feed.setTitle("Inbox for "+email);
		feed.setAuthor(email);
		feed.setLink(req.getContextPath()+"../app/inbox.jsp?email="+URLEncoder.encode(email,"UTF-8"));

		try {
			InboxAddress inbox = new InboxAddress(email);
			List<BlueboxMessage> messages = Inbox.getInstance().listInbox(inbox, BlueboxMessage.State.NORMAL, 0, 10, BlueboxMessage.RECEIVED, false);
			List<SyndEntry> entries = new ArrayList<SyndEntry>();
			SyndEntry entry;
			SyndContent description;
			for (BlueboxMessage message : messages) {
				MimeMessage msg = message.getBlueBoxMimeMessage();
				entry = new SyndEntryImpl();
				entry.setTitle(message.getBlueBoxMimeMessage().getSubject());
				// http://localhost:8080/bluebox/rest/json/inbox/detail/d976d0ee-d5bf-4f72-b6e8-187965e1acea
				entry.setLink(req.getContextPath()+"/rest/json/inbox/detail/"+message.getIdentifier());
				entry.setPublishedDate(new Date(message.getLongProperty(BlueboxMessage.RECEIVED)));
				entry.setUpdatedDate(new Date(message.getLongProperty(BlueboxMessage.RECEIVED)));
				if (msg.getFrom()!=null)
					entry.setAuthor(msg.getFrom()[0].toString());
				
				description = new SyndContentImpl();
				if (message.getHtml().length()>0) {
					description.setType("text/html");
					description.setValue(message.getHtml());					
				}
				else {
					description.setType("text/plain");
					description.setValue(message.getText());
				}

				entry.setDescription(description);
				entries.add(entry);
			}

			feed.setEntries(entries);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		return feed;
	}

}