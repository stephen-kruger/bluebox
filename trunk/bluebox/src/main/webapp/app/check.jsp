<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.rest.json.JSONAutoCompleteHandler"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%
	ResourceBundle checkResource = ResourceBundle.getBundle("check",request.getLocale());
%>
<%
	String email;
	if (request.getParameter(Inbox.EMAIL)!=null) {
		email = request.getParameter(Inbox.EMAIL);
	}
	else {
		email = "";
	}
%>
<style type="text/css">
	.checkbox {
		position:fixed;
		top:35%;
		bottom:20%;
		left:20%;
		right:20%;
	 	vertical-align: middle;
	}
	
	.checkboxContent {
	 	display: table;
		vertical-align: middle;
		text-align: center;
		width:100%;
		height:100%;
		margin-right:2em;
	}
</style>
<script type="text/javascript">

	require(["dijit/form/Button"]);

	function clearForm(){
		try {
			document.getElementById('<%= Inbox.EMAIL %>').value = "";
			submitForm();
		}
		catch (err) {
			alert("check1:"+err);
		}
	}
	
	function submitForm(){
		try {
			document.getElementById('checkemail').submit();
		}
		catch (err) {
			alert("check2:"+err);
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
	                   value: "<%=email%>",
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
			alert("check3:"+err);
		}
    }
	
	require(["dojo/domReady!"], function(domready){
		  // will not be called until DOM is ready
			initTypeAhead();
	});
</script>	

<div class="checkboxContent">
         <form id="checkemail" name="checkemail" action="<%=request.getContextPath()%>/app/inbox.jsp" method="get">
            	<h2><%= checkResource.getString("header") %></h2>
           
            	<input id="<%= Inbox.EMAIL %>" name="<%= Inbox.EMAIL %>"/>
			            	
            	<button onclick="submitForm();" data-dojo-type="dijit/form/Button" data-dojo-props="iconClass:'dijitEditorIcon  dijitEditorIconTabIndent', showLabel: false" type="button"><%= checkResource.getString("go") %></button>
         </form>
</div>