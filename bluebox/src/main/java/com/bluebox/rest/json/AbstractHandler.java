package com.bluebox.rest.json;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.bluebox.Utils;
import com.bluebox.smtp.InboxAddress;

public class AbstractHandler {
	private static final Logger log = Logger.getAnonymousLogger();

	protected void setDefaultHeaders(HttpServletResponse resp) {
		resp.setHeader("Pragma", "No-cache");
		resp.setDateHeader("Expires", 0);
		
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType("application/json");
		resp.setCharacterEncoding(Utils.UTF8);
	}

	/*
	 * This will extract the uri fragment at position index, where the last fragment is 0
	 * So for /aaa/bbb/ccc/ddd
	 * extractFragment(0) = ddd
	 * extractFragment(1) = ccc
	 * extractFragment(2) = bbb etc
	 * 
	 * 	for /aaa/bbb/ccc/ddd/
	 * extractFragment(0) = ""
	 * extractFragment(1) = "ddd"
	 */
	public static String extractFragment(String uri, int index) {
		if (uri.endsWith("/")) {
			if (index==0) {
				return "";
			}
			else {
				index--;
			}
		}
		StringTokenizer tok = new StringTokenizer(uri,"/",false);
		List<String> list = new ArrayList<String>();
		while (tok.hasMoreTokens()) {
			list.add(tok.nextToken());
		}
		return list.get(list.size()-index-1);
	}

	/*
	 * Form 1: looks for a uid in form xxx/yyy/zzz/uid/qqq
	 * where fragment = xxx/yyy/zzz/
	 * or
	 * Form 2: looks for a uid in form xxx/yyy/zzz/uid
	 * where fragment = xxx/yyy/zzz/
	 */
	public static String extractUid(String uri, String fragment) {
		// Form 1
		int start = uri.indexOf(fragment)+fragment.length()+1;
		int end = uri.indexOf("/", start+1);

		// Form 2
		if (end<0) {
			end = uri.length();
		}
		return uri.substring(start,end);
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
			log.severe("Problem decoding email : "+emailFragment);
			return "";
		}
	}
}
