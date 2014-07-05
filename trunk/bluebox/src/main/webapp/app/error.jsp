<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle uploadResource = ResourceBundle.getBundle("upload",request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%= uploadResource.getString("title") %></title>
	<jsp:include page="dojo.jsp" />
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2><%= uploadResource.getString("title") %></h2>
		</div>
			
		<div class="centerCol">
			<h1>We are unable to process your request</h1>
			<p>Click the browser back button to return to the previous page and try again.  If this error persists, report the problem to your administrator.</p>
			<form method="post" action="index.jsp">
				<div><input type="submit" value="Return to Application" /></div>
			</form>
			<span><a href="https://code.google.com/p/bluebox/issues/list">Report this Problem</a></span>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>