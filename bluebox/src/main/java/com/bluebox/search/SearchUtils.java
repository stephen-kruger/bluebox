package com.bluebox.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;
import com.bluebox.smtp.storage.BlueboxMessage;

public class SearchUtils {
	private static final Logger log = LoggerFactory.getLogger(SearchUtils.class);
	public enum SearchFields {UID, INBOX, FROM, SUBJECT, RECEIVED, TEXT_BODY, HTML_BODY, SIZE, RECIPIENT, RECIPIENTS, ANY, BODY};
	public enum SortFields {
		SORT_RECEIVED, SORT_SIZE;
		 public static SortFields getEnum(String value) {
		        for(SortFields v : values())
		            if(value.toUpperCase().indexOf(v.name())>=0)
		            	return v;
		        return SORT_RECEIVED;
		    }
		};
	public static final int MAX_COMMIT_INTERVAL = 20000; // ensure at least some time between unforced commits

//	public static String substringQuery(String querystr) {
//		//              querystr = QueryParser.escape(querystr);
//		//              querystr = "*"+QueryParser.escape(querystr)+"*";
//		//              querystr = "*"+querystr+"*";
//		if ((querystr==null)||(querystr.length()==0)) {
//			return "*";
//		}
//		else {
//			@SuppressWarnings("unused")
//			boolean leadingWC=false, trailingWC=false;
//			if (querystr.endsWith("*")) {
//				querystr = querystr.substring(0,querystr.length()-1);
//				trailingWC=true;
//			}
//			if (querystr.startsWith("*")) {
//				querystr = querystr.substring(1,querystr.length());
//				leadingWC = true;
//			}
//			querystr = QueryParser.escape(querystr);
//
//			if (leadingWC)
//				querystr = "*"+querystr;
////			if (trailingWC) 
//				querystr = querystr+"*";
//		}
//		return querystr;
//	}

	public static String plainQuery(String querystr) {
		return querystr;
	}

	public static String autocompleteQuery(String querystr) {
		if ((querystr==null)||(querystr.length()==0)) {
			return "*";
		}
		else {
			if (querystr.endsWith("*")) {
				querystr = querystr.substring(0,querystr.length()-1);
			}
			if (querystr.startsWith("*")) {
				querystr = querystr.substring(1,querystr.length());
			}
			querystr = QueryParser.escape(querystr);
//			querystr = querystr+"*";
			//			querystr = querystr;
		}
		return querystr;
	}

	/*
	 * Figure out which of the recipients this mail is actually being delivered to. If none match, use the Inbox as default;
	 */
	public static String getRecipient(String recipients, String inbox) {
		StringTokenizer tok = new StringTokenizer(recipients,",");
		String linbox = inbox.toLowerCase();
		while (tok.hasMoreElements()) {
			String curr = tok.nextToken();
			if (curr.toLowerCase().contains(linbox)) {
				return curr;
			}
		}
		return inbox;
	}
	
	/* Find which one of the potential recipeints of this mail matches the specified inbox
	 * 
	 */
	public static InboxAddress getRecipient(InboxAddress inbox, String recipients) {
		StringTokenizer tok = new StringTokenizer(recipients,",");
		while (tok.hasMoreElements()) {
			try {
				InboxAddress curr = new InboxAddress(Utils.decodeRFC2407(tok.nextToken()));
				if (inbox.getAddress().equalsIgnoreCase(curr.getAddress()))
					return curr;
			}
			catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return inbox;
	}

	public static  String getRecipients(BlueboxMessage message) throws Exception {
		MimeMessage bbmm = message.getBlueBoxMimeMessage();
		StringBuffer sb = new StringBuffer();
		Address[] addr = bbmm.getAllRecipients();
		if (addr!=null) {
			for (int i = 0; i < addr.length;i++) {
				sb.append(Utils.decodeQuotedPrintable(addr[i].toString())).append(",");
			}
		}
		return sb.toString().trim();
	}
	
	/*
	 * Convert the specified html string to a text only rendering of the final html content
	 */
	public static String htmlToString(String html) throws IOException {
		final StringBuilder sb = new StringBuilder();
		HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
			public boolean readyForNewline;

			@Override
			public void handleText(final char[] data, final int pos) {
				String s = new String(data);
				sb.append(s.trim()).append(' ');
				readyForNewline = true;
			}

			@Override
			public void handleStartTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				if (readyForNewline && (t == HTML.Tag.DIV || t == HTML.Tag.BR || t == HTML.Tag.P)) {
					sb.append("\n");
					readyForNewline = false;
				}
			}

			@Override
			public void handleSimpleTag(final HTML.Tag t, final MutableAttributeSet a, final int pos) {
				handleStartTag(t, a, pos);
			}
		};
		if (html!=null) {
			if (html.length()>0) {
				try {
					new ParserDelegator().parse(new StringReader(html), parserCallback, false);
				}
				catch (Throwable t) {
					log.warn("Could not parse html content - indexing all ({})",t.getMessage());
					sb.append(html);
				}
			}
		}
		return sb.toString().trim();
	}
}
