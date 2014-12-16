package com.bluebox.rest.json;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;

public class AbstractHandler {
	private static final Logger log = LoggerFactory.getLogger(AbstractHandler.class);
	public static final String JSON_CONTENT_TYPE = "text/x-json;charset=UTF-8";

	protected void setDefaultHeaders(HttpServletResponse resp) {
		resp.setHeader("Pragma", "No-cache");
		resp.setDateHeader("Expires", 0);

		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType("application/json");
		resp.setCharacterEncoding(Utils.UTF8);
	}

	/*
	 * This will extract the fragment following an expected root prefix
	 */
	public static String extractFragment(String uri, String root, int index) {
		return extractFragment(uri.substring(uri.indexOf(root)+root.length()),index);
	}

	/*
	 * This will extract the uri fragment at position index, where the last fragment is 0
	 * So for /aaa/bbb/ccc/ddd
	 * extractFragment(0) = aaa
	 * extractFragment(1) = bbb
	 * extractFragment(2) = ccc
	 * extractFragment(3) = ddd etc
	 * 
	 * 	for /aaa/bbb/ccc/ddd/
	 * extractFragment(0) = aaa
	 * extractFragment(3) = ddd
	 * extractFragment(3) = ""
	 */
	public static String extractFragment(String uri, int index) {
		try {
			StringTokenizer tok = new StringTokenizer(uri,"/",true);
			List<String> list = new ArrayList<String>();
			String token="", prevToken;
			while (tok.hasMoreTokens()) {
				prevToken = token;
				token = tok.nextToken();
				if ("/".equals(token)) {
					// check for form xxx//zzz
					if ("/".equals(prevToken))
						list.add("");
				}
				else {
					try {
						list.add(URLDecoder.decode(token,Utils.UTF8));
					} catch (UnsupportedEncodingException e) {
						list.add(token);
						e.printStackTrace();
					}
				}
			}
			return list.get(index);
		}
		catch (Throwable ex) {
			// this happens for the form /aa/bb/cc/ when you get index 3
			return "";
		}
	}

	protected static String extractEmail(String emailFragment) {
		try {
			emailFragment = URLDecoder.decode(emailFragment,Utils.UTF8);
			if ((emailFragment == null)||(emailFragment.trim().length()==0)||emailFragment.equals("*")) {
				return emailFragment = "";
			}
			return new InboxAddress(emailFragment).getAddress();
		}
		catch (Throwable e) {
			log.error("Problem decoding email : {}",emailFragment);
			return "";
		}
	}
}
