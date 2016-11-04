<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.MessageResource"%>
<%
	Config bbconfig = Config.getInstance();
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title>Documentation</title>
	<jsp:include page="dojo.jsp" />	
	<script>
		require(["dojo/ready", "dijit/registry", "dojo/parser"],
				function(ready, registry){
				  ready(function(){
					  selectMenu("docs");
				  });
		});
	</script>
	<style type="text/css">
		
		.json {
			border:0px;
			background:#ccc;
		}
	
	</style>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h1>Documentation</h1>
		</div>
			
		<div class="centerCol">
			<div style="text-align:left;">
					<h2>General usage</h2>
					<ol>
						<li style="list-style-type:none;">Please see the <a href="help.jsp">help and FAQ document</a></li>
					</ol>
					<h2>Installation</h2>
					For installation instructions please consult the <a href="https://github.com/stephen-kruger/bluebox">web</a> :
					<code style="width:200px">
					https://github.com/stephen-kruger/bluebox
					</code>
					
					<h2>REST API</h2>
					<ol>
						<li style="list-style-type:none;">For direct integration with test automation, the following REST calls may be used to reduce dependancy on UI
								<h3>List user emails</h3>
								This method lists all the emails for a particular email address, of the specified state<br/>
								<br/>
								http://[hostname]/[bluebox context]/jaxrs/inbox/list/[email]/[0=ANY, 1=NORMAL, 2=DELETED]/
								<br/><br/>
								<pre class="json">
								<code class="json">
[{
   "Uid": "bd222e31-e3a2-42fd-a6a1-6b27b75ce53d",
   "Sender": "Display Name &lt;test@test.com&gt;",
   "Subject": "Mail Subject",
   "Received": "8\/8\/14 2:06 AM",
   "State": 1,
   "Inbox": "recipient@test.com",
   "Size": "21K"
}]
								</code>
								</pre>
								<br/>
								<h3>List email detail</h3>
								This method lists the details of a particular email message specified by the UID<br/>
								<br/>
								http://[hostname]/[bluebox context]/jaxrs/<%=MessageResource.PATH %>/detail/[Uid]
								<br/><br/>
								<pre class="json">
								<code class="json">
{
	"Attachment":[null],
	"Uid":"bd222e31-e3a2-42fd-a6a1-6b27b75ce53d",
	"Recipient":["recipient@email.com"],
	"Cc":[],
	"Sender":["no-reply@email.com"],
	"Subject":"User - Your ......",
	"Inbox":"email@email.com",
	"Received":"08\/08\/14 02:06","State":1,"Size":"21",
	"HtmlBody":"...",
	"TextBody":"...",
	"Security":"..."
}
								</code>
								</pre>
								<br/>
								<h3>Get email html content links</h3>
								This method parses the email html body (if it exists) and returns any links found<br/>
								<br/>
								http://[hostname]/bluebox/jaxrs<%=MessageResource.PATH %>/links/[Uid]
								<br/><br/>
								<pre class="json">
								<code class="json">
{
	"links":[
		{
			"text":"https:\/\/xxx.com...ale=en_US",
			"data":"",
			"href":"https:\/\/xxx.com...ale=en_US"
		},
		{
			"text":"support@xxx.com",
			"data":"",
			"href":"mailto:support@xxx.com"
		},
		{
			"text":"http:\/\/www.xxx.com\/social",
			"data":"",
			"href":"http:\/\/www.xxx.com\/social"
		}
	]
}
								</code>
								</pre>
								<br/>															
						</li>
					</ol>
				<h2>Feeds</h2>
					<ol>
						<li style="list-style-type:none;">Feeds can be passed a "type" parameter to specify the feed protocol</li>
						<li style="list-style-type:none;">Can be one of : rss_0.91N,rss_0.93,rss_0.92,rss_1.0,rss_0.94,rss_2.0,rss_0.91U,rss_0.9,atom_1.0 or atom_0.3</li>
						<li style="list-style-type:none;">e.g. http://&lt;bluebox-server&gt;/bluebox/feed/inbox?type=atom_1.0&amp;email=steve@test.com</li>						
					</ol>
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>