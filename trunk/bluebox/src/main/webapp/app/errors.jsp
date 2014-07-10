<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.json.JSONErrorHandler"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle menuResource = ResourceBundle.getBundle("menu",request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%=headerResource.getString("welcome")%></title>
	<jsp:include page="dojo.jsp" />
	
	<style type="text/css">
		.errorList {
			height:15em;
			width:100%;
		}
		
		.errorBody {
			height:20em;
			width:100%;
		}
		
		.dojoxGridHeader div {
			font-style: bold !important;
			background:#CCD6EB;
		}
		
		.claro .dojoxGridPaginator {
			background:#CCD6EB;
		}
	</style>
	
	<script type="text/javascript">
		require(["dojo/parser", "dijit/form/Textarea"]);
		
		function setupTable() {
			try {
		      require(["dojox/grid/EnhancedGrid",
		               "dojox/data/JsonRestStore",
		               "dojox/grid/enhanced/plugins/Pagination",
		               "dojox/grid/enhanced/plugins/Selector"], function() {
			    // set the layout structure:
		    	var view = {
					cells: [[
						{name: '<%= menuResource.getString("errors") %>', field: 'title', width: 'auto', editable: false},
						{name: 'ID',  field: 'id', hidden: 'true', editable: false}
					]]
				};
				
		    	var restStore = new dojox.data.JsonRestStore({ 
					target: "<%=request.getContextPath()%>/<%=JSONErrorHandler.JSON_ROOT%>/", 
					parameters: [{}]
				    });
				var grid = new dojox.grid.EnhancedGrid({
				      id: 'grid',
				      store: restStore,
				      structure: view,
				      rowSelector: '0px',
				      plugins:{
				    	    pagination: {
				    	    	position: "bottom"
				    	    },
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
					  loadError(grid.store.getValue(grid.getItem(endPoint.row), "id"));
					  document.getElementById("errorTitle").innerHTML = grid.store.getValue(grid.getItem(endPoint.row), "title");
					});			
		      });
			}
			catch (err) {
				alert("setupTable:"+err);
			}
		}
		
		function loadError(uid) {
			var xhrArgs = {
					url: "<%=request.getContextPath()%>/<%=JSONErrorHandler.JSON_DETAIL_ROOT%>/"+uid,
					handleAs: "text",
					preventCache: false,
					load: function(data) {
						//document.getElementById("errorBody").innerHTML = data;
						require(["dijit/registry"], function(registry) {
							registry.byId("errorBody").setValue(data);
						});
					},
					error: function (error) {
						//document.getElementById("errorBody").innerHTML = error;
						require(["dijit/registry"], function(registry) {
							registry.byId("errorBody").setValue(error);
						});
					}
			};

			var deferred = dojo.xhrGet(xhrArgs);
		}
				
		require(["dojo/domReady!","dojox/data/JsonRestStore"], function() {
					selectMenu("errors");
					setupTable();
		});
		
	</script>
</head>
<body class="<%=Config.getInstance().getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2><%= menuResource.getString("errors") %></h2>
		</div>
			
		<div class="centerCol">
			<div>
				<div class=errorList id="gridDiv"></div>
				<h3>Error details</h3>
				<div id="errorTitle"></div>
				<textarea id="errorBody" data-dojo-type="dijit/form/Textarea" readonly="readonly" class="errorBody"></textarea>	
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>