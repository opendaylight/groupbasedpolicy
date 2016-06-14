define([
    'app/gbp/endpoints/endpoint.service',
    'app/gbp/forwarding/forwarding.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddEndpointController', AddEndpointController);

    AddEndpointController.$inject = ['$filter', '$mdDialog', '$scope', 'EndpointService', 'endpoint', 'ForwardingService', 'TenantService'];
    /* @ngInject */
    function AddEndpointController($filter, $mdDialog, $scope, EndpointService, endpoint, ForwardingService, TenantService) {
        /* properties */

        $scope.endpoint = endpoint ? endpoint : EndpointService.createObject();
        $scope.epgsChips = {
            selectedItem: null,
            searchText: null,
        };
        $scope.epgsListOfChoosenTenant = null;
        $scope.forwarding = ForwardingService.createObject();
        $scope.forwardingContexts = [];
        $scope.forwardingNetworkDomainIds = [];

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;
        $scope.filterContextIds = filterContextIds;
        $scope.filterNetworkDomainIds = filterNetworkDomainIds;
        $scope.searchEpgs = searchEpgs;


        $scope.forwarding.get(function () {
            var tenantForwarding = $filter('filter')($scope.forwarding.data, { 'tenant-id': $scope.parentTenant });

            if (tenantForwarding && tenantForwarding.length) {
                $scope.forwarding.data = tenantForwarding[0];
                $scope.filterNetworkDomainIds('l2-l3-forwarding:subnet');
            }

            if ($scope.endpoint && $scope.endpoint.data['context-type']) {
                $scope.filterContextIds($scope.endpoint.data['context-type']);
            }
        });



        populateEpgsListOfChoosenTenant();

        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
            $scope.getEndpointsList();
        }

        function save() {
            if ($scope.endpoint.data['network-containment'] && $scope.endpoint.data['network-containment']['network-domain-id']) {
                $scope.endpoint.data['network-containment']['network-domain-type'] = 'l2-l3-forwarding:subnet';
            }
            else {
                delete $scope.endpoint.data['network-containment'];
            }
            $scope.endpoint.post(function () {
                $scope.closeDialog();
            }, function () {
            } );
        }

        function filterContextIds(contextType) {
            $scope.forwardingContexts =  $filter('filter')($scope.forwarding.data['forwarding-context'], {'context-type': contextType});
        }

        function filterNetworkDomainIds(networkDomainType) {
            $scope.forwardingNetworkDomainIds =  $filter('filter')($scope.forwarding.data['network-domain'], {'network-domain-type': networkDomainType});
            $scope.forwardingNetworkDomainIds.unshift('');
        }

        function populateEpgsListOfChoosenTenant() {
            var tenantsIdsList = $scope.rootTenants.data.map(function (e) { return e.data.id; }),
                indexOfChoosenTenant = tenantsIdsList.indexOf($scope.parentTenant),
                epgsObjectsOfChoosenTenant = $scope.rootTenants.data[indexOfChoosenTenant].data.policy['endpoint-group'];
            $scope.epgsListOfChoosenTenant = epgsObjectsOfChoosenTenant.map(function (a) { return a.id; });
        }

        function searchEpgs(query) {
            var self = this,
                results = query ? self.epgsListOfChoosenTenant.filter(createFilterFor(query) ) : self.epgsListOfChoosenTenant;
            return results;
        }

        function createFilterFor(query) {
            return function filterFn(epg) {
                return (epg.indexOf(query) === 0);
            };
        }


    }
});
