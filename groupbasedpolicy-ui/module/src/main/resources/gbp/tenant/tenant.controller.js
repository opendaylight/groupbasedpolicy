define([
    'app/gbp/tenant/tenant.service',
    'app/gbp/tenant/tenant-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('TenantController', TenantController);

    TenantController.$inject = ['$scope', 'TenantService', 'TenantListService'];

    function TenantController($scope, TenantService, TenantListService) {
        $scope.tenant = TenantService.createObject();
        $scope.tenant.get('newTenant');
        console.log('Tenant', $scope.tenant);

        $scope.tenants = TenantListService.createList();
        $scope.tenants.get('config');

        console.log('Tenants', $scope.tenants);
    }
});
