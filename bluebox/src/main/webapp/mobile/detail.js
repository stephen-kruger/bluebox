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

.controller('DetailCtrl', function($scope,$http) {
  
	$scope.getdetail = function(uid) {
		// state=1 for normal, state=2 for deleted
		console.log('listing detail for '+uid);
		$http.get('../rest/json/inbox/detail/'+uid).success(function(res){
			$scope.detail = res;
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