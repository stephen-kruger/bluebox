<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.rest.json.JSONFolderHandler"%>
<%@ page language="java" import="java.util.ResourceBundle" %>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<% 
	ResourceBundle folderDetailResource = ResourceBundle.getBundle("folderDetails",request.getLocale());

%>
<script>		
	
	var folderEmail = "<%= StringEscapeUtils.escapeJavaScript(request.getParameter(Inbox.EMAIL)) %>";
	if (folderEmail=="null")
		folderEmail = "";
		
	function setInboxCount(count) {
		try {
			document.getElementById("inboxCount").innerHTML = count;		
		}
		catch (err) {
			console.log("folders1:"+err);
		}
	}
	
	function setDeletedCount(count) {
		try {
			document.getElementById("deletedCount").innerHTML = count;			
		}
		catch (err) {
			console.log("folders2:"+err);
		}
	}
	
	function loadInboxAndFolder(email, state) {
		console.log("loadInboxAndFolder "+state);
		loadInbox(email, state);
		loadFolder(email);
	} 
	
	function loadFolder(newEmail) {
		console.log("loadFolder:"+newEmail);

		try {
			require(["dojox/data/JsonRestStore"], function (JsonRestStore) {
				var urlStr = "<%=request.getContextPath()%>/<%=JSONFolderHandler.JSON_ROOT%>/"+encodeURI(newEmail);
				var jStore = new dojox.data.JsonRestStore({target:urlStr,syncMode:true});
				var queryResults = jStore.fetch({}).results;
				setInboxCount(queryResults.NORMAL.count);
				setDeletedCount(queryResults.DELETED.count);
				folderEmail = newEmail;
			});
		}
		catch (err) {
			console.log("folders3:"+err);
		}
	}
	
	function filter(item, node) {
		try {
			if (node.label.indexOf("<%= folderDetailResource.getString("inboxfor") %>")<0) {
				loadInbox(item.email, item.state);
			}
		}
		catch (err) {
			console.log("folders4:"+err);
		}
	}
	
	require(["dojo/domReady!"], function(domReady) {
		loadFolder(folderEmail);
	});
</script>
	
<!-- the inbox folder tree -->                
<header>
	<h1 id="mailTitle"></h1>
</header>
<div>
	<ul>
		<li style="list-style-type:none;cursor:pointer;padding:0.4em;">
		  	<a id="<%=BlueboxMessage.State.NORMAL.ordinal()%>" class="selectedFolder" onclick="loadInbox(folderEmail, '<%=BlueboxMessage.State.NORMAL.ordinal()%>');">
			  	<img style="padding-right : 5px;" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/inboxNormal.png" alt="<%= folderDetailResource.getString("inbox") %>"/><%= folderDetailResource.getString("inbox") %>
			  	<span id="inboxCount" class="badgeDown">?</span>
		  	</a>
		</li>
  		<li style="list-style-type:none;cursor:pointer;padding:0.4em;">
		  	<a id="<%=BlueboxMessage.State.DELETED.ordinal()%>" class="unselectedFolder" onclick="loadInbox(folderEmail, '<%=BlueboxMessage.State.DELETED.ordinal()%>');">
		  		<img style="padding-right : 5px;" src="<%=request.getContextPath()%>/app/<%=Config.getInstance().getString("bluebox_theme")%>/inboxTrash.png" alt="<%= folderDetailResource.getString("inbox") %>"/><%= folderDetailResource.getString("trash") %>
		  		<span id="deletedCount" class="badgeDown">?</span>
		  	</a>
		</li>
	</ul>
</div><!--end subsection-->