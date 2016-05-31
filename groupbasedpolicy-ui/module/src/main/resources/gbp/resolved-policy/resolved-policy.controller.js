define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('ResolvedPolicyController', ResolvedPolicyController);

    ResolvedPolicyController.$inject = ['$rootScope', '$scope', 'ResolvedPolicyService', 'EpgService', 'ContractService'];

    /* @ngInject */
    function ResolvedPolicyController($rootScope, $scope, ResolvedPolicyService, EpgService, ContractService) {
        $scope.topologyData = {
            nodes: [],
            links: [],
        };

        $scope.cbkFunctions = {
            clickNode: function(node){
                var epg = EpgService.createObject();
                epg.get(node['_data-id'], node['_model']['_data']['tenantId'], 'operational');
                epg.data.parentTenant = node['_model']['_data']['tenantId'];
                $scope.openSidePanel('resolved-policy/epg-sidepanel', epg, null);
                $scope.$apply();

                // //Example of highlighting
                // NextTopologyService.highlightNode($scope.nxTopology, 1);
                // NextTopologyService.highlightNode($scope.nxTopology, 1, true); //without links around
                // NextTopologyService.highlightLink($scope.nxTopology, '1-7');
                // NextTopologyService.highlightPath($scope.nxTopology, [array of links obj]);

                // //Fade out or in whole topology
                // NextTopologyService.fadeOutAllLayers();
                // NextTopologyService.fadeInAllLayers();
            },
            clickLink: function(link){
                var contract = ContractService.createObject();
                contract.get(link['_data-id'], link['_model']['_data']['tenantId'], 'operational');
                contract.data.parentTenant = link['_model']['_data']['tenantId'];
                $scope.openSidePanel('resolved-policy/contract-sidepanel', contract, null);
                $scope.$apply();
            },
            // topologyGenerated: function(){

            //     if ( $rootScope.updateTopoInterval ) {
            //         clearInterval($rootScope.updateTopoInterval);
            //     }

            //     $rootScope.updateTopoInterval = setInterval(function () {
            //         fillTopologyData();
            //     },5000);

            // },
        };

        var resolvedPolicies = ResolvedPolicyService.createObject();
        resolvedPolicies.get(function () {
            fillTopologyData();
        });

        function fillTopologyData() {
            var topoData = {nodes: [], links: [],};
            resolvedPolicies.data.forEach(function(rp) {
                topoData.nodes.push(createNode(rp['consumer-epg-id'], rp['consumer-tenant-id']));
                topoData.nodes.push(createNode(rp['provider-epg-id'], rp['provider-tenant-id']));
                // topoData.links.push(createLink(rp['policy-rule-group-with-endpoint-constraints'][0]['policy-rule-group'][0]['contract-id'], rp['consumer-epg-id'], rp['provider-epg-id']));
            });

            $scope.topologyData = topoData;
            $scope.topologyLoaded = true;
        }

        function createNode(nodeName, tenantId) {
            return {
                'id': nodeName,
                'tenantId' : tenantId,
                'node-id': nodeName,
                'label': nodeName,
            };
        }

        function createLink(linkName, source, target) {
            return {
                'id' : linkName,
                'source' : source,
                'target' : target,
            };
        }
    }

});

