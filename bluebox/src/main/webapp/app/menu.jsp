<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%
	Config bbconfig = Config.getInstance();
%>
<%
	ResourceBundle menuResource = ResourceBundle.getBundle("menu",request.getLocale());
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
%>

<style type="text/css">
	.menu {
		height:100%;
		width:100%;
		padding-left:1em;
	    vertical-align: middle;
	    display: table;
	}
	
	.menu1 {
		width:33%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menu2 {
		width:33%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menu3 {
		width:33%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menulink, .helpIcon, .menuSelected {
		text-decoration: none;
		color:white;
		font-size: 1.1em;
		padding: 0.75em;
	}
	
	.menuselected {
		-moz-box-shadow:    inset 0 0 10px #cfcfcf;
	   -webkit-box-shadow: inset 0 0 10px #cfcfcf;
	   	box-shadow:         inset 0 0 10px #cfcfcf;
	   	border-radius: 1em;
		color:white;
		padding: 0.5em;
	}
	
	.logoBox {
	    display: table;
	    vertical-align:middle;
	    padding:0.5em;
	}
	
	.logo {
		width:1.7em;
		height:1.7em;
		border:1px solid white;
		background: darkBlue; /* fallback for silly old IE */
		background: -webkit-linear-gradient(left top, lightBlue , darkBlue); /* For Safari 5.1 to 6.0 */
		background: -o-linear-gradient(bottom right, lightBlue, darkBlue); /* For Opera 11.1 to 12.0 */
		background: -moz-linear-gradient(bottom right, lightBlue, darkBlue); /* For Firefox 3.6 to 15 */
		background: linear-gradient(to bottom right, lightBlue , darkBlue); /* Standard syntax */
		display: table-cell;
	}
	
	.logoText {
		color:white;
		padding-left:5px;
		font-size:16pt;
		display: table-cell;
	}
	
	.helpIcon {
	  height: 1em;
	  width: 1.5em;
	  display: inline-table;
	  text-align: center;
	  border-radius: 50%; /* may require vendor prefixes */
	  background: white;
	  color: black;
	  text-decoration: none;
	  padding:0;
	}
	
	.badge {
		background: #107bbb;
		position: relative;
		bottom: 0.7em;
		margin-left: 2px;
		padding: 0 0.4em;
		color: #fff;
		display: inline;
		font-size: 0.9em;
		line-height: 1.2/*fixes link height with tall languages*/;
		-moz-border-radius: 3px;
		-webkit-border-radius: 3px;
		border-radius: 3px;
	}
	
	.badgeDown {
		background: #107bbb;
		position: relative;
		padding: 0 0.4em;
		color: #fff;
		display: inline;
		font-size: 0.9em;
		line-height: 1.2/*fixes link height with tall languages*/;
		-moz-border-radius: 3px;
		-webkit-border-radius: 3px;
		border-radius: 3px;
	}

</style>

<script type="text/javascript">		
			function selectMenu(id) {
				try {
					document.getElementById(id).className = "menuselected";
				}
				catch (err) {
					alert("menu1:"+err);
				}
			}
			
			// load the global stats
			function loadGlobalMenu() {
				try {
					require(["dojox/data/JsonRestStore"], function () {
						var urlStr = "<%=request.getContextPath()%>/<%=JSONStatsHandler.JSON_ROOT %>/<%=JSONStatsHandler.GLOBAL_STAT %>";
						var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:false});
						jStore.fetch({
							  onComplete : 
								  	function(queryResults, request) {
										document.getElementById("statsMenuGlobalCount").innerHTML = queryResults.countAll;
										document.getElementById("statsMenuErrorCount").innerHTML = queryResults.countError;
									}
						});
					});
				}
				catch (err) {
					alert("menu2:"+err);
				}
			}
			
			function startTimer() {
				try {
					// start the refresh timer
					require(["dojox/timing"], function(registry){
						var t = new dojox.timing.Timer(30000);
						t.onTick = function() {
							loadGlobalMenu();
						}
						t.start();
					});
				}
				catch (err) {
					alert("menu3:"+err);
				}
			}
			
			require(["dojo/domReady!"], function() {
				// will not be called until DOM is ready
				loadGlobalMenu();
				startTimer();
			});
					
	// Load the Tooltip widget class
	require(["dijit/Tooltip",  "dojo/parser", "dojo/domReady!"], function(Tooltip, parser, domReady){
		parser.parse();
	});
</script>
	    	    
<!-- draw the bluebox logo and title -->
<div class="menu">
	<div class="menu1">
		<div class="logoBox">
			<a class="menulink" href="<%=request.getContextPath()%>/app/index.jsp">
				<span class="logo"></span>
				<span class="logoText"><%=menuResource.getString("title")%></span>
			</a>
		</div>
	</div>
	<div class="menu2">
		<a id="inbox" class="menulink" href="<%=request.getContextPath()%>/app/inbox.jsp"><%= menuResource.getString("home") %><span id="statsMenuGlobalCount" class="badge">?</span></a>
		<a id="errors" class="menulink" href="<%=request.getContextPath()%>/app/errors.jsp"><%= menuResource.getString("errors") %><span id="statsMenuErrorCount" class="badge">?</span></a>
		<a id="upload" class="menulink" href="<%=request.getContextPath()%>/app/upload.jsp"><%= menuResource.getString("upload") %></a>
		<a id="info" class="menulink" href="<%=request.getContextPath()%>/app/info.jsp"><%= menuResource.getString("info") %></a>
	</div>
	<div class="menu3">
		<a id="admin" class="menulink" href="<%=request.getContextPath()%>/app/admin.jsp"><%= menuResource.getString("admin") %></a>
		<a id="help" class="helpIcon" href="<%=request.getContextPath()%>/app/help.jsp" onmouseover="dijit.Tooltip.defaultPosition=['below']">?</a>
		<div class="dijitHidden"><span data-dojo-type="dijit.Tooltip" data-dojo-props="connectId:'helpMenu'"><%= menuResource.getString("help") %></span></div>
	</div>
</div>