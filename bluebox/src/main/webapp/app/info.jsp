<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle infoResource = ResourceBundle.getBundle("info",request.getLocale());
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
		}
	
	</style>
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
					<td><label><%=infoResource.getString("toblacklist")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getFlatList(Config.BLUEBOX_TOBLACKLIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("fromblacklist")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getFlatList(Config.BLUEBOX_FROMBLACKLIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("towhitelist")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getFlatList(Config.BLUEBOX_TOWHITELIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label><%=infoResource.getString("fromwhitelist")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getFlatList(Config.BLUEBOX_FROMWHITELIST) %>
					</td>
					<td></td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
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
				
			</table>
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>