define(['app/gbp/common/gbp.service', 'app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$state', '$rootScope', '$scope', '$filter', 'RootGbpService', 'TenantListService', 'TenantService', 'EpgListService', 'ResolvedPolicyService', 'NextTopologyService', 'EndpointsListService'];

    function RootGbpCtrl($state, $rootScope, $scope, $filter, RootGbpService, TenantListService, TenantService, EpgListService, ResolvedPolicyService, NextTopologyService, EndpointsListService) {
        /* properties */
        $scope.stateUrl = null;
        $scope.sidePanelPage = false;
        $scope.sidePanelPageEndpoint = false;
        $scope.sidePanelObject = {};
        $scope.rootTenant = TenantService.createObject();
        $scope.rootTenants = TenantListService.createList();
        $scope.policyDisabled = true;
        $scope.viewPath = 'src/app/gbp/';
        $scope.selectedNode = {};
        $scope.apiType = 'operational';
        $scope.parentTenant = 'tenant-red';
        $scope.resolvedPolicy = {};
        $scope.endpoints = EndpointsListService.createList();

        /* methods */
        $scope.broadcastFromRoot = broadcastFromRoot;
        $scope.closeSidePanel = closeSidePanel;
        $scope.openSidePanel = openSidePanel;
        $scope.setRootTenant = setRootTenant;
        $scope.toggleExpanded = toggleExpanded;
        $scope.openSidePanelContract = openSidePanelContract;
        $scope.openSidePanelChild = openSidePanelChild;
        $scope.deselectEpg = deselectEpg;
        $scope.deselectContract = deselectContract;
        $scope.openSidePanelTpl = openSidePanelTpl;
        $scope.getObjectsCount = getObjectsCount;
        $scope.expandAll = expandAll;
        $scope.collapseAll = collapseAll;
        $scope.highlightNode = highlightNode;
        $scope.highlightLink = highlightLink;
        $scope.fadeAll = fadeAll;

        RootGbpService.setMainClass();
        console.log('RootGbpCtrl initialized');

        init();


        $scope.topologyData = {
            nodes: [],
            links: [],
        };

        /* implementations */
        /**
         * Sets '$scope.sidePanelPage' to false. This variable is watched in index.tpl.html template
         * and opens/closes side panel
         */
        function init() {
            $scope.rootTenants.clearData();
            $scope.rootTenants.get('config');
        }

        function broadcastFromRoot(eventName, val) {
            $scope.$broadcast(eventName, val);
        }

        function setRootTenant() {
            $scope.broadcastFromRoot('ROOT_TENANT_CHANGED');
            enableButtons();
        }

        function closeSidePanel() {
            $scope.sidePanelPage = false;
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
        }

        /**
         * fills $scope.stateUrl with loaded url
         * It's called on $viewContentLoaded event
         */
        function setStateUrl() {
            $scope.stateUrl = $state.current.url;
        }

        /**
         * Sets '$scope.sidePanelPage' to true. This variable is watched in index.tpl.html template
         * and opens/closes side panel
         */
        function openSidePanel(page, object, cbk) {
            if(object.constructor.name == 'Epg') {
                $scope.endpoints.clearData();
                $scope.endpoints.getByEpg(object.data.id);
            }
            var samePage = page === $scope.sidePanelPage;

            $scope.selectedNode = object;

            $scope.sidePanelCbk = cbk;
            $scope.sidePanelPage = page;
            $scope.sidePanelObject = object;

            if ( samePage &&  $scope.sidePanelCbk) {
                $scope.sidePanelCbk();
            }
        }

        function deselectEpg() {
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
            var elements;

            $scope.sidePanelPage = 'resolved-policy/epg-sidepanel';
            elements = EpgListService.createList();
            elements.get($scope.apiType, $scope.parentTenant);
            $scope.sidePanelObject = elements;
            $scope.selectedNode = null;
        }

        function deselectContract() {
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
            $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';

            var obj = Object.keys($scope.resolvedPolicy).map(function(k) {
                var obj = $scope.resolvedPolicy[k];
                obj.linkId = k;

                return obj;
            });

            $scope.sidePanelObject = obj;
            $scope.selectedNode = null;
        }

        function openSidePanelTpl(tpl) {
            switch(tpl) {
            case 'contract':
                $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';
                break;
            case 'subject':
                $scope.sidePanelPage = 'resolved-policy/subject-sidepanel';
                break;
            case 'clause':
                $scope.sidePanelPage = 'resolved-policy/clause-sidepanel';
                break;
            case 'rule':
                $scope.sidePanelPage = 'resolved-policy/rule-sidepanel';
                break;
            }
        }

        function openSidePanelContract(idElement) {
            var obj = $filter('filter')(Object.keys($scope.resolvedPolicy).map(function(k) {
                var obj = $scope.resolvedPolicy[k];
                obj.linkId = k;

                return obj;
            }), {'contract-id': idElement});

            $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';
            $scope.sidePanelObject = obj[0];
            $scope.selectedNode = obj[0];

            NextTopologyService.highlightLink($rootScope.nxTopology, obj[0].linkId);
        }

        function openSidePanelChild(index, type) {
            switch(type) {
            case 'subject':
                $scope.sidePanelPage = 'resolved-policy/subject-sidepanel';
                $scope.subjectIndex = index;
                break;
            case 'clause':
                $scope.sidePanelPage = 'resolved-policy/clause-sidepanel';
                $scope.clauseIndex = index;
                break;
            case 'rule':
                $scope.sidePanelPage = 'resolved-policy/rule-sidepanel';
                $scope.ruleIndex = index;
                break;
            }
        }

        function getObjectsCount(obj) {
            if(obj)
                return Object.keys(obj).length;
            else
                return 0;
        }

        function enableButtons() {
            $scope.policyDisabled = false;
        }

        function toggleExpanded(element) {
            if(typeof element !== 'string') {
                if(element.expanded)
                    element.expanded = false;
                else
                    element.expanded = true;
            }
        }

        function expandAll(arr) {
            arr.forEach(function(element) {
                element.expanded = true;
            });
        }

        function collapseAll(arr) {
            arr.forEach(function(element) {
                element.expanded = false;
            });
        }

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

        function highlightNode(node) {
            NextTopologyService.highlightNode($rootScope.nxTopology, node);
        }

        function highlightLink(link) {
            NextTopologyService.highlightLink($rootScope.nxTopology, link);
        }

        function fadeAll() {
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
        }

        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);

        // $scope.$watch('nxTopology', function() {
        //     $rootScope.nxTopology = $scope.nxTopology;
        // });
    }
});
