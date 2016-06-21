define([
    'app/gbp/endpoints/endpoint.service',
    'app/gbp/endpoints/endpoints-list.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('EndpointsController', EndpointsController);

    EndpointsController.$inject = ['$scope', '$mdDialog', 'EndpointsListService', 'EndpointService'];

    function EndpointsController($scope, $mdDialog, EndpointsListService, EndpointService) {
        /* properties */
        $scope.endpoints = EndpointsListService.createList();
        $scope.disableKeyFieldsEditing = false;
        $scope.endpointsTableQuery = {
            order: "data['context-id']",
            limit: 25,
            page: 1,
            options: [25, 50, 100],
            filter: '',
        };
        /* methods */
        $scope.openEndpointDialog = openEndpointDialog;
        $scope.getEndpointsList = getEndpointsList;
        $scope.deleteEndpointDialog = deleteEndpointDialog;

        getEndpointsList();

        function getEndpointsList() {
            $scope.endpoints.clearData();
            $scope.endpoints.get();
        }

        function openEndpointDialog(operation, endpointData) {
            $scope.disableKeyFieldsEditing = operation === 'edit';
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
                endpointData.deleteEndpoint(function () {
                    $scope.getEndpointsList();
                });
            }, function () {

            });
        }
    }
});
