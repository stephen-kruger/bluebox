<html ng-app="ionicApp">
<head>
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<title>Inbox</title>

<link href="//code.ionicframework.com/nightly/css/ionic.css"
	rel="stylesheet">
<script src="//code.ionicframework.com/nightly/js/ionic.bundle.js"></script>
<script src="inbox.js"></script>
</head>

<body ng-controller="InboxCtrl">

	<ion-header-bar class="bar-positive">
	<h1 class="title">Inbox</h1>
	</ion-header-bar>

	<ion-tabs class="tabs-positive tabs-icon-only"> 
		<ion-tab
			title="Inbox" icon-on="ion-ios-filing"
			icon-off="ion-ios-filing-outline"> 
			<ion-view view-title="Home">
	        <ion-content>
	          <div class="list" ng-if="data" ng-repeat="d in data">
			        <div class="item item-divider item-avatar item-list-detail item-thumbnail-left">                
			                <div class="item-icon-left">
			                    <div><i class="icon ion-email bgskyblue  iconx dashboardsquare"></i></div>
			                </div>
			                <h3 class="spaceWrap"><b>{{d.Subject}}</b></h3>
			                <p class="spaceWrap">
			                    <span class="small"><i class="icon ion-person">&nbsp;</i>{{d.Sender[0]}}</span>
			                </p>
			                <p class="spaceWrap">
			                    <span class="small">{{d.Size}}</span>
			                </p>
			
			           </div>
			    </div>
	        </ion-content>
	      </ion-view>
		</ion-tab> 
	
		<ion-tab title="Trash" icon-on="ion-ios-trash"
			icon-off="ion-ios-trash-outline"> 
			<ion-view view-title="Trash">
	        <ion-content class="padding">
	          <p>
	            <a class="button icon icon-right ion-chevron-right" href="#/tab/facts">Trash</a>
	          </p>
	        </ion-content>
	      </ion-view> 
		</ion-tab>
	</ion-tabs>
</body>
</html>