define([
    'app/gbp/contract/contract.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddContractController', AddContractController);

    AddContractController.$inject = ['$mdDialog', '$scope', 'ContractService', 'contract'];
    /* @ngInject */
    function AddContractController($mdDialog, $scope, ContractService, contract) {
        /* properties */
        $scope.contract = contract ? contract : ContractService.createObject();

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;
        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
            $scope.getContractList();
        }

        function save() {
            $scope.contract.put($scope.rootTenant.data.id, function (data) {
                $scope.closeDialog();
            }, function (err) {
            } );
        }

    }
});
