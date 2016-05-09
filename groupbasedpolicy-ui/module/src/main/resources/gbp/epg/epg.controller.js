define([
    'app/gbp/epg/epg.service',
    'app/gbp/epg/epg-list.service',
], function() {
    'use strict';

    angular.module('app.gbp').controller('EpgController', EpgController);

    EpgController.$inject = ['$scope', 'EpgService', 'EpgListService'];

    function EpgController($scope, EpgService, EpgListService) {
        $scope.epg = EpgService.createObject();
        $scope.epg.get('epg_test', 'tenant_test');
        console.log('Epg', $scope.epg);

        $scope.epgs = EpgListService.createList();
        $scope.epgs.get('config', 'tenant2');

        console.log('Epgs', $scope.epgs);
    }
});