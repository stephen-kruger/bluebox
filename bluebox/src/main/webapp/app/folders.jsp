<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.rest.json.JSONFolderHandler"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>

<% 
	ResourceBundle folderDetailResource = ResourceBundle.getBundle("folderDetails",request.getLocale());

%>
<script>		
	
	var folderEmail = "<%= request.getParameter(Inbox.EMAIL) %>";
	if (folderEmail=="null")
		folderEmail = "";
		
	function setInboxCount(count) {
		document.getElementById("inboxCount").innerHTML = count;				
	}
	
	function setDeletedCount(count) {
		document.getElementById("deletedCount").innerHTML = count;				
	}
	
	function loadInboxAndFolder(email) {
		loadInbox(email);
		loadFolder(email);
	} 
	
	function loadFolder(newEmail) {
		
		require(["dojox/data/JsonRestStore"], function (JsonRestStore) {
			var urlStr = "<%=request.getContextPath()%>/<%=JSONFolderHandler.JSON_ROOT%>/"+encodeURI(newEmail);
			var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:true});
			var queryResults = jStore.fetch({}).results;
			setInboxCount(queryResults.items[0].children[0].count);
			setDeletedCount(queryResults.items[0].children[1].count);
			folderEmail = newEmail;
		});
	}
	
	function filter(item, node) {
		if (node.label.indexOf("<%= folderDetailResource.getString("inboxfor") %>")<0) {
			loadInbox(item.email, item.state);
		}
	}
	
	require(["dojo/domReady!"], function(domReady) {
		loadFolder(folderEmail);
	});
</script>
<style type="text/css">
		
	.unselectedFolder {
		background:#ffffff;
		padding:5px;
	}
	
	.selectedFolder {
		background:#efefef;
		padding:5px;
		border-radius: 1em;
	}

</style>
	
<!-- the inbox folder tree -->                
<header>
	<h2 id="mailTitle"></h2>
</header>
<div>
	<ul>
		<li style="list-style-type:none;cursor:pointer;padding:0.1em;">
		  	<a id="<%=BlueboxMessage.State.NORMAL%>" class="selectedFolder" onclick="loadInbox(folderEmail, '<%=BlueboxMessage.State.NORMAL%>');">
			  	<img style="padding-right : 5px; width:16px; height:16px;" src="<%=request.getContextPath()%>/app/images/inboxNormal.png" alt="<%= folderDetailResource.getString("inbox") %>"/><%= folderDetailResource.getString("inbox") %>
			  	<span id="inboxCount" class="badgeDown">?</span>
		  	</a>
		</li>
  		<li style="list-style-type:none;cursor:pointer;padding:0.1em;">
		  	<a id="<%=BlueboxMessage.State.DELETED%>" class="unselectedFolder" onclick="loadInbox(folderEmail, '<%=BlueboxMessage.State.DELETED%>');">
		  		<img style="padding-right : 5px; width:16px; height:16px;" src="<%=request.getContextPath()%>/app/images/inboxTrash.png" alt="<%= folderDetailResource.getString("inbox") %>"/><%= folderDetailResource.getString("trash") %>
		  		<span id="deletedCount" class="badgeDown">?</span>
		  	</a>
		</li>
	</ul>
</div><!--end subsection-->