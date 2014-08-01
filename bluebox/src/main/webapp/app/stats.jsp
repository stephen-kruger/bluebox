<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page language="java" import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.BlueBoxServlet"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle statsResource = ResourceBundle.getBundle("stats",request.getLocale());
	ResourceBundle footerResource = ResourceBundle.getBundle("footer",request.getLocale());
%>
<script>		
	function loadRecent() {
		try {
			require(["dojox/data/JsonRestStore"], function () {
				var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.RECENT_STAT %>";
				var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
				var queryResults = jStore.fetch({
					  onComplete : 
						  	function(queryResults, request) {
								document.getElementById("<%=JSONStatsHandler.RECENT_STAT %>").innerHTML = '<a href="inbox.jsp?Email='+queryResults.recent.<%=BlueboxMessage.INBOX%>+'">'+queryResults.recent.<%=BlueboxMessage.SUBJECT%>+'</a>';
							}
				});
			});
		}
		catch (err) {
			console.log("stats1:"+err);
		}
	}
	
	function loadCombined() {
		try {
			require(["dojox/data/JsonRestStore"], function () {
				var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.COMBINED_STAT %>";
				var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
				var queryResults = jStore.fetch({
					  onComplete : 
						  	function(queryResults, request) {
						  		if (queryResults.recent.<%=BlueboxMessage.SUBJECT%>) {
									document.getElementById("<%=JSONStatsHandler.RECENT_STAT %>").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.recent.<%=BlueboxMessage.INBOX%>+'">'+queryResults.recent.<%=BlueboxMessage.SUBJECT%>+'</a>';
						  		}
						  		else {
									document.getElementById("<%=JSONStatsHandler.RECENT_STAT %>").innerHTML="<%= statsResource.getString("no_update") %>";						  			
						  		}
						  		if (queryResults.active.<%=BlueboxMessage.INBOX%>) {
									document.getElementById("<%=JSONStatsHandler.ACTIVE_STAT %>").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.active.<%=BlueboxMessage.INBOX%>+'">'+queryResults.active.<%=BlueboxMessage.INBOX%>+'</a><span class="badge">'+queryResults.active.<%=BlueboxMessage.COUNT%>+'</span>';
						  		}
						  		else {
						  			document.getElementById("<%=JSONStatsHandler.ACTIVE_STAT %>").innerHTML = "<%= statsResource.getString("no_update") %>";
						  		}
								if (queryResults.sender.<%=BlueboxMessage.FROM%>) {
									document.getElementById("<%=JSONStatsHandler.SENDER_STAT %>").innerHTML = '<a href="inbox.jsp?<%=Inbox.EMAIL%>='+queryResults.sender.<%=BlueboxMessage.FROM%>+'">'+queryResults.sender.<%=BlueboxMessage.FROM%>+'</a><span class="badge">'+queryResults.sender.<%=BlueboxMessage.COUNT%>+'</span>';
								}
								else {
									document.getElementById("<%=JSONStatsHandler.SENDER_STAT %>").innerHTML = "<%= statsResource.getString("no_update") %>";
								}
								document.getElementById("statsGlobalCount").innerHTML = '<%= statsResource.getString("traffic_text1") %> <span id="statsGlobalCount">'+queryResults.<%=BlueboxMessage.COUNT%>+'</span> <%= statsResource.getString("traffic_text2") %>';
							}
				});
			});
		}
		catch (err) {
			console.log("stats3:"+err);
		}
	}	
	
	function loadStats() {
		loadCombined();
	}
	
	require(["dojo/domReady!"], function() {
		loadStats();
	});
</script>

<div class="rightSideContent">
	<h2><%= statsResource.getString("traffic_title") %></h2>
	<p>
		<span id="statsGlobalCount" style="white-space: nowrap;"><img
			src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span>
	</p>

	<h2><%= statsResource.getString("active_title") %></h2>
	<p>
		<span id="<%=JSONStatsHandler.ACTIVE_STAT %>" style="white-space: nowrap;"><img
			src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span>
	</p>
	
	<h2><%= statsResource.getString("sender_title") %></h2>
	<p>
		<span id="<%=JSONStatsHandler.SENDER_STAT %>" style="white-space: nowrap;"><img
			src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span>
	</p>

	<h2><%= statsResource.getString("recent_title") %></h2>
	<p>
		<span id="stats_recent" style="white-space: normal;"><img
			src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/loading.gif"></span>
	</p>
	<br />
	<div class="seperator"></div>
	<br /> <span style="color: lightGrey;"><%= footerResource.getString("title") %>
		V<%= BlueBoxServlet.VERSION %></span>
</div>