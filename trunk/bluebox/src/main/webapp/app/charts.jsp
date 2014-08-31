<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox" language="java"%>
<%@ page import="com.bluebox.Config" language="java"%>
<%@ page import="com.bluebox.chart.Charts" language="java"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage" language="java"%>
<%@ page import="java.util.ResourceBundle" language="java"%>
<%@ page import="com.bluebox.BlueBoxServlet" language="java"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle chartResource = ResourceBundle.getBundle("charts",request.getLocale());
%>

<div class="leftSideContent">
	<h2><%= chartResource.getString("charts_daily_title") %></h2>
	<a href="<%=request.getContextPath()%>/app/info.jsp">
		<img width="100%" alt="chart" src="<%=request.getContextPath()%>/<%=Charts.CHART_ROOT%>?chart=daily&width=300&height=150"></img>
	</a>
</div>