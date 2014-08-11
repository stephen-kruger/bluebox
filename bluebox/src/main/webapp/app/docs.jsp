<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title>Documentation</title>
	<jsp:include page="dojo.jsp" />	
	<script>
		require(["dojo/domReady!"], function(){
			selectMenu("docs");
		});
	</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h1>Documentation</h1>
		</div>
			
		<div class="centerCol">
			<div style="text-align:left;">
					<h2>Under construction</h2>
					<ol>
						<li style="list-style-type:none;">tbd</li>
					</ol>
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>