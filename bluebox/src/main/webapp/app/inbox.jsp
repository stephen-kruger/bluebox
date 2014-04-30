<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONRawMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONInboxHandler"%>

<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
	<script>
		require(["dojo/domReady!"], function(domReady){
			selectMenu("inbox");
		});
	</script>
	<style type="text/css">
		
		.detailPane {
			width:100%;
			height:70%;
			display:table-row;
		}
			
		.seperator {
			border-top:1px dotted #ccc;
		}
	
	</style>
</head>
<body class="<%=Config.getInstance().getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<div style="display:table;width:100%;">
				<div style="display:table-row">
					<jsp:include page="folders.jsp" />
				</div>
				<div style="display:table-row"></div>
				<div class="seperator"></div>
				<br/>
				<div style="display:table-row">
					<jsp:include page="check.jsp" />
				</div>
			</div>
		</div>
			
		<div class="centerCol">
			<jsp:include page="maillist.jsp" />
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>