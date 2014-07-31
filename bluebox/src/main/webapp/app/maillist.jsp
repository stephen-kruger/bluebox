<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONRawMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONInboxHandler"%>

<%
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
%>

<style type="text/css">
	/* inbox */
		.inboxView {
			height:100%;
			width:100%;
			display:table;
		}
		
		.inboxGrid {
			width:100%;
			height:30%;
			display:table-row;
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
			console.log("loadInbox:"+email+" "+"<%= BlueboxMessage.State.NORMAL.name()%>"+" "+currentState);
			loadInbox(email,"<%= BlueboxMessage.State.NORMAL.name()%>");
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
					//document.getElementById("mailTitle").innerHTML = "<%=inboxDetailsResource.getString("inboxfor")%>"+email;
					document.getElementById("mailTitle").innerHTML = email;
				}
					
				// set the check fragment
				if (email)
				   	document.getElementById('<%=Inbox.EMAIL%>').value = email;
			   	else
			   		document.getElementById('<%=Inbox.EMAIL%>').value = "";
				
				// set the selected folder style
				if (state=="<%=BlueboxMessage.State.NORMAL%>") {
					document.getElementById('<%=BlueboxMessage.State.NORMAL%>').className = "selectedFolder";
					document.getElementById('<%=BlueboxMessage.State.DELETED%>').className = "unselectedFolder";
				}
				if (state=="<%=BlueboxMessage.State.DELETED%>") {
					document.getElementById('<%=BlueboxMessage.State.NORMAL%>').className = "unselectedFolder";
					document.getElementById('<%=BlueboxMessage.State.DELETED%>').className = "selectedFolder";	
				}
				currentEmail = email;
				loadStats();
			}
			catch (err) {
				alert("maillist1:"+err);
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
						alert("<%=inboxDetailsResource.getString("error.noselection")%>");
					}
				});
			}
			catch (err) {
				alert("maillist2:"+err);
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
								alert("<%=inboxDetailsResource.getString("error.unknown")%>"+error);
							}
					};
		
					dojo.xhrDelete(xhrArgs);		
				}
				else {
					alert("<%=inboxDetailsResource.getString("error.noselection")%>");
				}
			}
			catch (err) {
				alert("maillist3:"+err);
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
				alert("maillist4:"+err);
			}
	
		}
		
		function upload() {
			window.location = 'upload.jsp';
		}
		
		function loadRaw() {
			if (currentUid==null) {
				alert("<%=inboxDetailsResource.getString("error.noselection")%>");
			}
			else {
				var load = window.open("<%=request.getContextPath()%>/<%=JSONRawMessageHandler.JSON_ROOT%>/"+currentUid,'','scrollbars=yes,menubar=no,height=600,width=800,resizable=yes,toolbar=no,location=no,status=no');
			}
		}
		
		function atomFeed() {
			window.open("<%=request.getContextPath()%>/atom/inbox?email="+encodeURIComponent(currentEmail));
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
				alert("Error loading store :"+err);
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
				alert("setupTable:"+err);
			}
		}
		
		require(["dojo/domReady!","dojox/data/JsonRestStore"], function(domready, JSONRestStore) {
			// will not be called until DOM is ready
	    	var email = "<%=request.getParameter(Inbox.EMAIL)%>";
			if (email=="null")
				email = "";
			setupTable(email,"<%=BlueboxMessage.State.NORMAL%>");
			loadInbox(email,"<%=BlueboxMessage.State.NORMAL%>");
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
					<li><a href="javascript:;" onclick="atomFeed()"> <img
							class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbRss.png"
							alt="<%=inboxDetailsResource.getString("atomTooltip")%>" /><%=inboxDetailsResource.getString("atom")%></a>
					</li>
					<li><a href="javascript:;" onclick="deleteSelectedRows()">
							<img class="sixteenIcon" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/tbDelete.png"
							alt="<%=inboxDetailsResource.getString("deleteTooltip")%>" /><%=inboxDetailsResource.getString("delete")%></a>
					</li>
				</ul>
			</div>
			<!-- inbox datagrid  -->
			<div class="inboxGrid">
				<div id="gridDiv" class="gridDiv"></div>
			</div>
			<div class="detailPane"><jsp:include page="maildetail.jsp" /></div>
		</div>