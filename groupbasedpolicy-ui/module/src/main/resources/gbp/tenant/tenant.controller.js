define([
    'app/gbp/tenant/tenant.service',
    'app/gbp/tenant/tenant-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('TenantController', TenantController);

    TenantController.$inject = ['$mdDialog', '$scope', 'TenantListService'];
    /* @ngInject */
    function TenantController($mdDialog, $scope, TenantListService) {
        /* properties */
        $scope.tenants = TenantListService.createList();
        $scope.tenantsTableQuery = {};

        /* methods */
        $scope.getTenantList = getTenantList;
        $scope.openTenantDialog = openTenantDialog;
        $scope.deleteTenantDialog = deleteTenantDialog;

        init();

        /* Implementations */

        /**
         * fills $scope.tenants array with data from data store
         */
        function getTenantList() {
            $scope.tenants = TenantListService.createList();
            $scope.tenants.get('config');
        }

        /**
         * Initializing function
         */
        function init() {
            $scope.tenantsTableQuery = {
                order: "data.id",
                limit: 25,
                page: 1,
                options: [25, 50, 100],
                filter: ''
            };

            getTenantList();
        }

        function openTenantDialog(tenantData) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'AddTenantController',
                preserveScope: true,
                templateUrl: 'src/app/gbp/tenant/dialog-add-tenant.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    tenant: tenantData
                }
            });
        }

        function deleteTenantDialog(tenantData) {
            var confirm = $mdDialog.confirm()
                .title('Delete tenant')
                .textContent('Do you want to delete tenant ' + tenantData.data.name + '?')
                .ok('Delete')
                .cancel('Cancel');

            $mdDialog.show(confirm).then(function() {
                tenantData.deleteTenant(
                    function() {
                        $scope.getTenantList();
                    }
                );
            }, function() {

            });
        }

    }
});
