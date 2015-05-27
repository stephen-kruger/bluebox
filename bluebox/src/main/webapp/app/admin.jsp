<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.storage.StorageIf"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.json.JSONAdminHandler"%><%@ page
	import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle headerResource = ResourceBundle.getBundle("header",
			request.getLocale());
	ResourceBundle adminResource = ResourceBundle.getBundle("admin",
			request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
<title><%=headerResource.getString("welcome")%></title>
<jsp:include page="dojo.jsp" />
<script>		
		
		require(["dojo/parser", "dijit/ProgressBar", "dijit/form/Button", "dijit/form/NumberTextBox","dijit/form/HorizontalSlider","dijit/form/HorizontalRule","dijit/form/HorizontalRuleLabels"]);
		
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
					var urlStr = "<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/workerstats";
					var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
					jStore.fetch({
						  onComplete : 
							  	function(queryResults, request) {
								  try {
									  if (queryResults.backup) {
											backup.set({value: queryResults.backup});
											document.getElementById("backupLabel").innerHTML = queryResults.backup_status;
									  }
									  if (queryResults.restore) {
											restore.set({value: queryResults.restore});
											document.getElementById("restoreLabel").innerHTML = queryResults.restore_status;
									  }
									  if (queryResults.rawclean) {
											rawclean.set({value: queryResults.rawclean});
											document.getElementById("rawcleanLabel").innerHTML = queryResults.rawclean_status;
									  }
									  if (queryResults.reindex) {
											reindex.set({value: queryResults.reindex});
											document.getElementById("reindexLabel").innerHTML = queryResults.reindex_status;
									  }
									  if (queryResults.dbmaintenance) {
										  dbmaintenance.set({value: queryResults.dbmaintenance});
										  document.getElementById("dbmaintenanceLabel").innerHTML = queryResults.dbmaintenance_status;
									  }
									  if (queryResults.cleanup) {
										  cleanup.set({value: queryResults.cleanup});
										  document.getElementById("cleanupLabel").innerHTML = queryResults.cleanup_status;
									  }
									  if (queryResults.generate) {
										  generate.set({value: queryResults.generate});
										  document.getElementById("generateLabel").innerHTML = queryResults.generate_status;
									  }
								  }
								  catch (err) {
									  console.log("page not ready :"+err);
								  }
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
			console.log(dijit.byId("mailCountSlider").value);
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/generate?count="+dijit.byId("mailCountSlider").value,
					"Scheduled generation of "+dijit.byId("mailCountSlider").value+" emails");
		}

		function setBaseCount() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/setbasecount?count="+dijit.byId("setbasecount").value,
					"<%=adminResource.getString("set_global_action")%>");
		}
		
		function setSMTPBlacklist() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/setsmtpblacklist?value="+dijit.byId("setsmtpblacklist").value,
					"<%=adminResource.getString("set_smtpblacklist_action")%>");
		}
		
		function deleteAllMail() {
			genericConfirmGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/clear",
					"<%=adminResource.getString("delete_all_action")%>");
		}
		
		function purgeDeletedMail() {
			genericConfirmGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/purge_deleted",
					"<%=adminResource.getString("purge_deleted_action")%>");
		}
		
		function clearErrorLogs() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/errors",
					"<%=adminResource.getString("clear_errors_action")%>");
		}
		
		function pruneMail() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/prune",
					"<%=adminResource.getString("prune_action")%>");
		}
		
		function rebuildSearchIndexes() {
			genericConfirmGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/rebuildsearchindexes",
					"<%=adminResource.getString("rebuild_search_action")%>",
					"Started");
		}
		
		function dbMaintenance() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/dbmaintenance",
					"<%=adminResource.getString("db_maintenance_action")%>");
		}
		
		function dbBackup() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/backup",
					"<%=adminResource.getString("backup_action")%>");
		}
		
		function dbRestore() {
			genericConfirmGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/restore",
					"<%=adminResource.getString("restore_action")%>",
					"Server responded");
		}
		
		function dbRawClean() {
			genericGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/rawclean",
					"<%=adminResource.getString("rawclean_action")%>",
					"Server responded");
		}
		
		function dbClean() {
			genericConfirmGet("<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/clean",
					"<%=adminResource.getString("clear_backup_action")%>");
		}
		
		function genericConfirmGet(url,title) {
			require(["dijit/ConfirmDialog", "dojo/domReady!"], function(ConfirmDialog){
			    myDialog = new ConfirmDialog({
			        title: "<%=adminResource.getString("confirm_title")%>",
				content : title,
				style : "width: 300px",
				onExecute : function() { //Callback function 
					genericGet(url, title);
				},
				onCancel : function() {
					console.log("Event Cancelled");
				}
			});
			myDialog.show();
		});
	}

	function genericGet(url, title) {
		dojo.ready(function() {
			// The parameters to pass to xhrGet, the url, how to handle it, and the callbacks.
			var xhrArgs = {
				url : url,
				handleAs : "text",
				load : function(data) {
					// TODO - implement status message overlay animation
					showMessage(title + ":" + data);
				},
				error : function(error) {
					console.log("An unexpected error occurred: " + error);
					//dialog(title,error);
					showMessage(title + ":" + error);
				}
			};

			// Call the asynchronous xhrGet
			dojo.xhrGet(xhrArgs);
		});
	}

	require([ "dojo/domReady!" ], function() {
		selectMenu("admin");
		updateWorkers();
	});
