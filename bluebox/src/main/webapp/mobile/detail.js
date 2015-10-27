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

.controller('DetailCtrl', function($scope,$http) {
  
	$scope.getdetail = function(uid) {
		// state=1 for normal, state=2 for deleted
		console.log('listing detail for '+uid);
		$http.get('../rest/json/inbox/detail/'+uid).success(function(res){
			$scope.detail = res;
			console.log($scope.detail.Subject);
			}).error(function(failure) {
				console.log('Failed');		  
			});
			
		//$scope.data.items = searchResults;
		return $scope.detail;
	}
	
	$scope.detail = {};
		
	$scope.data = $scope.getdetail(getParameter('Uid','*'));
});