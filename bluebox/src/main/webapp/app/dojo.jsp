<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%> 
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
%>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"> 
<link rel="SHORTCUT ICON" href="/app/<%=bbconfig.getString("bluebox_theme")%>/favicon.ico" />

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
<script data-dojo-config="parseOnLoad:true, locale: 'en-us',extraLocale: ['fr','de','zn']" src="<%=bbconfig.getString("dojo_base")%>/dojo/dojo.js"></script>

<!--  load google web fonts  -->
<link type="text/css" href='http://fonts.googleapis.com/css?family=Roboto:700,400&subset=latin,cyrillic-ext,greek-ext,greek,vietnamese,latin-ext,cyrillic' rel='stylesheet'>
<link type="text/css" href="<%=request.getContextPath()%>/app/index.css" rel="stylesheet" />
<link type="text/css" href="<%=request.getContextPath()%>/app/<%=bbconfig.getString("bluebox_theme")%>/theme.css" rel="stylesheet" />
