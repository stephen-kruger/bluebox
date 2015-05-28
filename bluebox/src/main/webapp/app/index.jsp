<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
</head>
<body class="<%=Config.getInstance().getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<jsp:include page="check.jsp" />
			<div class="seperator"></div>
			<jsp:include page="charts.jsp" />
		</div>
			
		<div class="centerCol" style="display: table-cell;vertical-align: middle;">
			<h1><%=headerResource.getString("welcome")%></h1>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
		

	</div>
</body>
</html>