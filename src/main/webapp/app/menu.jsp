<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.rest.InboxResource"%>
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
	    padding-top:0.5em;
	    padding-bottom:0.5em;
		padding-left:1em;
	    vertical-align: middle;
	    display: table;
	}
	
	.menu1 {
		width:30%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menu2 {
		width:30%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menu3 {
		width:30%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menu4 {
		width:10%;
		display: table-cell;
	    vertical-align: middle;
	}
	
	.menulink, .helpIcon, .menuSelected {
		text-decoration: none;
		color:white;
		font-size: 1.1em;
		padding: 0.5em;
	}

	.menulink:hover { 
		-moz-box-shadow:    inset 0 0 10px #45BBED;
	   -webkit-box-shadow: inset 0 0 10px #45BBED;
	   	box-shadow:         inset 0 0 10px #45BBED;
	   	border-radius: 1em;
		color: white;
		padding: 0.5em;
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
	    vertical-align:middle;
	    width: 100%;
	}
	
	.logo {
		width:1.7em;
		height:1.7em;
		border:none;
		background: #107bbb; /* fallback for silly old IE */
		background: -webkit-linear-gradient(top, #107bbb, black); /* For Safari 5.1 to 6.0 */
		background: -o-linear-gradient(top, #107bbb, black); /* For Opera 11.1 to 12.0 */
		background: -moz-linear-gradient(top, #107bbb, black); /* For Firefox 3.6 to 15 */
		background: linear-gradient(top, #107bbb, black); /* Standard syntax */
		filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#107bbb', endColorstr='#000000');
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
.message {
		background: #107bbb;
		width: 100%;
		color: #fff;
		inset-top; 5px;
		inset-bottom; 5px;
		text-align:center;
		display:none;
}
.userpic {
  	width: 25px;
 	height: 25px;
  	border-radius: 50%;
  	background-repeat: no-repeat;
  	background-position: center center;
  	background-size: cover;
  	display: table-cell;
	vertical-align: middle;
}
</style>

<script type="text/javascript">		
		require(["dojo/parser", "dijit/Tooltip", "dijit/form/Button"]);
			function selectMenu(id) {
				try {
					document.getElementById(id).className = "menuselected";
				}
				catch (err) {
					console.log("menu1:"+err);
				}
			}
			
			// load the global stats
			function loadGlobalMenu() {
				try {
					require(["dojox/data/JsonRestStore"], function () {
						var urlStr = "<%=request.getContextPath()%>/jaxrs/stats/global";
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
					console.log("menu2:"+err);
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
					console.log("menu3:"+err);
				}
			}
			
				  
			function hideMessage() {
				console.log("hiding div");
				document.getElementById("messageDiv").style.visibility = "hidden";				
				document.getElementById("messageDiv").style.display = "none";
				document.getElementById("messageDiv").innerHTML="";
			}
			
			function showMessage(msg) {
				showDelayedMessage(msg,6000);
			}
			
			function showDelayedMessage(msg,delay) {
				document.getElementById("messageDiv").innerHTML=msg;
				document.getElementById("messageDiv").style.visibility = "visible";
				document.getElementById("messageDiv").style.display = "block";
				setTimeout("hideMessage()", delay); // after 1 sec
			}
			
			function updateCheck() {
				var xhrArgs = {
						url: "<%=request.getContextPath()%>/jaxrs<%=InboxResource.PATH %>/updateavailable",
						handleAs: "json",
						preventCache: false,
						load: function(data) {
							if(data.update_available) {
								document.getElementById("update").style.display="";
						    	dialog(title,content+"<br/>"+data);
							}
							else {
								document.getElementById("update").style.display="none";
							}
						},
						error: function (error) {
							console.log("Something veeery bad happened :"+error);
						}
				};
			
				dojo.xhrGet(xhrArgs);
			}
			
			require(["dojo/ready", "dijit/registry", "dojo/parser"],
					function(ready, registry){
					  ready(function(){
							// will not be called until DOM is ready
							loadGlobalMenu();
							startTimer();
							updateCheck();
					  });
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
		<!-- <a id="mobile" class="menulink" href="<%=request.getContextPath()%>/mobile/inboxes.jsp"><%= menuResource.getString("mobile") %></a>  -->
	</div>
	<div class="menu4">
		<a id="support" class="menulink" href="mailto:<%= bbconfig.getString(Config.BLUEBOX_HELPMAIL)%>">
			<div class="userpic" style="background-image:url('<%= bbconfig.getString(Config.BLUEBOX_HELPPHOTO)%>')" onmouseover="dijit.Tooltip.defaultPosition=['below']"></div>
				<div class="dijitHidden">
					<span data-dojo-type="dijit.Tooltip" data-dojo-props="connectId:'support'"><%= bbconfig.getString(Config.BLUEBOX_HELPNAME) %></span>
				</div>
		</a>
	</div>
	<div class="menu3">
		<a id="docs" class="menulink" href="<%=request.getContextPath()%>/app/docs.jsp"><%= menuResource.getString("docs") %></a>
		<a id="admin" class="menulink" href="<%=request.getContextPath()%>/app/admin.jsp"><%= menuResource.getString("admin") %></a>
		<a id="help" class="helpIcon" href="<%=request.getContextPath()%>/app/help.jsp" onmouseover="dijit.Tooltip.defaultPosition=['below']">?</a>
		<div class="dijitHidden"><span data-dojo-type="dijit.Tooltip" data-dojo-props="connectId:'help'"><%= menuResource.getString("help") %></span></div>
		<span id="update" style="display:none;" class="menulink"><jsp:include page="update.jsp" /></span>
	</div>

</div>
<div id="messageDiv" class="message">Loading</div>