define([
    'app/gbp/endpoints/endpoint.service',
    'app/gbp/endpoints/endpoints-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('EndpointsController', EndpointsController);

    EndpointsController.$inject = ['$scope', '$mdDialog', 'EndpointsListService', 'EndpointService'];

    function EndpointsController($scope, $mdDialog, EndpointsListService, EndpointService) {
        $scope.endpoints = EndpointsListService.createList();
        $scope.openEndpointDialog = openEndpointDialog;
        $scope.getEndpointsList = getEndpointsList;
        $scope.deleteEndpointDialog = deleteEndpointDialog;
        $scope.endpointsTableQuery = {
            order: "data['context-id']",
            limit: 25,
            page: 1,
            options: [25, 50, 100],
            filter: '',
        };

        getEndpointsList();

        function getEndpointsList() {
            $scope.endpoints.clearData();
            $scope.endpoints.get();
        }

        function openEndpointDialog(endpointData) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'AddEndpointController',
                preserveScope: true,
                templateUrl: $scope.viewPath + 'endpoints/dialog-add-endpoint.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    endpoint: endpointData,
                },
            });
        }

        function deleteEndpointDialog(endpointData) {
            var confirm = $mdDialog.confirm()
                .title('Delete endpoint')
                .textContent('Do you want to delete endpoint?')
                .ok('Delete')
                .cancel('Cancel');

            $mdDialog.show(confirm).then(function () {
                contractData.deleteEndpoint($scope.rootTenant.data.id,
                    function () {
                        $scope.getEndpointsList();
                    }
                );
            }, function () {

            });
        }
    }
});
