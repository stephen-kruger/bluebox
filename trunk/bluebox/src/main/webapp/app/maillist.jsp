<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONSPAMHandler"%>
<%@ page import="com.bluebox.rest.json.JSONRawMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONInboxHandler"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
%>

<style type="text/css">
	/* inbox */
		.inboxView {
			height:100%;
			width:100%;
		}
		
		.inboxGrid {
			width:100%;
			height:30%;
		}
		
		.dojoxGridCell {
			border: none !important;
			padding-left: 5px !important;
			white-space: nowrap;
			border-top: none;
		}
		
		.dojoxGridScrollbox {
			overflow-x:hidden;
		}
		
		.dojoxGridHeader div {
			font-style: bold !important;
			background:#CCD6EB;
		}
		
		.dojoxGrid, .gridDiv {
			width:100%;
			height:200px;
			background-color: #ffffff;
		}
		
		.dojoxGridArrowButtonChar {
		    display: inline;
		    float: left;
		}
		
		.dojoxGridHeader .dojoxGridCellFocus {
		    border: 0.5px solid;
		}
	
		.navcontainer ul {
			display: table-row;
			margin: 0;
			padding: 0;
			list-style-type: none;
			text-align: center;
		}
	
		.navcontainer ul li { 
			display: inline; 
		}
		
		.navcontainer ul li a {
			text-decoration: none;
			padding: .2em 1em;
			color: #000;
		}
		
		.navcontainer ul li a:hover {
			color: #000;
		}
		
