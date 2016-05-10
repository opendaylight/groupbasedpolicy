define([
    'app/gbp/tenant/tenant.service',
    'app/gbp/tenant/tenant-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('TenantController', TenantController);

    TenantController.$inject = ['$scope', 'TenantService', 'TenantListService'];
    /* @ngInject */
    function TenantController($scope, TenantService, TenantListService) {
        $scope.tenantsTableQuery = {
            order: "data.id",
            limit: 25,
            page: 1,
            options: [25, 50, 100],
            filter: ''
        };

        $scope.tenants = TenantListService.createList();
        $scope.tenants.get('config');
    }
});
