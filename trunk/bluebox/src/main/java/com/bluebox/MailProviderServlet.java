package com.bluebox;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.abdera.protocol.server.Provider;
import org.apache.abdera.protocol.server.impl.DefaultProvider;
import org.apache.abdera.protocol.server.impl.SimpleWorkspaceInfo;
import org.apache.abdera.protocol.server.servlet.AbderaServlet;

import com.bluebox.feed.MailCollectionAdapter;

public class MailProviderServlet extends AbderaServlet {
	private static final Logger log = Logger.getAnonymousLogger();
	private static final long serialVersionUID = 7739654640797497960L;
//	private Inbox inbox = BlueBoxServlet.inbox;

	@Override
	public void init() throws ServletException {
		super.init();
		try {
			log.info("Initialised BlueBox atom servlet at "+getServletContext().getServletContextName());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	protected Provider createProvider() {
		MailCollectionAdapter ca = new MailCollectionAdapter();
		ca.setHref("mail");

		SimpleWorkspaceInfo wi = new SimpleWorkspaceInfo();
		wi.setTitle("BlueBox Workspace");
		wi.addCollection(ca);

		DefaultProvider provider = new DefaultProvider("/atom/");
		provider.addWorkspace(wi);

		provider.init(getAbdera(), null);
		return provider;
	}

}