</style>
	<script type="text/javascript" charset="utf-8">
		
		require(["dojox/data/JsonRestStore"]);
		
		function loadInbox(email) {
			console.log("loadInbox:"+email+" "+"<%= BlueboxMessage.State.NORMAL.ordinal()%>"+" "+currentState);
			loadInbox(email,"<%= BlueboxMessage.State.NORMAL.ordinal()%>");
		}
		
		function trimString(s,maxlen) {
			if (s.length>maxlen) {
				s = s.substring(0,maxlen)+"...";
			}	
			return s;
		}
		
		function loadInbox(email, state) {
			currentState = state;
			console.log("loadInbox2:"+email+" >"+state+"<");
			try {
				require(["dijit/registry"], function(registry){
				    grid = registry.byId("grid");
				    if (grid) {
				    	clearSelection();
				    	store = getStore(email, state);
						grid.setStore(store, {});
				    }
				});	
					
				    
				// set the banner title
				if (email=="") {
					document.getElementById("mailTitle").innerHTML = "<%=inboxDetailsResource.getString("allMail")%>";
				}
				else {
					document.getElementById("mailTitle").innerHTML = trimString(email,22);
				}
					
				// set the check fragment
				if (email)
				   	document.getElementById('<%=Inbox.EMAIL%>').value = email;
			   	else
			   		document.getElementById('<%=Inbox.EMAIL%>').value = "";
				
				// set the selected folder style
				if (state=="<%=BlueboxMessage.State.NORMAL.ordinal()%>") {
					document.getElementById('<%=BlueboxMessage.State.NORMAL.ordinal()%>').className = "selectedFolder";
					document.getElementById('<%=BlueboxMessage.State.DELETED.ordinal()%>').className = "unselectedFolder";
				}
				if (state=="<%=BlueboxMessage.State.DELETED.ordinal()%>") {
					document.getElementById('<%=BlueboxMessage.State.NORMAL.ordinal()%>').className = "unselectedFolder";
					document.getElementById('<%=BlueboxMessage.State.DELETED.ordinal()%>').className = "selectedFolder";	
				}
				currentEmail = email;
				loadStats();
			}
			catch (err) {
				console.log("maillist1:"+err);
			}
	
		}
	
		function deleteSelectedRows() {
			try {
				var inbox = dijit.byId("grid");
				var items = inbox.selection.getSelected();
				var itemList = "";
				require(["dijit/registry"], function(registry){
				    var grid = registry.byId("grid");
					if(items.length){
						dojo.forEach(items, function(selectedItem){
							if(selectedItem !== null){
								itemList += grid.store.getValue(selectedItem, "<%=BlueboxMessage.UID%>")+",";
							}
						});
						deleteMail(itemList);
						if (items.length>1) {
							inbox.selection.clear();
						}
						loadInboxAndFolder(currentEmail, currentState);
					}
					else {
						console.log("<%=inboxDetailsResource.getString("error.noselection")%>");
					}
				});
			}
			catch (err) {
				console.log("maillist2:"+err);
			}
		}
		
		function spamSelectedRows() {
			
			require(["dijit/ConfirmDialog", "dojo/domReady!"], function(ConfirmDialog){
			    myDialog = new ConfirmDialog({
			        title: "<%=inboxDetailsResource.getString("spam")%>",
			        content: "<%=inboxDetailsResource.getString("confirm_spam")%>",
			        style: "width: 300px",
			        onExecute:function(){ //Callback function 
			        	try {
							var inbox = dijit.byId("grid");
							var items = inbox.selection.getSelected();
							var itemList = "";
							require(["dijit/registry"], function(registry){
							    var grid = registry.byId("grid");
								if(items.length){
									dojo.forEach(items, function(selectedItem){
										if(selectedItem !== null){
											itemList += grid.store.getValue(selectedItem, "<%=BlueboxMessage.UID%>")+",";
										}
									});
									spamMail(itemList);
									if (items.length>1) {
										inbox.selection.clear();
									}
									loadInboxAndFolder(currentEmail, currentState);
								}
								else {
									console.log("<%=inboxDetailsResource.getString("error.noselection")%>");
								}
							});
						}
						catch (err) {
							console.log("maillist2:"+err);
						}
			        },
			        onCancel:function(){ 
			            console.log("Event Cancelled");
			        }
			    });
			    myDialog.show();
			});
		}
		
		function spamMail(uidList) {
			try {
				if (currentUid) {
					var delUrl = "<%=request.getContextPath()%>/<%=JSONSPAMHandler.JSON_ROOT%>/"+uidList;
					var xhrArgs = {
							url: delUrl,
							handleAs: "json",
							preventCache: true,
							load: function(data) {
								loadInboxAndFolder(currentEmail, currentState);
								clearDetail();
							},
							error: function (error) {
								console.log("<%=inboxDetailsResource.getString("error.unknown")%>"+error);
							}
					};
		
					dojo.xhrDelete(xhrArgs);		
				}
				else {
					console.log("<%=inboxDetailsResource.getString("error.noselection")%>");
				}
			}
			catch (err) {
				console.log("maillist3:"+err);
			}
		}
	
		function refresh() {
			console.log("refresh "+currentEmail+ " "+currentState);
			loadInboxAndFolder(currentEmail, currentState);
		}
	
		function loadAll() {
			loadInboxAndFolder("");
		}
				
		function deleteMail(uidList) {
			try {
				if (currentUid) {
					var delUrl = "<%=request.getContextPath()%>/<%=JSONMessageHandler.JSON_ROOT%>/"+uidList;
					var xhrArgs = {
							url: delUrl,
							handleAs: "json",
							preventCache: true,
							load: function(data) {
								loadInboxAndFolder(currentEmail, currentState);
								clearDetail();
							},
							error: function (error) {
								console.log("<%=inboxDetailsResource.getString("error.unknown")%>"+error);
							}
					};
		
					dojo.xhrDelete(xhrArgs);		
				}
				else {
					console.log("<%=inboxDetailsResource.getString("error.noselection")%>");
				}
			}
			catch (err) {
				console.log("maillist3:"+err);
			}
		}
	
		function clearSelection() {
			try {
				require(["dijit/registry"], function(registry){
				    var widget = registry.byId("grid");
				    if (widget)
				    	widget.selection.clear();
			    });
			}
			catch (err) {
				console.log("maillist4:"+err);
			}
	
		}
		
		function upload() {
			window.location = 'upload.jsp';
		}
		
		function loadRaw() {
			if (currentUid==null) {
				console.log("<%=inboxDetailsResource.getString("error.noselection")%>");
			}
			else {
				var load = window.open("<%=request.getContextPath()%>/<%=JSONRawMessageHandler.JSON_ROOT%>/"+currentUid,'','scrollbars=yes,menubar=no,height=600,width=800,resizable=yes,toolbar=no,location=no,status=no');
			}
		}
		
		function rssFeed() {
			window.open("<%=request.getContextPath()%>/feed/inbox?email="+encodeURIComponent(currentEmail));
		}
		
		function getStore(email, state) {
			try {
				var urlStr = "<%=request.getContextPath()%>/<%=JSONInboxHandler.JSON_ROOT%>/"+encodeURI(email)+"/"+state;
				 var store = new dojox.data.JsonRestStore({ 
					    				target: urlStr, 
					    				parameters: [{name: "state", type: "string", optional: true}]
					    			    });
				    return store;
			}
			catch (err) {
				console.log("Error loading store :"+err);
				return null;
			}
		    
		}
		
		function isOdd(num) {
			return num % 2;
		}
			
		function setupTable(email,state) {
			try {
		      require(["dojox/grid/EnhancedGrid","dojox/data/JsonRestStore","dojox/grid/enhanced/plugins/Pagination","dojox/grid/enhanced/plugins/Selector"], function() {
			    // set the layout structure:
		    	var view = {
					cells: [[
						{name: '<%=inboxDetailsResource.getString("who")%>', field: '<%=BlueboxMessage.FROM%>', width: '20%', editable: false},
						{name: '<%=inboxDetailsResource.getString("subject")%>', field: '<%=BlueboxMessage.SUBJECT%>', width: '55%', editable: false},
						{name: '<%=inboxDetailsResource.getString("date")%>',  field: '<%=BlueboxMessage.RECEIVED%>', width: '15%', editable: false},
						{name: '<%=inboxDetailsResource.getString("size")%>',  field: '<%=BlueboxMessage.SIZE%>', width: '10%', editable: false},
						{name: 'UID',  field: '<%=BlueboxMessage.UID%>', hidden: 'true', editable: false}
					]]
				};
				
				var grid = new dojox.grid.EnhancedGrid({
				      id: 'grid',
				      store: getStore(email, state),
				      structure: view,
				      rowSelector: '0px',
				      plugins:{
				    	   // pagination: {
				    	    	//position: "bottom"
				    	    //},
				    	    selector: {
				    	    	col:"disabled",
				    	    	row:"multi",
				    	    	cell:"disabled"
				    	    }
				    	}
				      });
				grid.placeAt("gridDiv");
				grid.startup();
				
				// connect click events
				dojo.connect(grid, "onEndSelect", function(type, startPoint, endPoint, selected){
					  loadDetail(grid.store.getValue(grid.getItem(endPoint.row), "<%=BlueboxMessage.UID%>"));
					});
				
				// custom style
				//dojo.connect(grid, 'onStyleRow', this, function (row) {
					// needs some work - selected color is being overridden
			      // if (isOdd(row.index)) {
			       //       row.customStyles += "background-color:#ffffaf;";
			      // }
			    //});
				
		      });
			}
			catch (err) {
				console.log("setupTable:"+err);
			}
		}
		
		require(["dojo/domReady!","dojox/data/JsonRestStore"], function(domready, JSONRestStore) {
			// will not be called until DOM is ready
	    	var email = "<%=StringEscapeUtils.escapeJavaScript(request.getParameter(Inbox.EMAIL))%>";
			if (email=="null")
				email = "";
			setupTable(email,"<%=BlueboxMessage.State.NORMAL.ordinal()%>");
			loadInbox(email,"<%=BlueboxMessage.State.NORMAL.ordinal()%>");
		});
	</script>	
		
		<div class="inboxView">
			<div class="navcontainer">
				<ul>
					<li><a href="javascript:;" onclick="loadAll()"> <img
							class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbAll.png"
							alt="<%=inboxDetailsResource.getString("allTooltip")%>" /><%=inboxDetailsResource.getString("all")%>
					</a></li>
					<li><a href="javascript:;" onclick="loadRaw()"> <img
							class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbDownload.png"
							alt="<%=inboxDetailsResource.getString("downloadTooltip")%>" /><%=inboxDetailsResource.getString("download")%></a>
					</li>
					<li><a href="javascript:;" onclick="refresh()"> <img
							class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbRefresh.png"
							alt="<%=inboxDetailsResource.getString("refreshTooltip")%>" /><%=inboxDetailsResource.getString("refresh")%></a>
					</li>
					<li><a href="javascript:;" onclick="rssFeed()"> <img
							class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbRss.png"
							alt="<%=inboxDetailsResource.getString("rssTooltip")%>" /><%=inboxDetailsResource.getString("rss")%></a>
					</li>
					<li><a href="javascript:;" onclick="deleteSelectedRows()">
							<img class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbDelete.png"
							alt="<%=inboxDetailsResource.getString("deleteTooltip")%>" /><%=inboxDetailsResource.getString("delete")%></a>
					</li>
					<li><a href="javascript:;" onclick="spamSelectedRows()">
							<img class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbSpam.png"
							alt="<%=inboxDetailsResource.getString("spamTooltip")%>" /><%=inboxDetailsResource.getString("spam")%></a>
					</li>
				</ul>
			</div>
			<!-- inbox datagrid  -->
			<div class="inboxGrid">
				<div id="gridDiv" class="gridDiv"></div>
			</div>
			<div class="detailPane"><jsp:include page="maildetail.jsp" /></div>
		</div>