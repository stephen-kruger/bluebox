<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.storage.StorageIf"%>
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
		
		require(["dojo/parser", "dijit/ProgressBar", "dijit/form/Button", "dijit/form/NumberTextBox"]);
		
		// start the refresh timer
		require(["dojox/timing"], function(registry){
			var t = new dojox.timing.Timer(5000);
			t.onTick = function() {
				updateWorkers();
			};
			t.start();
		});
		
		function updateWorkers() {
			try {
				require(["dojox/data/JsonRestStore"], function () {
					var urlStr = "<%=request.getContextPath()%>/rest/admin/workerstats";
					var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
					var queryResults = jStore.fetch({
						  onComplete : 
							  	function(queryResults, request) {
								  if (queryResults.backup)
										backup.set({value: queryResults.backup});
								  if (queryResults.restore)
										restore.set({value: queryResults.restore});
								  if (queryResults.reindex)
										reindex.set({value: queryResults.reindex});
								  if (queryResults.<%=StorageIf.WT_NAME %>)
									  <%=StorageIf.WT_NAME %>.set({value: queryResults.<%=StorageIf.WT_NAME %>});
								  if (queryResults.cleanup)
									  cleanup.set({value: queryResults.cleanup});
								  if (queryResults.generate)
									  generate.set({value: queryResults.generate});
								},
							onError :
								function(error) {
									console.log(error);
								}
					});
				});
			}
			catch (err) {
				alert(err);
			}
		}
		
		function generateEmails() {
			genericGet("<%=request.getContextPath()%>/rest/admin/generate?count="+document.getElementById('count').value,"Email generation","Scheduled generation of "+document.getElementById('count').value+" emails");
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
		
		function dbBackup() {
			genericGet("<%=request.getContextPath()%>/rest/admin/backup","Backup requested","Server responded");
		}
		
		function dbRestore() {
			genericGet("<%=request.getContextPath()%>/rest/admin/restore","Restore requested","Server responded");
		}
		
		function dbClean() {
			genericGet("<%=request.getContextPath()%>/rest/admin/clean","Clean backups requested","Server responded");
		}
		
		function dialog(title, content) {
			require(["dijit/Dialog", "dojo/domReady!"], function(Dialog){
	    	    myDialog = new Dialog({
	    	        title: title,
	    	        content: content,
	    	        style: "width: 450px"
	    	    });
	    	    
	    	    var div = dojo.create('div', {}, myDialog.containerNode);
                dojo.style(dojo.byId(div), "padding", "2em");
                dojo.style(dojo.byId(div), "float", "middle");
	    	    var closeBtn = new dijit.form.Button({
                    label: "Close",
                    onClick: function(){
                    	myDialog.hide();
                        dojo.destroy(myDialog);
                    }
                 });
	    	    dojo.create(closeBtn.domNode,{}, div);
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
				    	dialog(title,content+"<br/>"+data);
				    },
				    error: function(error){
				      console.log("An unexpected error occurred: " + error);
				    }
				  }

				  // Call the asynchronous xhrGet
				  var deferred = dojo.xhrGet(xhrArgs);
				});
		}
		
		require(["dojo/domReady!"], function(){
			selectMenu("admin");
			updateWorkers();
		});
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
					<form id="generate" method="get" action="<%=request.getContextPath()%>/rest/admin/generate">
						<input id="count" type="text" data-dojo-type="dijit/form/NumberTextBox" name= "count" value="10" data-dojo-props="constraints:{min:10,max:5000,places:0,pattern:'#'},  invalidMessage:'Please enter a value between 10 and 5000'" />
					</form>
					</td>
					<td><button onclick="generateEmails();" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="generate" id="generateProgress" data-dojo-props="maximum:100"></div></td>
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
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="cleanup" id="cleanupProgress" data-dojo-props="maximum:100"></div></td>
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
					<form method="get" action="<%=request.getContextPath()%>/rest/admin/setbasecount">
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
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="reindex" id="reindexProgress" data-dojo-props="maximum:100"></div></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Perform DB maintenance</label></td>
					<td></td>
					<td><button onclick="dbMaintenance()" data-dojo-type="dijit/form/Button" type="button">Go</button></td>
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="<%=StorageIf.WT_NAME %>" id="<%=StorageIf.WT_NAME %>Progress" data-dojo-props="maximum:100"></div></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Backup mail db</label></td>
					<td></td>
					<td><button onclick="dbBackup()" data-dojo-type="dijit/form/Button" type="button">Backup</button></td>
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="backup" id="backupProgress" data-dojo-props="maximum:100"></div></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Restore mail db</label></td>
					<td></td>
					<td><button onclick="dbRestore()" data-dojo-type="dijit/form/Button" type="button">Restore</button></td>
					<td><div data-dojo-type="dijit/ProgressBar" style="width:100%" data-dojo-id="restore" id="restoreProgress" data-dojo-props="maximum:100"></div></td>
				</tr>
				<tr>
				<td><br/></td>
				</tr>
				<tr>
					<td><label>Clean mail backup</label></td>
					<td></td>
					<td><button onclick="dbClean()" data-dojo-type="dijit/form/Button" type="button">Clean</button></td>
				</tr>
				<tr>
				<td><br/></td>
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