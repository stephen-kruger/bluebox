package com.bluebox.feed;

import java.util.Date;

import com.bluebox.smtp.Inbox;
import com.bluebox.smtp.storage.BlueboxMessage;

public class MailEntry {

	private String id;
	private String name, title, content, link;
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	private Date updated;
	
//	public MailEntry(String id, String name, String title, Date updated) {
//		setId(id);
//		setName(name);
//		setTitle(title);
//		setUpdated(updated);
//		setLink("http://slashdot.org");
//	}
	
	public MailEntry(BlueboxMessage m) {
		try {
			setId(m.getIdentifier());
			setTitle(m.getIdentifier());
			setName(m.getProperty(BlueboxMessage.FROM));
			setUpdated(new Date(Long.parseLong(m.getProperty(BlueboxMessage.RECEIVED))));
			setContent(m.getProperty(BlueboxMessage.SUBJECT));
		} 
		catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public MailEntry(Inbox inbox, String messageId) throws Exception {
		this(inbox.retrieve(messageId));
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}


}
