<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
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
			text-align: left;
			white-space: normal;
			display:table-cell;
		}
		
		.infoLabel {
			text-align: right;
			white-space: normal;
			display:table-cell;
			padding-right:2em;
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
					<td class="infoLabel"><label><%=infoResource.getString("messageage")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_MESSAGE_AGE) %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("trashage")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_TRASH_AGE) %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("messagemax")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getString(Config.BLUEBOX_MESSAGE_MAX) %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("messagemaxsize")%></label></td>
					<td class="infoValue">
						<%= bbconfig.getLong(Config.BLUEBOX_MAIL_LIMIT)/1000000 %>
					</td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("toblacklist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(new Inbox().getToBlacklist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("fromblacklist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(new Inbox().getFromBlacklist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("smtpblacklist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(new Inbox().getSMTPBlacklist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("towhitelist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(new Inbox().getToWhitelist()) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td class="infoLabel"><label><%=infoResource.getString("fromwhitelist")%></label></td>
					<td class="infoValue">
						<%= Utils.toString(new Inbox().getFromWhitelist()) %>
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
			<jsp:include page="charts.jsp" />
		</div>
	</div>
</body>
</html>