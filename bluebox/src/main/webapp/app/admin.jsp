<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
	<script>
		require(["dojo/domReady!"], function(){
			selectMenu("admin");
		});
		
		require(["dojo/parser", "dijit/form/Button", "dijit/form/NumberTextBox"]);
		
		function generateEmails() {
			genericGet("<%=request.getContextPath()%>/rest/admin/test?count="+document.getElementById('count').value,"Email generation","Generated "+document.getElementById('count').value+" emails");
		}

		function setBaseCount() {
			genericGet("<%=request.getContextPath()%>/rest/admin/setbasecount?count="+document.getElementById('setbasecount').value,"Base count","Set to "+document.getElementById('setbasecount').value);
		}
		
		function deleteAllMail() {
			genericGet("<%=request.getContextPath()%>/rest/admin/clear","Mail deletion","All deleted");
		}
		
		function clearErrorLogs() {
			genericGet("<%=request.getContextPath()%>/rest/admin/errors","Error logs","Cleared");
		}
		
		function pruneMail() {
			genericGet("<%=request.getContextPath()%>/rest/admin/prune","Mail cleanup","Started");
		}
		
		function rebuildSearchIndexes() {
			genericGet("<%=request.getContextPath()%>/rest/admin/rebuildsearchindexes","Search index rebuild","Started");
		}
		
		function dbMaintenance() {
			genericGet("<%=request.getContextPath()%>/rest/admin/dbmaintenance","Maintenance requested","OK");

		}
		
		function dialog(title, content) {
			require(["dijit/Dialog", "dojo/domReady!"], function(Dialog){
	    	    myDialog = new Dialog({
	    	        title: title,
	    	        content: content,
	    	        style: "width: 300px"
	    	    });
	    	    myDialog.show();
	    	});
		}
		
		function genericGet(url,title,content) {
			dojo.ready(function(){
				  // The parameters to pass to xhrGet, the url, how to handle it, and the callbacks.
				  var xhrArgs = {
				    url: url,
				    handleAs: "text",
				    load: function(data){
				    	dialog(title,content);
				    },
				    error: function(error){
				      alert("An unexpected error occurred: " + error);
				    }
				  }

				  // Call the asynchronous xhrGet
				  var deferred = dojo.xhrGet(xhrArgs);
				});
		}
		
	</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2>Administration</h2>
		</div>
			
		<div class="centerCol">
		<div style="text-align:left;">
			<table>
				<tr>
					<td><label>Generate fake emails</label></td>
					<td>
					<form id="generate" method="get" action="rest/admin/test">
						<input id="count" type="text" data-dojo-type="dijit/form/NumberTextBox" name= "count" value="10" data-dojo-props="constraints:{min:10,max:5000,places:0},  invalidMessage:'Please enter a value between 10 and 5000'" />
					</form>
					</td>
					<td><button onclick="generateEmails();" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Delete all emails</label></td>
					<td></td>
					<td><button onclick="deleteAllMail()" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Clear error logs</label></td>
					<td></td>
					<td><button onclick="clearErrorLogs()" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>						
				<tr>
					<td><label>Prune expired emails and empty inboxes</label></td>
					<td></td>
					<td><button onclick="pruneMail()" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>	
				<tr>
				<td><br/></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>								
				<tr>
					<td><label>Set global received mail counter</label></td>
					<td>
					<form method="get" action="rest/admin/setbasecount">
					<input id="setbasecount" type="text" data-dojo-type="dijit/form/NumberTextBox" name="setbasecount" value="25000000" data-dojo-props="constraints:{pattern: '#',min:0,max:99999999,places:0},  invalidMessage:'Please enter a value between 10 and 5000'" />
					</form>
					</td>
					<td><button onclick="setBaseCount();" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Rebuild search indexes</label></td>
					<td></td>
					<td><button onclick="rebuildSearchIndexes();" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Perform DB maintenance</label></td>
					<td></td>
					<td><button onclick="dbMaintenance()" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>To blacklist</label></td>
					<td>
						<%= bbconfig.getFlatList(Config.BLUEBOX_TOBLACKLIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>From blacklist</label></td>
					<td>
						<%= bbconfig.getFlatList(Config.BLUEBOX_FROMBLACKLIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>To whitelist</label></td>
					<td>
						<%= bbconfig.getFlatList(Config.BLUEBOX_TOWHITELIST) %>
					</td>
					<td></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>From whitelist</label></td>
					<td>
						<%= bbconfig.getFlatList(Config.BLUEBOX_FROMWHITELIST) %>
					</td>
					<td></td>
				</tr>
				
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label>How many hours to keep messages</label></td>
					<td>
						<%= bbconfig.getString(Config.BLUEBOX_MESSAGE_AGE) %>
					</td>
				</tr>
				<tr>
					<td><br/></td>
				</tr>
				<tr>
					<td><label>How many hours to keep deleted messages</label></td>
					<td>
						<%= bbconfig.getString(Config.BLUEBOX_TRASH_AGE) %>
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