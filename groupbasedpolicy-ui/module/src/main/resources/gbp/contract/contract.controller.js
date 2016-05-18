define([
    'app/gbp/contract/contract.service',
    'app/gbp/contract/contract-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('ContractController', ContractController);

    ContractController.$inject = ['$mdDialog', '$scope', 'TenantListService', 'TenantService', 'ContractService', 'ContractListService', '$stateParams'];

    function ContractController($mdDialog, $scope, TenantListService, TenantService, ContractService, ContractListService, $stateParams) {
        $scope.contracts = ContractListService.createList();
        $scope.openContractDialog = openContractDialog;
        $scope.contractsTableQuery = {
            order: 'data.id',
            limit: 25,
            page: 1,
            options: [25, 50, 100],
            filter: '',
        };
        $scope.getContractList = getContractList;
        $scope.deleteContractDialog = deleteContractDialog;

        getContractList();

        function getContractList() {
            $scope.contracts.clearData();
            $scope.contracts.get($scope.rootTenant.data.id);
        }

        function openContractDialog(contractData) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'AddContractController',
                preserveScope: true,
                templateUrl: 'src/app/gbp/contract/dialog-add-contract.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    contract: contractData,
                },
            });
        }

        function deleteContractDialog(contractData) {
            var confirm = $mdDialog.confirm()
                .title('Delete contract')
                .textContent('Do you want to delete contract ' + contractData.data.id + '?')
                .ok('Delete')
                .cancel('Cancel');

            $mdDialog.show(confirm).then(function () {
                contractData.deleteContract($scope.rootTenant.data.id,
                    function () {
                        $scope.getContractList();
                    }
                );
            }, function () {

            });
        }

        $scope.$on('ROOT_TENANT_CHANGED', getContractList);
    }
});
