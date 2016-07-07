define([
    'app/gbp/endpoints/endpoint.service',
    'app/gbp/forwarding/forwarding.service',
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddEndpointController', AddEndpointController);

    AddEndpointController.$inject = ['$state', '$filter', '$mdDialog', '$scope', 'EndpointService', 'endpoint', 'ForwardingService'];
    /* @ngInject */
    function AddEndpointController($state, $filter, $mdDialog, $scope, EndpointService, endpoint, ForwardingService) {
        /* properties */

        $scope.endpoint = endpoint ? endpoint : EndpointService.createObject();
        if (!$scope.endpoint.data.tenant) {
            $scope.endpoint.data.tenant = $scope.rootTenant ? $scope.rootTenant : null;
        }
        $scope.epgsChips = {
            selectedItem: null,
            searchText: null,
        };
        $scope.epgsListOfChoosenTenant = [];
        $scope.forwarding = ForwardingService.createObject();
        $scope.forwardingContexts = [];
        $scope.forwardingNetworkDomainIds = [];
        $scope.regexps = {
            'ipv4cidr': '(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))',
            'ipv6cidr': 's*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*(\/([1-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8]))',
            'mac-address': '([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})',
        };

        $scope.regexps['ip-prefix'] = '(('+$scope.regexps.ipv4cidr + ')|(' + $scope.regexps.ipv6cidr + '))';

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;
        $scope.filterContextIds = filterContextIds;
        $scope.filterNetworkDomainIds = filterNetworkDomainIds;
        $scope.searchEpgs = searchEpgs;
        $scope.populateScopeAfterTenantSelected = populateScopeAfterTenantSelected;

        populateScopeAfterTenantSelected();

        /* Implementations */
        $scope.forwarding.get(postForwardingGet);

        function closeDialog(){
            $mdDialog.cancel();
            if($state.current.name == 'main.gbp.index.endpoints')
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
                $scope.broadcastFromRoot('endpointChanged');
            }, function () {
            } );
        }

        function filterContextIds(contextType) {
            $scope.forwardingContexts =  $filter('filter')($scope.forwarding.data['forwarding-context'], {'context-type': contextType});
        }

        function filterNetworkDomainIds(networkDomainType) {
            $scope.forwardingNetworkDomainIds =  $filter('filter')($scope.forwarding.data['network-domain'], {'network-domain-type': networkDomainType});
        }

        function populateEpgsListOfChoosenTenant() {
            $scope.rootTenants.data.some(function (tenant) {
                if (tenant.data.id === $scope.endpoint.data.tenant) {
                    $scope.epgsListOfChoosenTenant = tenant.data.policy['endpoint-group'].map(function (ele) { return ele.id; } );
                }
            });
        }

        function searchEpgs(query) {
            var self = this,
                results = query ? self.epgsListOfChoosenTenant.filter(createFilterFor(query) ) : self.epgsListOfChoosenTenant;
            return results;
        }

        function createFilterFor(query) {
            return function filterFn(epg) {
                return (epg.toLowerCase().indexOf(query.toLowerCase()) === 0);
            };
        }

        function postForwardingGet() {
            var tenantForwarding = $filter('filter')($scope.forwarding.data, { 'tenant-id': $scope.endpoint.data.tenant });

            if (tenantForwarding && tenantForwarding.length) {
                $scope.forwarding.data = tenantForwarding[0];
                $scope.filterNetworkDomainIds('l2-l3-forwarding:subnet');
            }

            if ($scope.endpoint && $scope.endpoint.data['context-type']) {
                $scope.filterContextIds($scope.endpoint.data['context-type']);
            }
        }

        function populateScopeAfterTenantSelected() {
            populateEpgsListOfChoosenTenant();
        }

    }
});
