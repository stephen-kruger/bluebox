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
	<div class="item-icon-left">
		<a href="inboxes.jsp"><i class="icon ion-ios-home bgskyblue  iconx dashboardsquare"></i></a>
	</div>
	<h1 class="title">Inbox for {{Inbox}}</h1>
	</ion-header-bar>

	<ion-tabs class="tabs-positive tabs-icon-only"> 
		<ion-tab
			title="Inbox" icon-on="ion-ios-filing"
			icon-off="ion-ios-filing-outline"> 
			<ion-view view-title="Home">
	        <ion-content>
	         <ion-list>
		      	<ion-item collection-repeat="d in data" item="item" href="detail.jsp?Email={{d.Sender[0]}}&Uid={{d.Uid}}" class="item-remove-animate">
		      	<h3 class="spaceWrap"><b>{{d.Subject}}</b></h3>
		      	<p class="spaceWrap">
			                    <span class="small"><i class="icon ion-person">&nbsp;</i>{{d.Sender[0]}}</span>
			                </p>
			                <p class="spaceWrap">
			                    <span class="small">{{d.Size}}</span>
			                </p>
		      	</ion-item>
		      	
				<!-- <ion-infinite-scroll on-infinite="addItems('')"></ion-infinite-scroll> -->
		      </ion-list>
      <!-- 
	          <div class="list" ng-if="data" ng-repeat="d in data" >
			        <div class="item item-divider item-avatar item-list-detail item-thumbnail-left" href="detail.jsp?Email={{d.Inbox}}&State=1">                
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
			     -->
	        </ion-content>
	      </ion-view>
		</ion-tab> 
	
		<ion-tab title="Trash" icon-on="ion-ios-trash"
			icon-off="ion-ios-trash-outline"> 
			<ion-view view-title="Trash">
	        <ion-content>
	          <div class="list" ng-if="data" ng-repeat="d in deleted" href="detail.jsp?Email={{d.Inbox}}&State=1">
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
	</ion-tabs>
</body>
</html>