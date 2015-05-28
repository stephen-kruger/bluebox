<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.smtp.Inbox" language="java"%>
<%@ page import="com.bluebox.Config" language="java"%>
<%@ page import="com.bluebox.rest.json.JSONStatsHandler" language="java"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage" language="java"%>
<%@ page import="java.util.ResourceBundle" language="java"%>
<%@ page import="com.bluebox.BlueBoxServlet" language="java"%>
<% 
	Config bbconfig = Config.getInstance(); 
	ResourceBundle updateResource = ResourceBundle.getBundle("update",request.getLocale());
%>

<script type="text/javascript">	

function dialog(title, content) {
	require(["dijit/Dialog", "dojo/domReady!"], function(Dialog){
	    myDialog = new Dialog({
	        title: title,
	        content: content,
	        style: "width: 450px"
	    });
	    
	    var div = dojo.create('div', {}, myDialog.containerNode);
        dojo.style(dojo.byId(div), "padding", "2em");
        dojo.style(dojo.byId(div), "float", "middle");
	    var closeBtn = new dijit.form.Button({
            label: "Close",
            onClick: function(){
            	myDialog.hide();
                dojo.destroy(myDialog);
            }
         });
	    dojo.create(closeBtn.domNode,{}, div);
	    myDialog.show();
	});
}

function showUpdates() {
	var xhrArgs = {
			url: "<%=request.getContextPath()%>/rest/updateavailable",
			handleAs: "json",
			preventCache: false,
			load: function(data) {
				if(data.update_available) {
					document.getElementById("update").style.display="";
			    	dialog("<%= updateResource.getString("update_available") %>",
			    			"<%= updateResource.getString("current_version") %><b>"+data.current_version+"</b>"+
			    			"<br/><%= updateResource.getString("new_version") %><b>"+
			    			data.available_version+"</b><br/><%= updateResource.getString("download_text") %><a href='"+data.online_war+"'><%= updateResource.getString("link_text") %></a>");
				}
				else {
					document.getElementById("update").style.display="none";
				}
			},
			error: function (error) {
				console.log("Something veeery bad happened :"+error);
			}
	};

	dojo.xhrGet(xhrArgs);
}
</script>

<a href="#" onclick="showUpdates()" id="updateAvailable" class="blinkBadge"><%= updateResource.getString("update_available") %></a>