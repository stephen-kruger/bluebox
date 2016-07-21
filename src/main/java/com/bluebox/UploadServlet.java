package com.bluebox;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.bluebox.smtp.storage.StorageFactory;

@WebServlet(
		displayName="UploadServlet",
		name="uploadservlet", 
		urlPatterns={"/upload/*"}
		)
public class UploadServlet extends HttpServlet {

	private static final org.slf4j.Logger log = LoggerFactory.getLogger(UploadServlet.class);
	private static final long serialVersionUID = 1L;
	public static final long MAX_UPLOAD_IN_MEGS = 5;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		request.setCharacterEncoding("UTF-8");
		// Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();

		// Configure a repository (to ensure a secure temp location is used)
		ServletContext servletContext = this.getServletConfig().getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		try {
			JSONArray result = new JSONArray();
			List<FileItem> items = upload.parseRequest(request);
			for (FileItem item : items) {
				if (item.isFormField()) {
					log.info("Ignoring form field");
				}
				else {
					try {
						JSONObject res = new JSONObject();
						res.put("file", item.getName());
						res.put("type", "eml");
						res.put("width", "0");
						res.put("height", "0");
						try {

							if (item.getSize()>0) {
								log.info("Loading file {} with size {}",item.getName(),item.getSize());
								MimeMessage message = Utils.loadEML(item.getInputStream());
								Utils.sendMessageDirect(StorageFactory.getInstance(),message);
							}
							else {
								log.warn("Ignoring empty file upload");
								res.put("error", "Empty file specified");
							}
							result.put(res);
						}
						catch (Throwable t) {
							log.error(t.getMessage());
							res.put("error", t.getMessage());
						}
					}
					catch (JSONException je) {
						je.printStackTrace();
					}
				}
			}
			response.getWriter().write(result.toString());
		} 
		catch (FileUploadException e) {
			e.printStackTrace();
		}
		response.setStatus(200);
		response.flushBuffer();
	}

//	public void doPostOld(HttpServletRequest request, HttpServletResponse response) throws IOException {
//		response.setContentType("text/html");
//		PrintWriter out = response.getWriter();
//		printJSP(request,response,"/upload_response_header.jsp");
//		//		printHead(out);
//		boolean isMultipartContent = ServletFileUpload.isMultipartContent(request);
//		if (!isMultipartContent) {
//			out.println("You did not select any files to upload<br/>");
//			return;
//		}
//
//		FileItemFactory factory = new DiskFileItemFactory();
//		ServletFileUpload upload = new ServletFileUpload(factory);
//		upload.setSizeMax(MAX_UPLOAD_IN_MEGS * 1024 * 1024);
//
//		TestProgressListener testProgressListener = new TestProgressListener();
//		upload.setProgressListener(testProgressListener);
//
//		HttpSession session = request.getSession();
//		session.setAttribute("testProgressListener", testProgressListener);
//
//		try {
//			List<FileItem> fields = upload.parseRequest(request);
//			Iterator<FileItem> it = fields.iterator();
//			if (!it.hasNext()) {
//				out.println("No fields found");
//				return;
//			}
//			out.println("<table border=\"1\">");
//			while (it.hasNext()) {
//				out.println("<tr>");
//				FileItem fileItem = it.next();
//				boolean isFormField = fileItem.isFormField();
//				if (isFormField) {
//					//					out.println("<td>regular form field</td><td>Field name: " + fileItem.getFieldName() + "<br/>STRING: " + fileItem.getString());
//					//					out.println("</td>");
//					log.info("Ignoring form field "+fileItem.getName());
//				} 
//				else {
//					String message;
//					try {
//						if (fileItem.getSize()>0) {
//							log.info("Loading file "+fileItem.getName()+" with size "+fileItem.getSize());
//							message = "<font color=\"green\">"+Utils.uploadEML(fileItem.getInputStream())+"</font>";
//						}
//						else {
//							message = "<font color=\"blue\">No file was specified - ignoring</font>";
//						}
//					}
//					catch (Throwable t) {
//						message = "<font color=\"red\">Invalid email (no recipients?) - "+t.getMessage()+"</font>";
//					}
//					out.println("<td>"+fileItem.getFieldName()+"</td><td>" +
//							"Name: <b>" + fileItem.getName() + "</b>" +
//							"<br/>Content type: <b>" + fileItem.getContentType() + "</b>" +
//							"<br/>Size (bytes): <b>" + fileItem.getSize() + "</b>" +
//							//"<br/>Saved to: <b>" + fileItem.toString() + "</b>" +
//							"<br/>Status: <b>" + message + "</b>"
//							);
//					out.println("</td>");
//				}
//				out.println("</tr>");
//			}
//			out.println("</table>");
//			out.println("<br/>");
//			//			out.println("<a href=\"/bluebox\">Click here to continue</a>");
//			out.println("<a href=\"app/upload.jsp\" >Upload more</a>");
//			out.println("<a href=\"/bluebox\" >Done</a>");
//		} 
//		catch (FileUploadException e) {
//			out.println("Error: " + e.getMessage());
//			e.printStackTrace();
//		}
//		//		printTail(out);
//		printJSP(request,response,"/upload_response_footer.jsp");
//
//	}

