package com.bluebox.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchUtils {
	private static final Logger log = LoggerFactory.getLogger(SearchUtils.class);
	public enum SearchFields {UID, INBOX, FROM, SUBJECT, RECEIVED, TEXT_BODY, HTML_BODY, SIZE, RECIPIENT, RECIPIENTS, ANY, BODY};

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
