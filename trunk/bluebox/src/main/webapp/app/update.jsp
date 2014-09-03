<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox" language="java"%>
<%@ page import="com.bluebox.Config" language="java"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler" language="java"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage" language="java"%>
<%@ page import="java.util.ResourceBundle" language="java"%>
<%@ page import="com.bluebox.BlueBoxServlet" language="java"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle updateResource = ResourceBundle.getBundle("update",request.getLocale());
%>
<span id="updateAvailable" class="blinkBadge"><%= updateResource.getString("update_available") %></span>