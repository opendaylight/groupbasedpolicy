define([], function() {
    'use strict';

    angular.module('app.gbp').controller('EpgListSidePanelController', EpgListSidePanelController);

    EpgListSidePanelController.$inject = ['$scope'];

    function EpgListSidePanelController($scope) {
        $scope.fadeAll();

        $scope.sidePanelEpgs = Object.keys($scope.resolvedPolicy.epgs).map(function (k) {
            var ob = $scope.resolvedPolicy.epgs[k];
            ob.id = k;

            return ob;
        });
        $scope.activeObject = 'epg';
    }
});