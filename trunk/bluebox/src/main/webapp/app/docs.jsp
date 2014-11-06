<%@ page language="java" pageEncoding="utf-8" contentType="text/html;charset=utf-8"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="com.bluebox.Config"%>
<%@ page import="com.bluebox.rest.json.JSONInboxHandler"%>
<%@ page import="com.bluebox.rest.json.JSONMessageHandler"%>
<%@ page import="com.bluebox.rest.json.JSONMessageUtilHandler"%>
<%
	Config bbconfig = Config.getInstance();
%>

<!DOCTYPE html>
<html lang="en-US">
<head>
	<title>Documentation</title>
	<jsp:include page="dojo.jsp" />	
	<script>
		require(["dojo/domReady!"], function(){
			selectMenu("docs");
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
					<ol>
						<li style="list-style-type:none;">Download the web application archive from <a href="https://drive.google.com/folderview?id=0B_idaoMmuNnyYW9FS1paTEhuSXc&usp=sharing=sharing">here</a> and drop into your application container</li>
						<li style="list-style-type:none;">Optionally, install and start <a href="http://www.mongodb.org/">MongoDB</a>. Leaving out this step defaults to instanciating a <a href="http://db.apache.org/derby/">Derby</a> embedded instance.</li>
						<li style="list-style-type:none;">Set up an IP table mapping to route SMTP port requests to the web app :<i>iptables -t nat -A PREROUTING -i eth0 -p tcp -m tcp --dport 25 -j REDIRECT --to-ports 2500</i></li>
					</ol>
					<h2>Upgrading</h2>
					<ol>
						<li style="list-style-type:none;">From the administration page, backup the mail content</li>
						<li style="list-style-type:none;">Download the web application archive from <a href="https://drive.google.com/folderview?id=0B_idaoMmuNnyYW9FS1paTEhuSXc&usp=sharing=sharing">here</a> and drop into your application container</li>
						<li style="list-style-type:none;">Restart the application server</li>
						<li style="list-style-type:none;">From the administration page, rebuild search indexes</li>
						<li style="list-style-type:none;">From the administration page, restore the mail content</li>
					</ol>
					<h2>REST API</h2>
					<ol>
						<li style="list-style-type:none;">For direct integration with test automation, the following REST calls may be used to reduce dependancy on UI
								<h3>List user emails</h3>
								This method lists all the emails for a particular email address, of the specified state<br/>
								<br/>
								http://[hostname]/bluebox/<%=JSONInboxHandler.JSON_ROOT %>/[email]/[0=ANY, 1=NORMAL, 2=DELETED]/
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
								http://[hostname]/bluebox/<%=JSONMessageHandler.JSON_ROOT %>/[Uid]
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
								http://[hostname]/bluebox/<%=JSONMessageUtilHandler.JSON_ROOT %>/[Uid]/links
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
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>