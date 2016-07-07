define([], function() {
    'use strict';

    angular.module('app.gbp').controller('ContractListSidePanelController', ContractListSidePanelController);

    ContractListSidePanelController.$inject = ['$scope'];

    function ContractListSidePanelController($scope) {
        $scope.fadeAll();

        $scope.sidePanelContracts = Object.keys($scope.resolvedPolicy.contracts).map(function (k) {
            var ob = $scope.resolvedPolicy.contracts[k];
            return ob;
        });
        $scope.activeObject = 'contract';
    }
});
