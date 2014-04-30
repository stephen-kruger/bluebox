<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" pageEncoding="utf-8"
	contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.rest.json.JSONAttachmentHandler"%>
<%@ page import="com.bluebox.smtp.storage.BlueboxMessage"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.smtp.MimeMessageWrapper"%>


<%
	ResourceBundle mailDetailsResource = ResourceBundle.getBundle("mailDetails",request.getLocale());
	Config bbconfig = Config.getInstance();
%>
<script type="text/javascript" charset="utf-8">

	require(["dijit/form/Button"]);

	function getAttachmentIcon(attachmentName) {
		attachmentName = attachmentName.toLowerCase();
		
		if (attachmentName.indexOf(".pdf")>0) {
			return "../theme/images/ftPdf16.png";
		}
		if ((attachmentName.indexOf(".ppt")>0)||
				(attachmentName.indexOf(".odp")>0)) {
			return "../theme/images/ftPresentation16.png";
		}
		if ((attachmentName.indexOf(".doc")>0)||
				(attachmentName.indexOf(".docx")>0)||
				(attachmentName.indexOf(".rtf")>0)||
				(attachmentName.indexOf(".wps")>0)||
				(attachmentName.indexOf(".wpd")>0)||
				(attachmentName.indexOf(".odt")>0)) {
			return "../theme/images/ftWordProcessing16.png";
		}
		if ((attachmentName.indexOf(".avi")>0)||
				(attachmentName.indexOf(".mp4")>0)||
				(attachmentName.indexOf(".mpeg")>0)||
				(attachmentName.indexOf(".mov")>0)||
				(attachmentName.indexOf(".flv")>0)||
				(attachmentName.indexOf(".swf")>0)||
				(attachmentName.indexOf(".mkv")>0)||
				(attachmentName.indexOf(".wmv")>0)) {
			return "../theme/images/ftVideo16.png";
		}
		if ((attachmentName.indexOf(".csv")>0)||
				(attachmentName.indexOf(".xls")>0)||
				(attachmentName.indexOf(".xlsx")>0)||
				(attachmentName.indexOf(".xlr")>0)||
				(attachmentName.indexOf(".ods")>0)||
				(attachmentName.indexOf(".dat")>0)||
				(attachmentName.indexOf(".xml")>0)) {
			return "../theme/images/ftData16.png";
		}				
		if ((attachmentName.indexOf(".zip")>0)||
				(attachmentName.indexOf(".7z")>0)||
				(attachmentName.indexOf(".gz")>0)||
				(attachmentName.indexOf(".rar")>0)||
				(attachmentName.indexOf(".arc")>0)||
				(attachmentName.indexOf(".tgz")>0)||
				(attachmentName.indexOf(".tar")>0)) {
			return "../theme/images/ftCompressed16.png";
		}
		if ((attachmentName.indexOf(".txt")>0)||
				(attachmentName.indexOf(".text")>0)||
				(attachmentName.indexOf(".readme")>0)) {
			return "../theme/images/ftText16.png";
		}
		if ((attachmentName.indexOf(".wav")>0)||
				(attachmentName.indexOf(".aac")>0)||
				(attachmentName.indexOf(".aif")>0)||
				(attachmentName.indexOf(".wma")>0)||
				(attachmentName.indexOf(".m3u")>0)||
				(attachmentName.indexOf(".mid")>0)||
				(attachmentName.indexOf(".mp3")>0)) {
			return "../theme/images/ftAudio16.png";
		}
		if ((attachmentName.indexOf(".jpg")>0)||
				(attachmentName.indexOf(".gif")>0)||
				(attachmentName.indexOf(".png")>0)) {
			return "../theme/images/ftGraphic16.png";
		}
		return "../theme/images/attachment.png";
	}
	
	// create a link to rest service using */uid/attachment_index/filename
	function displayAttachments(uid, id, attachmentArray) {
		if (attachmentArray!=null) {
			var str = "";
			var base = "<%=JSONAttachmentHandler.JSON_ROOT%>/"+uid+"/";
			for (var i=0; i < attachmentArray.length; i++ ) {
				str += "&nbsp;<a href=\"../"+base+i+"/"+attachmentArray[i]+"\" target=\"_blank\"><img class='attachmentIcon' src='"+getAttachmentIcon(attachmentArray[i])+"'/>"+attachmentArray[i]+"</a>&nbsp;";
			}
			attachmenDiv = document.getElementById(id);
			attachmenDiv.innerHTML = str;
			attachmenDiv.style.display="block";
		}
	}
	
	function createEmailButton(parentId, labelStr) {
		new dijit.form.Button({
			id :parentId+labelStr,
	        label: labelStr,
	        onClick: function() {
	        	loadInboxAndFolder(labelStr);
	        }
	    },
	    parentId);	
	}
	
	function loadActualInbox() {
		loadInboxAndFolder(currentInbox);
	}
	
	function displayArrayButton(parentId, array) {
		var parentNode = document.getElementById(parentId);
		// remove all the previously added buttons
		while (parentNode.childNodes[0]) {
			parentNode.removeChild(cells[i].childNodes[0]);
		}
	
		if (array) {
			for (var i = 0; i < array.length; i++) {
				createEmailButton(parentId, encodeMyHtml(array[i]));
			}
		}
	}
	
	function removeQuotes(str) {
		return str.replace(/['"]/g,'');
	}
	
	/* This is for the detail section of the viewed email message */
	function displayArray(label, array) {
		if (array) {
			var str = "";
			for (var i = 0; i < array.length; i++) {
				if (i>0) {
					str += ", <a href='#' onclick='loadInboxAndFolder(\""+removeQuotes(array[i])+"\");'><img class=\"mailIcon\" src=\"../theme/images/iconMail.png\"/></a>"+encodeMyHtml(array[i]);
				}
				else {
					str += "<a href='#' onclick='loadInboxAndFolder(\""+removeQuotes(array[i])+"\");'><img class=\"mailIcon\" src=\"../theme/images/iconMail.png\"/></a>"+encodeMyHtml(array[i]);
				}
			}
			label.innerHTML = str;
		}
		else {			
			label.innerHTML = ""; // not set
		}
	}
	
	function clearDetail() {
		document.getElementById("mailHeaderBlock").style.display="none";
		document.getElementById("mailToggleBlock").style.display="none";
	
		document.getElementById("CcLabel").style.display="none";
		document.getElementById("From").innerHTML="";
		document.getElementById("To").innerHTML="";
		document.getElementById("Cc").innerHTML="";
		document.getElementById("Date").innerHTML="";
		document.getElementById("Attachment").innerHTML="";
	}
	
	function showDetail() {
		document.getElementById("mailHeaderBlock").style.display="block";
		document.getElementById("mailToggleBlock").style.display="block";
	}
	
	function loadDetail(uid) {
		currentUid = uid;
		clearDetail();
		showDetail();
		var xhrArgs = {
				url: "../rest/json/inbox/detail/"+uid,
				handleAs: "json",
				preventCache: false,
				load: function(data) {
					document.getElementById("subjectIcon").style.display="block";
					if (data.To){
						displayArray(document.getElementById("To"), data.To);
					}
					if (data.Cc){
						document.getElementById("CcLabel").style.display="block";
						displayArray(document.getElementById("Cc"), data.Cc);
					}
					if (data.From){
						displayArray(document.getElementById("From"), data.From);
					}
					if (data.Inbox){
						currentInbox = data.Inbox;
					}
					// subject may be null
					if (data.Subject)
						document.getElementById("Subject").innerHTML = data.<%=BlueboxMessage.SUBJECT%>;
					else
						document.getElementById("Subject").innerHTML = "";
					// date field may be null
					if (data.Date)
						document.getElementById("Date").innerHTML = data.Date[0];
					else
						document.getElementById("Date").innerHTML = "";
	
					if (data.Attachment){
						displayAttachments(uid,"Attachment",data.Attachment);
					}
					
					require(["dijit/registry"], function(registry) {
						registry.byId("html-tab").setContent(data.<%=MimeMessageWrapper.HTML_BODY%>);
						registry.byId("text-tab").setValue(data.<%=MimeMessageWrapper.TEXT_BODY%>);
						registry.byId("text-tab").resize();
					});
					
					// show the table with the most content by default
					if (data.<%=MimeMessageWrapper.HTML_BODY%>.length>data.<%=MimeMessageWrapper.TEXT_BODY%>.length) {
						selectMailTab("html-tab");
					}
					else {
						selectMailTab("text-tab");
					}
	
				},
				error: function (error) {
					alert("Something veeery bad happened :"+error);
				}
		};
	
		dojo.xhrGet(xhrArgs);
	}
	
	function encodeMyHtml(str) {
	   var div = document.createElement("div");
	   var text = document.createTextNode(str);
	   div.appendChild(text);
	   return div.innerHTML;
	}
	
	function selectMailTab(showTab) {
		require(["dijit/registry"], function(registry) {
			var tabs = registry.byId("mail-tab");
			var pane = registry.byId(showTab);
			tabs.selectChild(pane);
		});
	}
	
	require(["dijit/layout/TabContainer", "dijit/layout/ContentPane", "dijit/form/Textarea", "dojo/domReady!"], function(TabContainer, ContentPane, Textarea, domReady){
	    var tc = new TabContainer({
	        style: "width: 100%; height: 100%;",
	        tabPosition:"right-h",
	        doLayout:"false"
	    }, "mail-tab");

	    var cp1 = new Textarea({
	        title: "<%= mailDetailsResource.getString("text") %>",
	        id : "text-tab",
	        readonly:"readonly",
	        class:"textBody"
	   });
	   
	    tc.addChild(cp1);

	    var cp2 = new ContentPane({
	         title: "<%= mailDetailsResource.getString("html") %>",
	         id : "html-tab"
	    });
	    tc.addChild(cp2);

	    tc.startup();
	    
		clearDetail();

	});	
</script>	
<style>
.mailDate{
	align:right;
}

.headerValue{
	display:inline;
	font-weight:bold;
}

.headerLabel{
	font-weight: normal;
	padding-left:5px;
	padding-right:5px;
	color: Gray;
	display: inline-table !important;
}

.fromHeaderValue{
	font-weight: bold; 
	display:inline;
	color: #E26200;
	display: inline-table !important;
}

.subject {
  	font-weight: bold; 
	font-size:1.5em;
	float:left;
}

.mailIcon{
	position:static;
	vertical-align: middle;
	right:2px;
}

.attachmentIcon{
	position:static;
	right:-1px;
	font-weight: normal;
	vertical-align: middle;
	width : 16px;
	height : 16px;
}

.headerBox {
	padding : 20px;
}

.headerBoxTable {
	width:100%;
	border:0;
}

.textBody {
	border: 0;
	overflow:auto;
	position:relative;
	min-height:400px;
	width:100%;
	vertical-align:top;
}

.htmlBody {
	width:100%;
	height:100%;
	border: 0;
}
</style>
<div id="mailHeaderBlock" class="headerBox" style="padding-top: 10px">
	<table class="headerBoxTable">
		<tr>
			<td valign="top"><img id="subjectIcon"
				src="../theme/images/message.png" align="top" /></td>
			<td>
				<table class="headerBoxTable">
					<tr>
						<td><span id="Subject" class="subject"></span></td>
					</tr>
					<tr>
						<td  align="left">
							<span id="From" class="fromHeaderValue"></span>&nbsp;
						<td align="right">
							<span id="Date" class="mailDate"></span>
						</td>
					</tr>
					<tr>
						<td  align="left">
							<span class="headerLabel">&nbsp;<%= mailDetailsResource.getString("to") %></span>&nbsp;
							<span id="To" class="headerValue"></span>
						</td>
					</tr>
					<tr>
						<td align="left">
							<span id="CcLabel" class="headerLabel">&nbsp;<%= mailDetailsResource.getString("cc") %></span>&nbsp;
							<span id="Cc" class="fromHeaderValue"></span>
						</td>
					</tr>
					<tr>
						<td align="left">
							<span class="headerLabel"><%= mailDetailsResource.getString("attachments") %></span>&nbsp;
							<div id="Attachment" class="fromHeaderValue"
								data-dojo-type="dijit/layout/ContentPane"
								style="padding: 0px 0px 0px 0px; display: none;"></div>
							</td>
					</tr>
				</table>
			</td>
		</tr>
	</table>
</div>


<!-- the Text vs Html view selector -->
<br />
<div id="mailToggleBlock">
	<div style="width: 100%; height: 480px;text-align:left;">
		<div id="mail-tab"></div>
	</div>
</div>