define([
    'app/gbp/contract/contract.service',
    'app/gbp/contract/contract-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('ContractController', ContractController);

    ContractController.$inject = ['$scope', 'TenantListService', 'TenantService', 'ContractService', 'ContractListService', '$stateParams'];

    function ContractController($scope, TenantListService, TenantService, ContractService, ContractListService, $stateParams) {
        $scope.contracts = ContractListService.createList();
        $scope.contractsTableQuery = {
            order: 'data.id',
            limit: 25,
            page: 1,
            options: [25, 50, 100],
            filter: '',
        };

        getContracts();

        /* if ($stateParams.contractId) {
            $scope.contractId = $stateParams.contractId;
            console.log('contract.ctrl.if.$scope.contractId', $scope.contractId);
            $scope.contract.get($scope.contractId);
        }
        else {
            console.log('contract.ctrl.else.$scope.contractId', $scope.contractId);
            $scope.contract.get($scope.$parent.tenantId);
        }*/

        function getContracts() {
            $scope.contracts.data = [];
            $scope.contracts.get($scope.rootTenant.data.id);
        }


        $scope.$on('ROOT_TENANT_CHANGED', getContracts);
    }
});
