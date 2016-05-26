<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.smtp.Inbox"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONRawMessageHandler"%>
<%
	ResourceBundle headerResource = ResourceBundle.getBundle("header",request.getLocale());
	ResourceBundle inboxDetailsResource = ResourceBundle.getBundle("inboxDetails", request.getLocale());
	Config bbconfig = Config.getInstance();
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
<title><%=headerResource.getString("welcome")%></title>
<jsp:include page="dojo.jsp" />
<style type="text/css">
.inboxAddress {
    border: none;
    display: inline;
}

.inboxAddressButton {
 	border:0px;
    margin: 2px;
    padding: 2px;
    display: inline;
    color : Gray;
    font-weight: bold;
}

.inboxEmail {
    border: none;
    display: none;
}
.addressName {
    color: blue;
}
.specialaddressName {
    color: red;
}
</style>
<script type="text/javascript" charset="utf-8">
require([
         "dojo/_base/declare", "dojo/parser", "dojo/ready",
         "dijit/_WidgetBase", "dijit/_TemplatedMixin"
     ], function(declare, parser, ready, _WidgetBase, _TemplatedMixin){

         declare("InboxAddress", [_WidgetBase, _TemplatedMixin], {
             templateString:
                 "<div class='inboxAddress'>" +
                 "<img class='mailIcon' src='<%=request.getContextPath()%>/app/<%=bbconfig.getString("bluebox_theme")%>/mailSmall.png'/>"+
                     "<div class='inboxAddress'><span data-dojo-attach-point='nameNode'></span></div>" +
                     "<div class='inboxAddressButton' onclick='toggle(this)'><img class='mailIcon' src='<%=request.getContextPath()%>/app/<%=bbconfig.getString("bluebox_theme")%>/mailSmall.png'/>" +
                     "  <div class='inboxEmail' data-dojo-attach-point='emailNode'></div>" +
                     "</div>"+
                 "</div>",

             // Attributes
             name: "unknown",
             _setNameAttr: { node: "nameNode", type: "innerHTML" },

             nameClass: "addressName",
             _setNameClassAttr: { node: "nameNode", type: "class" },

             email: "unknown",
             _setEmailAttr: { node: "emailNode", type: "innerHTML" }
         });
         
         
     });

function toggle(id) {
	 if (id.firstElementChild.style.display=="inline") {
		 id.firstElementChild.style.display="none";
	 }
	 else {
		 id.firstElementChild.style.display="inline";
	 }
}

	</script>
</head>
<body class="<%=Config.getInstance().getString("dojo_style")%>">
	<span data-dojo-type="InboxAddress" data-dojo-props="name:'John Smith', email:'john.smith@yoyo.com'"></span>
	<span data-dojo-type="InboxAddress" data-dojo-props="name:'Jack Bauer', nameClass:'specialaddressName', email:'jack.b@dingbat.org'"></span>
</body>
</html>