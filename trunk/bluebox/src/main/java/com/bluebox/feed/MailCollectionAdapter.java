package com.bluebox.feed;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.abdera.i18n.iri.IRI;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Person;
import org.apache.abdera.protocol.server.RequestContext;
import org.apache.abdera.protocol.server.context.ResponseContextException;
import org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class MailCollectionAdapter extends AbstractEntityCollectionAdapter<MailEntry> {
	private static final Logger log = Logger.getAnonymousLogger();

	public MailCollectionAdapter() {
	}

	@Override
	public String getTitle(RequestContext request) {
		return "BlueBox Atom Feed";
	}

	@Override
	public void deleteEntry(String resourceName, RequestContext request)
			throws ResponseContextException {
		log.info("Not yet implemented");
	}

	@Override
	public Object getContent(MailEntry entry, RequestContext request)
			throws ResponseContextException {
		return entry.getContent();
	}

	@Override
	public Iterable<MailEntry> getEntries(RequestContext request) throws ResponseContextException {
		log.info("getEntries");
		List<MailEntry> list = new ArrayList<MailEntry>();
		try {
			List<BlueboxMessage> mlist = Inbox.getInstance().listInbox(null, BlueboxMessage.State.ANY, 0, 10, BlueboxMessage.RECEIVED, true);
			for (BlueboxMessage m : mlist) {
				list.add(new MailEntry(m));
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	@Override
	public MailEntry getEntry(String resourceName, RequestContext request)
			throws ResponseContextException {
		log.info("getEntry");
		try {
			return new MailEntry(Inbox.getInstance(), resourceName);
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new ResponseContextException(e.getMessage(), 0);
		}
	}

	@Override
	public String getId(MailEntry entry) throws ResponseContextException {
		return entry.getId();
	}

	@Override
	public String getName(MailEntry entry) throws ResponseContextException {
		return entry.getName();
	}

	@Override
	public String getTitle(MailEntry entry) throws ResponseContextException {
		return entry.getTitle();
	}

	@Override
	public Date getUpdated(MailEntry entry) throws ResponseContextException {
		return entry.getUpdated();
	}

	@Override
	public MailEntry postEntry(String resourceName, IRI id, String summary, Date updated,
			List<Person> authors, Content content, RequestContext request) throws ResponseContextException {
		log.info("Not yet implemented");
		return null;
	}

	@Override
	public void putEntry(MailEntry entry, 
			String title,
			Date updated,
			List<Person> authors,
			String summary,
			Content content,
			RequestContext request)
					throws ResponseContextException {
		log.info("Not yet implemented");
	}

	@Override
	public String getAuthor(RequestContext request)
			throws ResponseContextException {
		return "BlueBox";
	}

	@Override
	public String getId(RequestContext request) {
		return "BlueBox";
	}

}
