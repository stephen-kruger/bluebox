<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.chart.Charts" language="java"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle infoResource = ResourceBundle.getBundle("info",request.getLocale());
	ResourceBundle chartResource = ResourceBundle.getBundle("charts",request.getLocale());

%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=infoResource.getString("title")%></title>
	<jsp:include page="dojo.jsp" />
	
	<script type="text/javascript" charset="utf-8">
		require(["dojo/domReady!","dijit/form/Button",  "dojox/form/Uploader", "dojox/form/uploader/FileList"], function(domReady,Button,Uploader,FileList){
			selectMenu("info");
		});
	</script>
	
	<style type="text/css">
		
		.infoValue {
			font-weight: bold;
			text-align: right;
			white-space: normal;
			display:table-cell;
		}
		
		/* make centre smaller to allow bigge rgraph in rightCol*/
		.centerCol {
			width:50%;
		}
	
	</style>
	<script>
	function resetLists() {
        dojo.ready(function(){
			  // The parameters to pass to xhrGet, the url, how to handle it, and the callbacks.
			  var xhrArgs = {
			    url: "<%=request.getContextPath()%>/rest/resetlists",
			    handleAs: "text",
			    load: function(data){
			    	window.location.reload();
			    },
			    error: function(error){
			      console.log("An unexpected error occurred: " + error);
			    }
			  };

			  // Call the asynchronous xhrGet
			  dojo.xhrPost(xhrArgs);
			});
	}
	</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2><%=infoResource.getString("title")%></h2>
		</div>
			
		<div class="centerCol">
		<div style="text-align:left;">
			<table>
				<tr>
					<td><label><%=infoResource.getString("messageage")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_MESSAGE_AGE) %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("trashage")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_TRASH_AGE) %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("messagemax")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_MESSAGE_MAX) %>
					</td>
				</tr>
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("reset")%></label></td>
					<td align="right"><button onclick="resetLists()" data-dojo-type="dijit/form/Button" type="button"><%=infoResource.getString("resetButton")%></button></td>
				</tr>
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("toblacklist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(Inbox.getInstance().getToBlacklist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("fromblacklist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(Inbox.getInstance().getFromBlacklist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("towhitelist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(Inbox.getInstance().getToWhitelist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("fromwhitelist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(Inbox.getInstance().getFromWhitelist()) %>
					</td>
					<td></td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
			</table>
			</div>
		</div>
			
		<div class="rightCol">
			<h3><%= chartResource.getString("charts_daily_title") %></h3>
			<img width="100%" alt="chart" src="<%=request.getContextPath()%>/<%=Charts.CHART_ROOT%>?chart=daily&width=450&height=250"></img>
			<h3><%= chartResource.getString("charts_hourly_title") %></h3>
			<img width="100%" alt="chart" src="<%=request.getContextPath()%>/<%=Charts.CHART_ROOT%>?chart=hourly&width=450&height=250"></img>
			<h3><%= chartResource.getString("charts_weekly_title") %></h3>
			<img width="100%" alt="chart" src="<%=request.getContextPath()%>/<%=Charts.CHART_ROOT%>?chart=weekly&width=400&height=250"></img>
		</div>
	</div>
</body>
</html>