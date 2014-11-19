<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox" language="java"%>
<%@ page import="com.bluebox.Config" language="java"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage" language="java"%>
<%@ page import="java.util.ResourceBundle" language="java"%>
<%@ page import="com.bluebox.BlueBoxServlet" language="java"%>
<%@ page import="com.bluebox.rest.json.JSONChartHandler"%>

<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle chartResource = ResourceBundle.getBundle("charts",request.getLocale());
%>

<script>
require(["dojox/charting/Chart", "dojox/charting/StoreSeries", "dojo/store/JsonRest", "dojox/charting/axis2d/Default", "dojox/charting/plot2d/Lines", "dojox/charting/plot2d/StackedAreas", "dojox/charting/themes/BlueDusk", "dojo/ready"],
		  function(Chart, StoreSeries, JsonRest, Default, Lines, StackedAreas, BlueDusk, ready){
		  ready(function() {
			// monthly chart
			try {
				var dataStore = new JsonRest({target:"<%=request.getContextPath()%>/<%=JSONChartHandler.JSON_ROOT%>/monthly"});
				var monthlychart = new Chart("monthlychart");
				monthlychart.setTheme(BlueDusk);
				monthlychart.addPlot("default", {type: StackedAreas,tension: "X",stroke: { width: 1.5, color: "#CCD6EB" }});
				monthlychart.addAxis("x");
				monthlychart.addAxis("y", {vertical: true});
				try {
				   	monthlychart.addSeries("y", new StoreSeries(dataStore, { query: {} }, "value"));
				}
				catch (err) {
				  	alert(err);
				}
				monthlychart.render();
			 }
		    catch (err) {
		    	alert(err);
		    }
		    
		 // weekly chart            
			try {		
				var dataStore = new JsonRest({target:"<%=request.getContextPath()%>/<%=JSONChartHandler.JSON_ROOT%>/weekly"});
			   	var results = dataStore.query("").then(function(data){
			   			require(["dojox/charting/plot2d/Pie"],
									    function(Pie){
										    		var monthlychart = new Chart("weeklychart");
										            monthlychart.setTheme(BlueDusk);
											        monthlychart.addPlot("default", {type: Pie, fontColor: "lightGray", stroke: { width: 1.5, color: "#CCD6EB" }, markers:false, labels:true, labelStyle: "default"});
											    	monthlychart.addSeries("y", data);
												    monthlychart.render();
							    		 });
					    });
					    console.log(results);
		    }
		    catch (err) {
		    	alert(err);
		    }
		    
		 	// hourly chart            
			try {		
				var hdataStore = new JsonRest({target:"<%=request.getContextPath()%>/<%=JSONChartHandler.JSON_ROOT%>/hourly"});
				var hourlychart = new Chart("hourlychart");
				hourlychart.setTheme(BlueDusk);
				hourlychart.addPlot("default", {type: StackedAreas,tension: "X",stroke: { width: 1.5, color: "#CCD6EB" }});
				hourlychart.addAxis("x");
				hourlychart.addAxis("y", {vertical: true});
				try {
				   	hourlychart.addSeries("y", new StoreSeries(hdataStore, { query: {} }, "value"));
				}
				catch (err) {
				  	alert(err);
				}
				hourlychart.render();
		    }
		    catch (err) {
		    	alert(err);
		    }
		});	
	});	
</script>

<div class="leftSideContent">
	<h2><%= chartResource.getString("charts_daily_title") %></h2>
		<div id="monthlychart" style="width: 300px; height: 150px; margin: 5px auto 0px auto;"></div>
	<h2><%= chartResource.getString("charts_weekly_title") %></h2>
		<div id="weeklychart" style="width: 300px; height: 150px; margin: 5px auto 0px auto;"></div>
	<h2><%= chartResource.getString("charts_hourly_title") %></h2>
]		<div id="hourlychart" style="width: 300px; height: 150px; margin: 5px auto 0px auto;"></div>
]</div>