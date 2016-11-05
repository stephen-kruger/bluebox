<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.InboxResource"%>
<%@ page import="com.bluebox.rest.MessageResource"%>
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
		
		.tbButton:hover {
			background:#E8F4F4;
			border-radius: 1em;
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
			//console.log("loadInbox2:"+email+" >"+state+"<");
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
					document.getElementById("mailTitle").innerHTML = "<span>"+trimString(email,22)+"</span>";
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
					var delUrl = "<%=request.getContextPath()%>/jaxrs<%=InboxResource.PATH%>/"+uidList;
					var xhrArgs = {
							url: delUrl,
							handleAs: "text",
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
					var delUrl = "<%=request.getContextPath()%>/jaxrs<%=MessageResource.PATH%>/delete/"+uidList;
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
				var load = window.open("<%=request.getContextPath()%>/jaxrs<%=MessageResource.PATH%>/raw/"+currentUid,'','scrollbars=yes,menubar=no,height=600,width=800,resizable=yes,toolbar=no,location=no,status=no');
			}
		}
		
		function rssFeed() {
			window.open("<%=request.getContextPath()%>/feed/inbox?email="+currentEmail);
		}
		
		function getStore(email, state) {
			try {
				var urlStr = "<%=request.getContextPath()%>/jaxrs<%=InboxResource.PATH%>/list/"+email+"/"+state;
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
			// this was buggy when displaying asian fullname emails
	    	//var email = "<%=StringEscapeUtils.escapeJavaScript(request.getParameter(Inbox.EMAIL))%>";
	    	//var email = "<%=request.getParameter(Inbox.EMAIL)%>";
			//if (email=="null")
			//	email = "";
			var email = getParam("<%= Inbox.EMAIL %>");

			setupTable(email,"<%=BlueboxMessage.State.NORMAL.ordinal()%>");
			loadInbox(email,"<%=BlueboxMessage.State.NORMAL.ordinal()%>");
		});
	</script>	
		
		<div class="inboxView">
			<div class="navcontainer">
				<ul>
					<li class="tbButton">
						<a href="javascript:;" onclick="loadAll()"> 
							<i class="fa fa-lg fa-envelope-o" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("allTooltip")%>"></i>
							<%=inboxDetailsResource.getString("all")%>
						</a>
					</li>
					<li class="tbButton">
						<a href="javascript:;" onclick="loadRaw()"> 
							<i class="fa fa-lg fa-save" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("downloadTooltip")%>"></i>
							<%=inboxDetailsResource.getString("download")%>
						</a>
					</li>
					<li class="tbButton">
						<a href="javascript:;" onclick="refresh()"> 
							<i class="fa fa-lg fa-refresh" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("refreshTooltip")%>"></i>
							<%=inboxDetailsResource.getString("refresh")%>
						</a>
					</li>
					<li class="tbButton">
						<a href="javascript:;" onclick="rssFeed()">
							<i class="fa fa-lg fa-rss" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("rssTooltip")%>"></i>
							<%=inboxDetailsResource.getString("rss")%>
						</a>
					</li>
					<li class="tbButton">
						<a href="javascript:;" onclick="deleteSelectedRows()">
							<i class="fa fa-lg fa-trash-o" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("deleteTooltip")%>"></i>
							<%=inboxDetailsResource.getString("delete")%>
						</a>
					</li>
					<li class="tbButton">
						<a href="javascript:;" onclick="spamSelectedRows()">
							<i class="fa fa-lg fa-ban" style="vertical-align:middle" title="<%=inboxDetailsResource.getString("spamTooltip")%>"></i>
							<%=inboxDetailsResource.getString("spam")%>
						</a>
					</li>
				</ul>
			</div>
			<!-- inbox datagrid  -->
			<div class="inboxGrid">
				<div id="gridDiv" class="gridDiv"></div>
			</div>
			<div class="detailPane"><jsp:include page="maildetail.jsp" /></div>
		</div>