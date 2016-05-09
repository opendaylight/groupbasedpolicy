define([
    'app/gbp/contract/contract.service',
    'app/gbp/contract/contract-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('ContractController', ContractController);

    ContractController.$inject = ['$scope', 'TenantService', 'ContractService', 'ContractListService'];

    function ContractController($scope, TenantService, ContractService, ContractListService) {
        $scope.tenant = TenantService.createObject();
        $scope.tenant.get('tenant1');
        console.log('tenant from CONTRACT CTRL:', $scope.tenant);

        $scope.contract = ContractService.createObject();
        $scope.contract.get('contract1');
        console.log('contract from CONTRACT CTRL', $scope.contract);

        $scope.contracts = ContractListService.createList();
        $scope.contracts.get('config');
        console.log('contracts from CONTRACT CTRL', $scope.contracts);

    }
});
