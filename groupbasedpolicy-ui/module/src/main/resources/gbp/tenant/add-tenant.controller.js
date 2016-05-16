define([
    'app/gbp/tenant/tenant.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddTenantController', AddTenantController);

    AddTenantController.$inject = ['$mdDialog', '$scope', 'TenantService'];
    /* @ngInject */
    function AddTenantController($mdDialog, $scope, TenantService) {
        /* properties */
        $scope.tenant = TenantService.createObject();

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;

        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
        }

        function save() {
            $scope.tenant.put(function(data) {
                $scope.closeDialog();
            }, function(err) {
            } );
        }
    }
});
