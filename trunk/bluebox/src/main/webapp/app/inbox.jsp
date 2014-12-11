<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>

<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
	<script type="text/javascript" charset="utf-8">
		require(["dojo/domReady!"], function(domReady){
			selectMenu("inbox");
		});
		require([
		         "dojo/_base/kernel",
		         "dojo/parser",
		         "dojox/dgauges/components/default/CircularLinearGauge"]
		      );
		
		function startGaugeTimer() {
			try {
				console.log("into gauge timer");
				// start the refresh timer
				require(["dojox/timing"], function(registry){
					var t = new dojox.timing.Timer(10000);
					t.onTick = function() {
						console.log("tick gauge timer");
						loadCombined();
					}
					t.start();
				});
			}
			catch (err) {
				console.log("menu3:"+err);
			}
		}
		
		require(["dojo/domReady!"], function() {
			console.log("starting gauge timer");
			// will not be called until DOM is ready
			startGaugeTimer();
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
					<div id="mphGauge" data-dojo-type="dojox/dgauges/components/default/CircularLinearGauge" value="0" minimum="0" maximum="1000" style="width:150px; height:150px"></div>
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