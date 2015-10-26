function getParameter(name,defaultValue) {
	if (!name)
		return '*';
	 var url = location.href;
	if (!url)
		return '*';
	  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
	  var regexS = "[\\?&]"+name+"=([^&#]*)";
	  var regex = new RegExp( regexS );
	  var results = regex.exec( url );
	  return results == null ? defaultValue : results[1];
}
angular.module('ionicApp', ['ionic'])

.controller('InboxCtrl', function($scope,$http) {
  
	$scope.listmail = function(email) {
		// state=1 for normal, state=2 for deleted
		console.log('listing inbox for '+email+' state=1');
		$http.get('../rest/json/inbox/'+email+'/1/').success(function(res){
			console.log("Found "+res.length+" mails");
			$scope.data = res;
			}).error(function(failure) {
				console.log('Failed');		  
			});
			
		//$scope.data.items = searchResults;
		return $scope.data;
	}
	
	$scope.listtrash = function(email) {
		// state=1 for normal, state=2 for deleted
		console.log('listing inbox for '+email+' state=2');
		$http.get('../rest/json/inbox/'+email+'/2/').success(function(res){
			console.log("Found "+res.length+" deleted mails");
			$scope.deleted = res;
			}).error(function(failure) {
				console.log('Failed');		  
			});
			
		//$scope.data.items = searchResults;
		return $scope.deleted;
	}
	
	$scope.data = [];
	$scope.deleted = [];
		
	  $scope.data = $scope.listmail(getParameter('Email','*'));
	  $scope.deleted = $scope.listtrash(getParameter('Email','*'));
});