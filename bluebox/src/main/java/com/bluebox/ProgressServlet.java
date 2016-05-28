package com.bluebox;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(
		displayName="ProgressServlet",
		name="progressservlet", 
		urlPatterns={"/progress/*"}
		)
public class ProgressServlet extends HttpServlet {

	private static final Logger log = Logger.getAnonymousLogger();
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("text/html");
		response.setHeader("Cache-Control", "no-cache");
		
		PrintWriter out = response.getWriter();

		HttpSession session = request.getSession(true);
		if (session == null) {
			log.info("session is null");
			out.println("Sorry, session is null"); // just to be safe
			return;
		}

		TestProgressListener testProgressListener = (TestProgressListener) session.getAttribute("testProgressListener");
		if (testProgressListener == null) {
			log.info("Progress listener is null");
			out.println("Progress listener is null");
			return;
		}

//		out.println(testProgressListener.getMessage());

		out.println(testProgressListener.getPercentDone());

	}
}

