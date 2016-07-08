define([], function() {
    'use strict';

    angular.module('app.gbp').controller('EpgSidePanelController', EpgSidePanelController);

    EpgSidePanelController.$inject = ['$scope', 'EndpointsListService'];

    function EpgSidePanelController($scope, EndpointsListService) {
        $scope.endpoints = EndpointsListService.createList();

        function getEndpoints() {
            if($scope.sidePanelObject)
                $scope.endpoints.getByEpg($scope.sidePanelObject, $scope.rootTenant);
        }

        $scope.$watch('sidePanelObject', getEndpoints);
        $scope.$on('endpointChanged', getEndpoints);
    }
});
