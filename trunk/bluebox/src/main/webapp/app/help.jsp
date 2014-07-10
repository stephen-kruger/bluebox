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
	<title>Help</title>
	<jsp:include page="dojo.jsp" />	
	<script>
		require(["dojo/domReady!"], function(){
			selectMenu("help");
		});
	</script>
</head>
<body class="<%=bbconfig.getString("dojo_style")%>">
	<div class="headerCol"><jsp:include page="menu.jsp" /></div>
	<div class="colWrapper">		
		<div class="leftCol">
			<h1>Help</h1>
		</div>
			
		<div class="centerCol">
			<div style="text-align:left;">
					<h2>Contact and support links</h2>
					<ol>
						<li style="list-style-type:none;">For feature requests, info etc contact me directly via the <a href="https://code.google.com/p/bluebox/">Bluebox Project</a> site</li>
						<li style="list-style-type:none;">To report rendering defects, bugs etc, please use the <a href="https://code.google.com/p/bluebox/issues/list">defect tracking</a> system</li>
						<li style="list-style-type:none;">Please try and attach the offending .eml file whenever possible to help in reproducing</li>
					</ol>
					<h2>What is it?</h2>
					<ol>
						<li style="list-style-type:none;">Many teams within our company develop software that rely heavily on sending emails (for sign-up, social software notifications etc).</li>
						<li style="list-style-type:none;">Properly testing this software requires multiple "fake" email addesses to allow testing of the capabili style="list-style-type:none;"ties of the software from various points of view and roles</li>
						<li style="list-style-type:none;">Traditionally we either used Google Mail (which allowed random prefixes to be added to email addresses), or other services such as <a href="http://mailinator.com">Mailinator</a></li>
						<li style="list-style-type:none;">However sending such emails from unreleased proucts, potentially containing trade secrets, non-public product information and other private information to the public internet is not feasible, and an internal service was needed.</li>
					</ol>
					<h2>How do I use it?</h2>
					<ol>
						<li style="list-style-type:none;">The easiest way : simply send an email to any address you like using the <b>@bluebox.xxx.com domain</b></li>
						<li style="list-style-type:none;">For example send a test mail using your Notes client to <a href="mailto:test@bluebox.xxx.com">test@bluebox.xxx.com</a> or <a href="mailto:fake.mail@bluebox.xxx.com">fake.mail@bluebox.xxx.com</a></li>
						<li style="list-style-type:none;">Then enter the email address into the <a href="index.jsp">form</a> here at BlueBox to show your inbox</li>
						<li style="list-style-type:none;">Another way : set your SMTP server to <b>bluebox.xxx.com</b>, you can send emails to any address and any domain (although they will all be delivered and available only on bluebox.xxx.com)</li>
					</ol>
					<h2>How long will my email be available?</h2>
					<ol>
						<li style="list-style-type:none;">Messages will be automatically deleted after <%= Config.getInstance().getString(Config.BLUEBOX_MESSAGE_AGE) %> hours</li>
						<li style="list-style-type:none;">Mails in the Trash folder (i.e. explicitly deleted by the user)will be removed after <%= Config.getInstance().getString(Config.BLUEBOX_TRASH_AGE) %> hours</li>
						<li style="list-style-type:none;">If you require it for longer, simply use the Download button to save a copy to disk, or attach to a defect report</li>
						<li style="list-style-type:none;">You can then use the Upload functionality to re-inject it into the system as many times as you like</li>
					</ol>
					<h2>SMTP Open Relay Test scans are complaining Bluebox is an open relay</h2>
					<ol>
						<li style="list-style-type:none;">These scans generally test if an SMTP server accepts email from arbitrary domains.</li>
						<li style="list-style-type:none;">By design, Bluebox IS very forgiving, and will accept any messages unless otherwise configured via blacklist or whitelist</li>
						<li style="list-style-type:none;">However no mails, regardless of recipient, are EVER forwarded on to other systems. So any scan which implies Bluebox is an open relay is not correctly implemented, 
						as they would need to validate that a test email is actually delivered to a different MX domain - but generally these tests are lazily written, and only go as far
						as testing that the SMTP server performs the "accept" handshake.</li>		
						<li style="list-style-type:none;">It is possible to defend against some of these poorly written relay scans by intelligently configuring the bluebox_to_blacklist or bluebox_from_blacklist in bluebox.properties:
						Run the offending scan program, and check the app server logs to detect what email is being used to run the test (e.g. test@microsoft.com), 
						and then add that domain to the bluebox_to_blacklist, which will cause rejection of any messages from that domain, hopefully 
						fooling the simple-minded Open Relay scan.</li>
					</ol>
					<h2>Can I send mails to other domains?</h2>
					<ol>
						<li style="list-style-type:none;">BlueBox itself does not do any mail forwarding, only incoming mail is accepted</li>
						<li style="list-style-type:none;">It's not very scrupulous about destination addresses, so if you have set bluebox as your smtp server, you can send to any hostnames that you like. Such as stephen@xxx.com, steve@google.com, or any other domain you might want</li>		
					</ol>
					<h2>Installation</h2>
					<ol>
						<li style="list-style-type:none;">Full install instructions can be found <a href="https://code.google.com/p/bluebox/">here</a></li>
					</ol>
					<h2>Terms and conditions of usage</h2>
					<ol>
						<li style="list-style-type:none;">Please respect others who may be using the system - don't delete mails which are not yours</li>		
						<li style="list-style-type:none;">This software is provided as is. Availability and support is limited to the goodwill of the author, or patches you may wish to submit</li>
					</ol>
			</div>
		</div>
			
		<div class="rightCol">
			<jsp:include page="stats.jsp" />
		</div>
	</div>
</body>
</html>