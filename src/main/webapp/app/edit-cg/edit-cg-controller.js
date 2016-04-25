var app = angular.module('home');


app.controller('editCgController', ['$scope', '$http', '$modal', '$modalInstance','vmStructureService', function ($scope, $http, $modal, $modalInstance, vmStructureService) {	
	
	$scope.vmGsAndCgFlatData = {};
	$scope.protectedSelectedIndex = -1;
	
	
	$scope.initData = function(){
		$scope.vmGsAndCgFlatData = vmStructureService.getCachedVmGsAndCgFlatData();
		$scope.protectedSelectedIndex = vmStructureService.getProtectedSelectedIndex();
		$scope.cgVms = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].vms;
		
		$scope.vmStructureData = vmStructureService.getCachedVmStructureData();
		
		$scope.unprotectedVms = $scope.vmStructureData.unprotectedVms;
		
		$scope.cgName = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].name;
		
		$scope.groupSets = JSON.parse(JSON.stringify($scope.vmStructureData.groupSets));
		
		$scope.selectedGroupSet = new Array();
		var cgParent = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].parent;
		if(cgParent != null){
			for(i = 0; i < $scope.groupSets.length; i++){
				if($scope.groupSets[i].name == cgParent){
					$scope.selectedGroupSet.push($scope.groupSets[i]);
				}
			}
		}
				
		var newGs = {};
		newGs.id = 'newId';
		newGs.name = 'New ...';
		$scope.groupSets.push(newGs);
		
		$scope.cgVmsJoinedCandidates = new Array();
		$scope.selectedVms = new Array();
		
		for(i = 0; i < $scope.cgVms.length; i++){
			var currVm = $scope.cgVms[i];
			var currVmCloned = JSON.parse(JSON.stringify(currVm));
			$scope.selectedVms.push(currVmCloned);			
			$scope.cgVmsJoinedCandidates.push(currVmCloned);
			currVmCloned.sequenceNumber = currVmCloned.sequenceNumber + 1;
		}
		
		for(i = 0; i < $scope.unprotectedVms.length; i++){
			var currVm = $scope.unprotectedVms[i];
			var currVmCloned = JSON.parse(JSON.stringify(currVm));
			$scope.cgVmsJoinedCandidates.push(currVmCloned);
			currVmCloned.sequenceNumber = 3;
		}
		
		$scope.selectedPackage = {};
		for(i = 0; i < $scope.vmStructureData.systemInfo.packages.length; i++){
			var currPackage = $scope.vmStructureData.systemInfo.packages[i];
			if(currPackage.id == $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].packageId){
				$scope.selectedPackage = currPackage;
			}
		}
		
		$scope.sequenceNumbers = [1, 2, 3, 4, 5];
		$scope.enableReplication = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].enableProtection;
		$scope.priceSlider = 0;
		$scope.selectedCopy = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex].replicaClusters[0].groupCopySettings[0];
	};
	   
	$scope.initData();
	
	
	
	$scope.selectGroupSet = function(newValue, oldValue){
		if($scope.selectedGroupSet.length > 0 && $scope.selectedGroupSet[0].id == 'newId'){
			$scope.selectedGroupSet = null;
			$scope.openAlertModal();
		}
	};
	
	
	$scope.editCg = function(){
		var currCg = $scope.vmGsAndCgFlatData[$scope.protectedSelectedIndex];
		
		var currCgCloned = JSON.parse(JSON.stringify(currCg));
		currCgCloned.replicaClusters[0].groupCopySettings[0].snapshots = [];
    	
    	var currCgModified = JSON.parse(JSON.stringify(currCgCloned));
    	currCgModified.name = $scope.cgName;
    	currCgModified.packageId = $scope.selectedPackage.id;
    	currCgModified.packageName = $scope.selectedPackage.name;
    	currCgModified.packageDisplayName = $scope.selectedPackage.displayName;
    	currCgModified.vms = $scope.selectedVms;
    	currCgModified.vms = JSON.parse(JSON.stringify(currCgModified.vms));
    	$scope.decreaseSequenceNumber(currCgModified.vms);
    	currCgModified.enableProtection = $scope.enableReplication;
    	
    	var cgId = currCg.id;
    	var cgChanges = {};
    	cgChanges.originalConsistencyGroup = currCgCloned;
    	cgChanges.currentConsistencyGroup = currCgModified;
    		
    	vmStructureService.editCg(cgId, cgChanges);  
    	/*$modalInstance.dismiss('cancel');*/
	}
	
	
	$scope.cancel = function(){
		$modalInstance.dismiss('cancel');
	}
	
	
	$scope.decreaseSequenceNumber = function(vms){
		for(i = 0; i < vms.length; i++){
			var currVm = vms[i];
			currVm.sequenceNumber = currVm.sequenceNumber - 1;
		}
	}
	
	$scope.openAlertModal = function(){
		var modalInstance = $modal.open({
             templateUrl: 'app/group-set/group-set-modal.html',
             controller:  'groupSetController',
             windowClass: 'bookmarks-modal'
         });
		
		modalInstance.result.then(function(result){
			var newGs = {};
			newGs.id = result;
			newGs.name = result;
			/*$scope.groupSets.push(newGs);*/
			$scope.groupSets.splice(0, 0, newGs);
			/*$scope.selectedGroupSet.pop();*/
			$scope.selectedGroupSet.push(newGs);
			$scope.apply();
		});
	};
	   
    
}]);