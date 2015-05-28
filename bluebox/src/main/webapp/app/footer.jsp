<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%@ page import="com.bluebox.Config"%>
<%
	ResourceBundle footerResource = ResourceBundle.getBundle("footer",request.getLocale());
%>
<%@ page import="com.bluebox.BlueBoxServlet"%>
<div class="menu1">
	<%= footerResource.getString("title") %> V<%= BlueBoxServlet.VERSION %>
</div>