//	private void printJSP(HttpServletRequest request, HttpServletResponse response, String jsp) throws IOException {
//		RequestDispatcher dispatcher;
//		dispatcher= getServletContext().getRequestDispatcher(jsp);
//		try {
//			dispatcher.include(request, response);
//		} catch (ServletException e) {
//			e.printStackTrace();
//			log.severe(e.getMessage());
//		}
//	}

	//	private void printHead(PrintWriter out) {
	//		//		out.print("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
	//		//		out.print("<%@ page language=\"java\" pageEncoding=\"utf-8\" contentType=\"text/html;charset=utf-8\"%>");
	//		out.print("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
	//		out.print("<head>");
	//		out.print("<link rel=\"stylesheet\" type=\"text/css\" href=\""+Config.getInstance().getString("dojo_base")+"/dojo/resources/dojo.css\"/>");
	//		out.print("<link type=\"text/css\" href=\"theme/login.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/menu.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/common.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/inbox.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/mail.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/check.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/folder.css\" 			rel=\"stylesheet\" />");
	//		out.print("<link type=\"text/css\" href=\"theme/footer.css\" 			rel=\"stylesheet\" />");
	//		out.print("</head>");
	//		out.print("<body>");
	//		out.print("<div dojoType=\"dijit.layout.BorderContainer\"");
	//		out.print("style=\"width: 100%; height: 100%; background: transparent; padding: 0; border: none;\">");
	//		out.print("<div dojoType=\"dijit.layout.ContentPane\" region=\"top\" style=\"background: transparent; padding: 0; border: none;\">");
	//		out.print("<jsp:include page=\"jsp/menu_classic.jsp\" />");
	//		out.print("<div id=\"frame\">");
	//		out.print("<div class=\"umoyaTitleBar\">");
	//		out.print("<div class=\"umoyaRightCorner\">");
	//		out.print("<div class=\"umoyaInner\"> ");
	//		out.print("<h2 id=\"title\" class=\"directory\">Done</h2>");
	//		out.print("</div>");
	//		out.print("</div>");
	//		out.print("</div>");
	//		out.print("</div>");
	//		out.print("</div>");	
	//	}

	//	private void printTail(PrintWriter out) {
	//		out.print("</div>");
	//		out.print("<div dojoType=\"dijit.layout.ContentPane\" region=\"bottom\" style=\"background: transparent; padding: 0; border: none;\">");
	//		out.print("	<jsp:include page=\"jsp/footer_classic.jsp\" />");
	//		out.print("</div>");
	//		out.print("	</div>");
	//
	//		out.print("</body>");
	//		out.print("</html>");		
	//	}
}

