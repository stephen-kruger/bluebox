<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox" language="java"%>
<%@ page import="com.bluebox.Config" language="java"%>
<%@ page import="com.bluebox.chart.Charts" language="java"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage" language="java"%>
<%@ page import="java.util.ResourceBundle" language="java"%>
<%@ page import="com.bluebox.BlueBoxServlet" language="java"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle statsResource = ResourceBundle.getBundle("charts",request.getLocale());
%>
<script>	

</script>

<div class="leftSideContent">
	<h2><%= statsResource.getString("charts_title") %></h2>
	<img width="100%" alt="chart" src="<%=request.getContextPath()%>/<%=Charts.CHART_ROOT%>?width=350&height=160"></img>
</div>