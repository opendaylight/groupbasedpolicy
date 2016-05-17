define([
    'app/gbp/tenant/tenant.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddTenantController', AddTenantController);

    AddTenantController.$inject = ['$mdDialog', '$scope', 'TenantService', 'tenant'];
    /* @ngInject */
    function AddTenantController($mdDialog, $scope, TenantService, tenant) {
        /* properties */
        $scope.tenant = tenant ? tenant : TenantService.createObject();

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;
        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
            $scope.getTenantList();
        }

        function save() {
            $scope.tenant.put(function(data) {
                $scope.closeDialog();
            }, function(err) {
            } );
        }

    }
});
