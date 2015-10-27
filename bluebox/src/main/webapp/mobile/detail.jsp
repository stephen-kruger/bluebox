<html ng-app="ionicApp">
<head>
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<title>Detail</title>

<link href="//code.ionicframework.com/nightly/css/ionic.css"
	rel="stylesheet">
<script src="//code.ionicframework.com/nightly/js/ionic.bundle.js"></script>
<script src="detail.js"></script>
</head>

<body ng-controller="DetailCtrl">

	<ion-nav-bar class="bar-positive">
	
	  <ion-nav-buttons side="left">
	    <button class="button button-icon ion-ios-arrow-back" onclick="history.go(-1);"> {{Inbox}}</button>
	  </ion-nav-buttons>
	  <ion-nav-buttons side="right">
	    <button class="button button-icon ion-trash-a"></button>
	    <button class="button button-icon ion-home" onclick="window.location='./inboxes.jsp'"></button>
	  </ion-nav-buttons>
	</ion-nav-bar>
	<ion-header-bar class="bar-balanced bar-subheader">
		<h2 class="title">{{detail.Subject}}</h2>
		From <b><span ng-repeat="sender in detail.Sender">{{ sender }}</span></b><br/>
		<span>Received <b>{{ detail.Received }}</b></span>
	</ion-header-bar>
	
	<ion-tabs class="tabs-positive tabs-icon-only"> 
	
		<ion-tab
			title="Text" icon-on="ion-ios-paper"
			icon-off="ion-ios-paper-outline"> 
			<ion-view view-title="Text">
	        <ion-content>
	          {{detail.TextBody}}
	        </ion-content>
	      </ion-view>
		</ion-tab> 
	
		<ion-tab title="Html" icon-on="ion-ios-world"
			icon-off="ion-ios-world-outline"> 
			<ion-view view-title="Html">
	        <ion-content>
	        <div ng-bind-html="to_trusted(detail.HtmlBody)"></div>
	          <!-- {{detail.HtmlBody}} -->
	        </ion-content>
	      </ion-view> 
		</ion-tab>
		
		<ion-tab title="Attachments" icon-on="ion-social-buffer"
			icon-off="ion-social-buffer-outline"> 
			<ion-view view-title="Attachments">
	        <ion-content>
	        	<h3>Attachments</h3>
	          <p ng-repeat="attachment in detail.Attachment">
	          	<a class="button button-icon ion-android-attach" 
	          			href="../rest/json/inbox/attachment/3887c26f-7f32-4557-8007-dc383149005a/0/{{attachment}}"> {{ attachment }}
	          	</a>
	          </p>
	        </ion-content>
	      </ion-view> 
		</ion-tab>
		
	</ion-tabs>
	
</body>
</html>