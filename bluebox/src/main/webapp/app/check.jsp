<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8" %>
<%@ page import="com.bluebox.smtp.Inbox" language="java" %>
<%@ page import="com.bluebox.rest.json.JSONAutoCompleteHandler" language="java" %>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler" language="java" %>
<%@ page import="java.util.ResourceBundle" language="java" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" language="java" %>
<% request.setCharacterEncoding("utf-8"); %>

<%
	ResourceBundle checkResource = ResourceBundle.getBundle("check",request.getLocale());
%>

<style type="text/css">
.checkbox {
	position: fixed;
	top: 35%;
	bottom: 20%;
	left: 20%;
	right: 20%;
	vertical-align: middle;
}

.checkboxContent {
	display: inline;
	vertical-align: middle;
	text-align: center;
	width: 80%;
	height: 100%;
}
</style>
<script type="text/javascript" charset="utf-8">

	require(["dijit/form/Button"]);

	function clearForm(){
		try {
			document.getElementById('<%= Inbox.EMAIL %>').value = "";
			submitForm();
		}
		catch (err) {
			console.log("check1:"+err);
		}
	}
	
	function submitForm(){
		try {
			document.getElementById('checkemail').submit();
		}
		catch (err) {
			console.log("check2:"+err);
		}
	}
			
	function initTypeAhead() {
		try {
			require(["dojox/data/QueryReadStore","dijit/form/ComboBox"], function (QueryReadStore,ComboBox) {
	               var stateStore = new dojox.data.QueryReadStore({
	                   url: "<%=request.getContextPath()%>/<%= JSONAutoCompleteHandler.JSON_ROOT %>"
	               });
	               new dijit.form.ComboBox({
	                   id: "<%= Inbox.EMAIL %>",
	                   name: "Email",
	                   value: getParam("<%= Inbox.EMAIL %>"),
	                   autocomplete:true,
	                   store: stateStore,
	                   placeholder: "<%= checkResource.getString("typeaheadPlaceholder") %>",
	                   searchAttr: "label",
	                   	onChange:function(){      
	      					document.getElementById('checkemail').submit();
	      		    	}
	               },
	
	               "<%= Inbox.EMAIL %>");
			});
		}
		catch (err) {
			console.log("check3:"+err);
		}
    }
	
	require(["dojo/domReady!"], function(domready){
		  // will not be called until DOM is ready
			initTypeAhead();
	});
</script>

<div class="checkboxContent">
	<table>
		<tr>
			<td style="float:left;">
				<h2><%= checkResource.getString("header") %></h2>
			</td>
		</tr>
		<tr>
			<td>
				<form id="checkemail" name="checkemail"
					action="<%=request.getContextPath()%>/app/inbox.jsp" method="get">
					<input id="<%= Inbox.EMAIL %>" name="<%= Inbox.EMAIL %>" />
				</form>
			</td>
			<td>
				<button onclick="submitForm();" data-dojo-type="dijit/form/Button"
					data-dojo-props="iconClass:'dijitEditorIcon  dijitEditorIconTabIndent', showLabel: false"
					type="button"></button>
			</td>
		</tr>
	</table>
</div>