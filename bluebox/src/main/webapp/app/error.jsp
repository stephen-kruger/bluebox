<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>


<!DOCTYPE html>
<html lang="en-US">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>Error</title>
	<jsp:include page="dojo.jsp" />
</head>
<body>
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2>Error</h2>
		</div>
			
		<div class="centerCol">
			<h1>Something went wrong (Code:<%= request.getParameter("code") %>)</h1>
			<p>Please try again, or report the problem using the link below.</p>
			<table style="width:100%;">
				<tr>
					<td>
						<span><a href="index.jsp">Return to Application</a></span>
					</td>
					<td>
						<span><a href="https://code.google.com/p/bluebox/issues/list">Report this Problem</a></span>
					</td>
				</tr>
			</table>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>