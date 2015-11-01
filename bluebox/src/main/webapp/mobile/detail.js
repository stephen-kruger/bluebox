function getParameter(name,defaultValue) {
	if (!name)
		return defaultValue;
	 var url = location.href;
	if (!url)
		return defaultValue;
	  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
	  var regexS = "[\\?&]"+name+"=([^&#]*)";
	  var regex = new RegExp( regexS );
	  var results = regex.exec( url );
	  return decodeURI(results == null ? defaultValue : results[1]);
}
angular.module('ionicApp', ['ionic'])

.controller('DetailCtrl', function($scope,$http,$sce,$ionicTabsDelegate) {
  
	$scope.to_trusted = function(html_code) {
	    return $sce.trustAsHtml(html_code);
	}
	
	$scope.getdetail = function(uid) {
		// state=1 for normal, state=2 for deleted
		console.log('listing detail for '+uid);
		$http.get('../rest/json/inbox/detail/'+uid).success(function(res){
			$scope.detail = res;
			var length = 0;
			if ($scope.detail.Attachment) {
				length = $scope.detail.Attachment;
			}
			$scope.detail.AttachmentBlob = [length];
			// fix up the attachment links
			for (var i = 0; i < length; i++) {
				var blob = {
						href : "../rest/json/inbox/attachment/"+getParameter('Uid')+"/"+i+"/"+$scope.detail.Attachment[i],
						name : $scope.detail.Attachment[i]
				};
				$scope.detail.AttachmentBlob[i] = blob;
				console.log($scope.detail.AttachmentBlob[i]);
			}
			// now select html or text tab
			if (res.HtmlBody.length>0)
				$ionicTabsDelegate .select(1, false);
			}).error(function(failure) {
				console.log('Failed');		  
			});
			
		//$scope.data.items = searchResults;
		return $scope.detail;
	}
	
	$scope.detail = {};
	$scope.Inbox = getParameter('Email','Error');		
	$scope.data = $scope.getdetail(getParameter('Uid','*'));
});