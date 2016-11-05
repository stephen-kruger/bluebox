<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle" language="java" %>
<%@ page import="com.bluebox.Config" language="java" %>
<%@ page import="com.bluebox.rest.StatsResource" language="java"%>
<% request.setCharacterEncoding("utf-8"); %>

<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
	<script type="text/javascript" charset="utf-8">
		require([
		         "dojo/_base/kernel",
		         "dojo/parser",
		         "dojox/dgauges/components/default/CircularLinearGauge"]
		      );
		
		function updateMph(emailAddress) {
			try {
				require(["dojox/data/JsonRestStore"], function () {
					var urlStr = "<%=request.getContextPath()%>/jaxrs/stats/mph/"+encodeURIComponent(emailAddress);
					var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
					var queryResults = jStore.fetch({
						  onComplete : 
							  	function(queryResults, request) {
									if (document.getElementById("mphGauge")) {
										require(["dijit/registry"], function(registry){
										    var mphGauge = registry.byId("mphGauge");
										    if (mphGauge) {
										    	//if (mphGauge.get('maximum')<queryResults.mph.mph) {
												//	console.log("Updating gauge maximum"+queryResults.mph.mph);
										    	//	mphGauge.set('maximum', queryResults.mph.mph*2);
										    	//}
										    	//else {
												//	console.log("Not updating gauge maximum="+mphGauge.get('maximum')+" value="+queryResults.mph.mph);
										    	//}
										    	mphGauge.set('value', queryResults.mph);
										    	mphGauge.refreshRendering();
										    }
										    else {
										    	console.log("no dijit gauge found");
										    }
									    });
									}
									else {
										console.log("no gauge found");
									}
								}
					});
				});
			}
			catch (err) {
				console.log("stats3:"+err);
			}
		}
		
		function startGaugeTimer() {
			try {
				// start the refresh timer
				require(["dojox/timing"], function(registry){
					var t = new dojox.timing.Timer(45000);
					t.onTick = function() {
						updateMph(folderEmail);
					}
					t.start();
				});
			}
			catch (err) {
				console.log("startGaugeTimer error:"+err);
			}
		}
		
		require(["dojo/ready", "dijit/registry", "dojo/parser"],
				function(ready, registry){
				  ready(function(){
						selectMenu("inbox");
						// will not be called until DOM is ready
						startGaugeTimer();
				  });
		});
		
	</script>
	<style type="text/css">
		
		.detailPane {
			width:100%;
			height:70%;
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
				<div class="seperator"></div>
				<h2>Mails per hour</h2>
				<div style="display: block;margin-left: auto;margin-right: auto ">
					<div id="mphGauge" data-dojo-type="dojox/dgauges/components/default/CircularLinearGauge" value="0" minimum="0" maximum="5000" style="width:100px; height:100px"></div>
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