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

        $scope.resolvedPolicy = {};

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
                //var contract = ContractService.createObject();
                //contract.get(link['_model']['_data'].contract, link['_model']['_data'].tenant, 'operational');
                //contract.data.parentTenant = link['_model']['_data'].tenant;
                var resolvedContract = $scope.resolvedPolicy[link['_model']['_data'].id];
                $scope.openSidePanel('resolved-policy/contract-sidepanel', resolvedContract, null);
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

                fillResolvedPolicy(rp);
                topoData.links = getContracts(rp);
            });

            $scope.topologyData = topoData;
            $scope.topologyLoaded = true;
        }

        function fillResolvedPolicy(data) {
            if(data['policy-rule-group-with-endpoint-constraints']) {
                processPolicyRuleGroupWithEpConstraints(
                    data['policy-rule-group-with-endpoint-constraints'],
                    data['provider-epg-id'],
                    data['consumer-epg-id']);
            }

        }

        function processPolicyRuleGroupWithEpConstraints(data, providerEpgId, consumerEpgId) {
            data.forEach(function(element) {
                element['policy-rule-group'].forEach(function(el) {
                    var linkId = generateLinkId(el['contract-id'], providerEpgId, consumerEpgId);

                    if(!$scope.resolvedPolicy.hasOwnProperty(linkId)) {
                        $scope.resolvedPolicy[linkId] = {
                            'contract-id': el['contract-id'],
                            'subjects': {},
                        };
                    }

                    if(!$scope.resolvedPolicy[linkId].subjects.hasOwnProperty(el['subject-name'])) {
                        $scope.resolvedPolicy[linkId].subjects[el['subject-name']] = {'resolved-rule': []};
                    }

                    $scope.resolvedPolicy[linkId].subjects[el['subject-name']]['resolved-rule'].push(el['resolved-rule']);
                })
            })
        }

        function generateLinkId(contractId, providerEpgId, consumerEpgId) {
            return contractId + '_' + providerEpgId + '_' + consumerEpgId;
        }

        function createNode(nodeName, tenantId) {
            return {
                'id': nodeName,
                'tenantId' : tenantId,
                'node-id': nodeName,
                'label': nodeName,
            };
        }

        function createLink( source, target, contract, tenant) {
            return {
                'id': generateLinkId(contract, source, target),
                'source': source,
                'target': target,
                'tenant': tenant,
            };
        }

        function getContracts(data) {
            var retVal = [];

            if( data['policy-rule-group-with-endpoint-constraints'] &&
                data['policy-rule-group-with-endpoint-constraints'][0]['policy-rule-group']) {
                data['policy-rule-group-with-endpoint-constraints'][0]['policy-rule-group'].forEach(function(prg) {
                        retVal.push(
                            createLink(
                                data['provider-epg-id'],
                                data['consumer-epg-id'],
                                prg['contract-id'],
                                prg['tenant-id']
                            )
                        )
                    });
            }

            return retVal;
        }
    }

});

