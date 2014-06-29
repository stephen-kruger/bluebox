<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%@ page import="com.bluebox.BlueBoxServlet"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle statsResource = ResourceBundle.getBundle("stats",request.getLocale());
	ResourceBundle footerResource = ResourceBundle.getBundle("footer",request.getLocale());
%>
<script>		
	function loadRecent() {
		require(["dojox/data/JsonRestStore"], function () {
			var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.RECENT_STAT %>";
			var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
			var queryResults = jStore.fetch({
				  onComplete : 
					  	function(queryResults, request) {
							document.getElementById("stats_recent").innerHTML = '<a href="inbox.jsp?Email='+queryResults.recent.<%=BlueboxMessage.TO%>+'">'+queryResults.recent.<%=BlueboxMessage.SUBJECT%>+'</a>';
						}
			});
		});
	}	
	
	function loadActive() {
		try {
			require(["dojox/data/JsonRestStore"], function () {
				var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.ACTIVE_STAT %>";
				var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
				var queryResults = jStore.fetch({
					  onComplete : 
						  	function(queryResults, request) {
								document.getElementById("stats_active").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.active.<%=BlueboxMessage.TO%>+'">'+queryResults.active.<%=BlueboxMessage.TO%>+'</a>';
							}
				});
			});
			
		}
		catch (err) {
			alert(err);
		}
	
	}
	
	function loadCombined() {
		require(["dojox/data/JsonRestStore"], function () {
			var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.COMBINED_STAT %>";
			var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
			var queryResults = jStore.fetch({
				  onComplete : 
					  	function(queryResults, request) {
							document.getElementById("stats_recent").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.recent.<%=BlueboxMessage.TO%>+'">'+queryResults.recent.<%=BlueboxMessage.SUBJECT%>+'</a>';
							document.getElementById("stats_active").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.active.<%=BlueboxMessage.TO%>+'">'+queryResults.active.<%=BlueboxMessage.TO%>+'</a>';
							document.getElementById("statsGlobalCount").innerHTML = '<%= statsResource.getString("traffic_text1") %> <span id="statsGlobalCount">'+queryResults.<%=BlueboxMessage.COUNT%>+'</span> <%= statsResource.getString("traffic_text2") %>';
						}
			});
		});
	}	
	
	function loadStats() {
		loadCombined();
	}
	
	require(["dojo/domReady!"], function() {
		loadStats();
	});
</script>
<style>
	p {
	        margin-top:0em;
	        margin-bottom:0em;
	} 
</style>
	<div class="rightSideContent">	    	    
		<h2><%= statsResource.getString("traffic_title") %></h2>
		<p><span id="statsGlobalCount" style="white-space: nowrap;"><img src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span></p>

		<h2><%= statsResource.getString("active_title") %></h2>
		<p><span id="stats_active" style="white-space: nowrap;"><img src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span></p>
         
		<h2><%= statsResource.getString("recent_title") %></h2>
		<p><span id="stats_recent" style="white-space: normal;"><img src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span></p>
		<div class="seperator"></div>
		<br/>
		<span style="color:lightGrey;"><%= footerResource.getString("title") %> V<%= BlueBoxServlet.VERSION %></span>
	</div>