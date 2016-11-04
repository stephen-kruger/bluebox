<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%> 
<%@ page import="com.bluebox.Config"%>
<%
	Config bbconfig = Config.getInstance();
%>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="shortcut icon" type='image/x-icon' href="<%=request.getContextPath()%>/app/<%=bbconfig.getString("bluebox_theme")%>/favicon.ico" />

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
<script data-dojo-config="parseOnLoad:true, locale: 'en-us',extraLocale: ['fr','de','zn']" src="<%=bbconfig.getString("dojo_base")%>/dojo/dojo.js">

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

</script>

<!--  load google web fonts  -->
<link type="text/css" href='https://fonts.googleapis.com/css?family=Roboto:700,400&subset=latin,cyrillic-ext,greek-ext,greek,vietnamese,latin-ext,cyrillic' rel='stylesheet'>
<link type="text/css" href="<%=request.getContextPath()%>/app/index.css" rel="stylesheet" />
<link type="text/css" href="<%=request.getContextPath()%>/app/<%=bbconfig.getString("bluebox_theme")%>/theme.css" rel="stylesheet" />
<link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.6.3/css/font-awesome.min.css"/>
<link rel="stylesheet" href="//fonts.googleapis.com/icon?family=Material+Icons"/>
	
<script type="text/javascript" charset="utf-8">

	function getParamRaw(name){
	   if(name=(new RegExp('[?&]'+encodeURIComponent(name)+'=([^&]*)')).exec(location.search))
	      return decodeURIComponent(name[1]);
	}
	
	function getParam(name){
		p = getParamRaw(name);
		if ((p=="null")||(!p))
			p = "";
		return p;
	}
</script>
<style>
	font-family: noto;
</style>