</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">
		<div class="leftCol">
			<h2><%=adminResource.getString("title")%></h2>
		</div>

		<div class="centerCol">
			<div style="text-align: left;">
				<table>
					<tr>
						<td><label><%=adminResource.getString("generate_action")%></label></td>
						<td>
							<div id="mailCountSlider" style="width: 100%;"
								name="horizontalSlider"
								data-dojo-type="dijit/form/HorizontalSlider"
								data-dojo-props="value:100,
						    minimum: 0,
						    maximum:5000,
						    discreteValues:501,
						    value:100,
						    intermediateChanges:false,
						    showButtons:false">
								<div data-dojo-type="dijit/form/HorizontalRule"
									container="bottomDecoration" count=5 style="height: 0.75em;"></div>
								<ol data-dojo-type="dijit/form/HorizontalRuleLabels"
									container="bottomDecoration"
									style="height: 1em; font-size: 75%; color: gray;">
									<li>0</li>
									<li>1250</li>
									<li>2500</li>
									<li>3800</li>
									<li>5000</li>
								</ol>
							</div>
						</td>
						<td><button onclick="generateEmails();"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="generate"
								id="generateProgress" data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="generatelabel"
							id="generateLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("delete_all_action")%></label></td>
						<td></td>
						<td><button onclick="deleteAllMail()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("purge_deleted_action")%></label></td>
						<td></td>
						<td><button onclick="purgeDeletedMail()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("clear_errors_action")%></label></td>
						<td></td>
						<td><button onclick="clearErrorLogs()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("prune_action")%></label></td>
						<td></td>
						<td><button onclick="pruneMail()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="cleanup" id="cleanupProgress"
								data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="cleanuplabel"
							id="cleanupLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("set_global_action")%></label></td>
						<td>
							<form method="get"
								action="<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/setbasecount">
								<input id="setbasecount" type="text"
									data-dojo-type="dijit/form/NumberTextBox" name="setbasecount"
									value="25000000"
									data-dojo-props="constraints:{pattern: '#',min:0,max:99999999,places:0},  invalidMessage:'Please enter a value between 10 and 5000'" />
							</form>
						</td>
						<td><button onclick="setBaseCount();"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("rebuild_search_action")%></label></td>
						<td></td>
						<td><button onclick="rebuildSearchIndexes();"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="reindex" id="reindexProgress"
								data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="reindexlabel"
							id="reindexLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("db_maintenance_action")%></label></td>
						<td></td>
						<td><button onclick="dbMaintenance()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="dbmaintenance"
								id="dbmaintenanceProgress" data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="dbmaintenancelabel"
							id="dbmaintenanceLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("backup_action")%></label></td>
						<td></td>
						<td><button onclick="dbBackup()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="backup" id="backupProgress"
								data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="backuplabel"
							id="backupLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("restore_action")%></label></td>
						<td></td>
						<td><button onclick="dbRestore()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="restore" id="restoreProgress"
								data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="restorelabel"
							id="restoreLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("rawclean_action")%></label></td>
						<td></td>
						<td><button onclick="dbRawClean()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
						<td><div data-dojo-type="dijit/ProgressBar"
								style="width: 100%" data-dojo-id="rawclean"
								id="rawcleanProgress" data-dojo-props="maximum:100"></div></td>
						<td></td>
						<td align="right"><label data-dojo-id="rawcleanlabel"
							id="rawcleanLabel"></label></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("clear_backup_action")%></label></td>
						<td></td>
						<td><button onclick="dbClean()"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
					</tr>
					<tr>
						<td><br /></td>
					</tr>
					<tr>
						<td><label><%=adminResource.getString("set_smtpblacklist_action")%></label></td>
						<td>
							<form method="get"
								action="<%=request.getContextPath()%>/<%=JSONAdminHandler.JSON_ROOT%>/setsmtpblacklist">
								<input id="setsmtpblacklist" type="text"
									data-dojo-type="dijit/form/TextBox" name="setsmtpblacklist"
									value="<%= Utils.toCSVString(new Inbox().getSMTPBlacklist()) %>" />
							</form>
						</td>
						<td><button onclick="setSMTPBlacklist();"
								data-dojo-type="dijit/form/Button" type="button"><%=adminResource.getString("execute")%></button></td>
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