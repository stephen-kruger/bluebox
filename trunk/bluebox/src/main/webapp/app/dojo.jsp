<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
%>
<link rel="SHORTCUT ICON" href="images/favicon.ico" />

<!-- core dojo style sheets -->
<link
	href="<%=bbconfig.getString("dojo_base")%>/dojo/resources/dojo.css"
	rel="stylesheet" type="text/css" />
<link
	href="<%=bbconfig.getString("dojo_base")%>/dijit/themes/<%=bbconfig.getString("dojo_style")%>/<%=bbconfig.getString("dojo_style")%>.css"
	rel="stylesheet" type="text/css" />
<link
	href="<%=bbconfig.getString("dojo_base")%>/dojox/grid/resources/Grid.css"
	rel="stylesheet" rel="stylesheet" type="text/css" />
<link
	href="<%=bbconfig.getString("dojo_base")%>/dojox/grid/enhanced/resources/claro/EnhancedGrid.css"
	rel="stylesheet" rel="stylesheet" type="text/css" />
<link
	href="<%=bbconfig.getString("dojo_base")%>/dojox/grid/enhanced/resources/EnhancedGrid_rtl.css"
	rel="stylesheet" rel="stylesheet" type="text/css" />	
	
<!-- Load Dojo, Dijit, and DojoX resources from Google CDN -->
<script data-dojo-config="parseOnLoad:false" src="<%=bbconfig.getString("dojo_base")%>/dojo/dojo.js"></script>

<!--  load google web fonts  -->
<link href='http://fonts.googleapis.com/css?family=Roboto:700,400&subset=latin,cyrillic-ext,greek-ext,greek,vietnamese,latin-ext,cyrillic' rel='stylesheet' type='text/css'>

<link type="text/css" href="index.css" rel="stylesheet" />
