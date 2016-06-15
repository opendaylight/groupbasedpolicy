define([
    'app/gbp/endpoints/endpoint.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddEndpointController', AddEndpointController);

    AddEndpointController.$inject = ['$mdDialog', '$scope', 'EndpointService', 'endpoint'];
    /* @ngInject */
    function AddEndpointController($mdDialog, $scope, EndpointService, endpoint) {
        /* properties */
        $scope.endpoint = endpoint ? endpoint : EndpointService.createObject();

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;
        $scope.checkEndpointGroup = checkEndpointGroup;
        $scope.checkEndpointCondition = checkEndpointCondition;

        console.log('$scope.rootTenants.data.Tenant', $scope.rootTenants.data.Tenant);
        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
            $scope.getEndpointsList();
        }

        function save() {
            $scope.endpoint.post(function () {
                $scope.closeDialog();
            }, function () {
            } );
        }

        function checkEndpointGroup(){
            // $scope.bgpRouteForm.nextHopVal.invalidHops = $scope.bgpRoute.getInvalidHops();
        }

        function checkEndpointCondition(){
        }
    }
});
