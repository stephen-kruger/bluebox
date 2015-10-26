angular.module('ionicApp', ['ionic'])

.controller('InboxesCtrl', function($scope,$http) {
  
	$scope.search = function(query) {
		console.log('searching for '+query);
		$scope.data.items = [];
		// call the autocomplete service
		$http.get('../rest/json/autocomplete?start=0&count=150&label='+query).success(function(res){
			console.log("Found "+res.items.length+" results");
			  $scope.data.count += res.items.length;
			  for (var i = 0; i < res.items.length; i++) {
				  $scope.data.items.push(res.items[i]);
			  }
			  $scope.$broadcast('scroll.infiniteScrollComplete');
			}).error(function(failure) {
				console.log('Failed');		  
			});;
			
		//$scope.data.items = searchResults;
		return $scope.data.items;
		//$scope.search("ste");
	}
  
  $scope.data = {
		    items: [],
		    query: ''
		  };
      
  $scope.search($scope.data.query);
});