<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>

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
				<div style="display:table-row">
					<jsp:include page="check.jsp" />
				</div>
			</div>
		</div>
			
		<div class="centerCol">
			<jsp:include page="maillist.jsp" />
		</div>
			
		<div class="rightCol">
			<jsp:include page="search.jsp" />
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>