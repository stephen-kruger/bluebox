angular.module('ionicApp', ['ionic'])

.controller('InboxCtrl', function($scope,$http) {
  
	//http://localhost:8080/bluebox/rest/json/inbox/Jake%20Johnson%20%3Cjake.johnson@tulip.com%3E/1/
	$scope.listmail = function(email,state) {
		// state=1 for normal, state=2 for deleted
		console.log('listing inbox for '+email);
		$http.get('../rest/json/inbox/'+email+'/'+state+'/').success(function(res){
			console.log("Found "+res.length+" mails");
			  $scope.data = res;
			  $scope.$broadcast('scroll.infiniteScrollComplete');
			}).error(function(failure) {
				console.log('Failed');		  
			});;
			
		//$scope.data.items = searchResults;
		return $scope.data;
		//$scope.search("ste");
	}
	
	  $scope.data = [];
		
	  $scope.listmail('jake.johnson@tulip.com',1);
});