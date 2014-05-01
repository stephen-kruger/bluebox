<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
	ResourceBundle uploadResource = ResourceBundle.getBundle("upload",request.getLocale());
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title><%= uploadResource.getString("title") %></title>
	<jsp:include page="dojo.jsp" />
	<link href="<%=bbconfig.getString("dojo_base")%>/dojox/form/resources/UploaderFileList.css" rel="stylesheet" type="text/css" />
	
	<script>
		require(["dojo/domReady!","dijit/form/Button",  "dojox/form/Uploader", "dojox/form/uploader/FileList"], function(domReady,Button,Uploader,FileList){
			selectMenu("upload");
		});
	</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h2><%= uploadResource.getString("title") %></h2>
		</div>
			
		<div class="centerCol">
			<div>
				<form method="post" action="<%=request.getContextPath()%>/upload" id="myForm" enctype="multipart/form-data" >
				   <input name="uploadedfile" multiple="true" type="file" data-dojo-type="dojox.form.Uploader" label="<%= uploadResource.getString("uploadTitle") %>" id="uploader" />			   
				   <input type="submit" label="<%= uploadResource.getString("title") %>" data-dojo-type="dijit.form.Button" />
				</form>
			</div>
			<div id="files" data-dojo-type="dojox.form.uploader.FileList" uploaderId="uploader"></div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>