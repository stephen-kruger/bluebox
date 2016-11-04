<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page language="java" import="com.bluebox.Config"%>
<%@ page language="java" import="com.bluebox.Utils"%>
<%@ page language="java"
	import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page language="java" import="com.bluebox.smtp.Inbox"%>
<%@ page language="java" import="com.bluebox.rest.InboxResource"%>
<%@ page language="java" import="java.util.ResourceBundle"%>
<%@ page language="java" import="com.bluebox.smtp.InboxAddress"%>
<%@ page language="java" import="com.bluebox.Utils"%>
<%@ page import="com.bluebox.search.SearchUtils"%>
<%
	Config bbconfig = Config.getInstance();
%>
<%
	ResourceBundle menuResource = ResourceBundle.getBundle("menu",request.getLocale());
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxResource = ResourceBundle.getBundle("inbox",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails",request.getLocale());
%>

<style type="text/css">

</style>

<!--  search Javascript -->
<script type="text/javascript">
		
			var currentUid;
			var currentEmail, currentState;
			var searchScope = "<%=SearchUtils.SearchFields.ANY%>";
			
			function searchInbox(searchString) {
				try {
					clearSelection();
				    var urlStr = "<%=request.getContextPath()%>/jaxrs<%=InboxResource.PATH%>/search/"+searchScope+"/"+encodeURI(searchString);
				    require(["dojox/data/JsonRestStore"], function () {
					    var store = new dojox.data.JsonRestStore({ target: urlStr, parameters: [{name: "state", type: "string", optional: true}]});
					    require(["dijit/registry"], function(registry){
					        var widget = registry.byId("grid");
					        
					        // register interest in count changes so we display total search result size
					       // dojo.connect (widget, "_onFetchComplete", function(items) { 
					        	setInboxCount('*'); 
					        	setDeletedCount('*');
							//}); 
					        //function size(size, request){
					        //	setInboxCount(size); 
					        //}
	
					        //store.fetch({query: {}, onBegin: size, start: 9990, count: 9999});
					        widget.setStore(store, {});	   
					    });
				    });
				}
				catch (err) {
					console.log("search1:"+err);
				}
			}
				
			function search() {
				var searchString = document.getElementById("blueboxSearchText").value;
		   		searchInbox(searchString);
			}
			
			function setSearchScope(label,newScope) {
				searchScope = newScope;
				require(["dijit/registry"], function(registry){
				    var widget = registry.byId("searchScopeSelector");
				    if (widget)
				    	widget.set('label',label);
				});
				document.getElementById("blueboxSearchText").focus();
			}
			
			require(["dijit/form/DropDownButton", "dijit/DropDownMenu", "dijit/MenuItem", "dojo/dom", "dojo/domReady!"],
			        function(DropDownButton, DropDownMenu, MenuItem, dom){
			    var menu = new DropDownMenu();
			    var menuItem1 = new MenuItem({
			        label: "<%= inboxResource.getString("searchScopeAll") %>",
			        iconClass:"dijitIcon dijitIconConfigure",
			        onClick: function(){ setSearchScope('<%= inboxResource.getString("searchScopeAll") %>','<%=SearchUtils.SearchFields.ANY%>') }
			    });
			    menu.addChild(menuItem1);

			    var menuItem2 = new MenuItem({
			        label: "<%= inboxResource.getString("searchScopeSubject") %>",
			        iconClass:"dijitEditorIcon dijitEditorIconPaste",
			        onClick: function(){ setSearchScope('<%= inboxResource.getString("searchScopeSubject") %>','<%=SearchUtils.SearchFields.SUBJECT%>') }
			    });
			    menu.addChild(menuItem2);
			    
			    var menuItem3 = new MenuItem({
			        label: "<%= inboxResource.getString("searchScopeBody") %>",
			        iconClass:"dijitEditorIcon dijitEditorIconNewPage",
			        onClick: function(){ setSearchScope('<%= inboxResource.getString("searchScopeBody") %>','<%=SearchUtils.SearchFields.BODY%>') }
			    });
			    menu.addChild(menuItem3);
			    
			    var menuItem4 = new MenuItem({
			        label: "<%= inboxResource.getString("searchScopeRecipients") %>",
			        iconClass:"dijitIcon dijitIconUsers",
			        onClick: function(){ setSearchScope('<%= inboxResource.getString("searchScopeRecipients") %>','<%=SearchUtils.SearchFields.RECIPIENTS%>') }
			    });
			    menu.addChild(menuItem4);
			    
			    var menuItem5 = new MenuItem({
			        label: "<%= inboxResource.getString("searchScopeFrom") %>",
			        iconClass:"dijitIcon dijitIconMail",
			        onClick: function(){ setSearchScope('<%= inboxResource.getString("searchScopeFrom") %>','<%=SearchUtils.SearchFields.FROM%>') }
			    });
			    menu.addChild(menuItem5);

			    var button = new DropDownButton({
			        label: "<%= inboxResource.getString("searchScopeAll") %>",
			        style: "padding-left:10px",
			        name: "searchScopeSelector",
			        dropDown: menu,
			        id: "searchScopeSelector"
			    });
			    dom.byId("dropDownButtonContainer").appendChild(button.domNode);				
			});
			
		</script>
<!-- end search Javascript -->
<!-- search -->
<form action="javascript:;">
	<table class="searchBox">
		<tr>
			<td>
				<div id="dropDownButtonContainer" class="noBorder"></div>
			</td>
			<td><input
				placeholder="<%= inboxResource.getString("searchPlaceholder") %>"
				id="blueboxSearchText" name="blueboxSearchText"
				class="lotusText lotusInactive" type="text" title="Search" /></td>
			<td>
				<i style="cursor: pointer; cursor: hand;" alt="<%= inboxDetailsResource.getString("search") %>" title="<%= inboxDetailsResource.getString("search") %>" onclick="search()" class="material-icons" style="vertical-align:middle">search</i>
			</td>
		<tr>
	</table>
</form>
<!-- end search -->