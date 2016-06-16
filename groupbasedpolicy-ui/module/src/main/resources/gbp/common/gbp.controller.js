define(['app/gbp/common/gbp.service', 'app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$state', '$rootScope', '$scope', '$filter', 'RootGbpService', 'TenantListService', 'TenantService', 'EpgListService', 'ResolvedPolicyService', 'NextTopologyService', 'EndpointsListService'];

    function RootGbpCtrl($state, $rootScope, $scope, $filter, RootGbpService, TenantListService, TenantService, EpgListService, ResolvedPolicyService, NextTopologyService, EndpointsListService) {
        /* properties */
        $scope.apiType = 'operational';
        $scope.endpoints = EndpointsListService.createList();
        $scope.policyDisabled = true;
        $scope.rootTenant = null;
        $scope.rootTenants = TenantListService.createList();
        $scope.resolvedPolicy = {};
        $scope.selectedNode = {};
        $scope.sidePanelObject = {};
        $scope.sidePanelPage = false;
        $scope.sidePanelPageEndpoint = false;
        $scope.stateUrl = null;
        $scope.topologyData = {nodes: [], links: []};
        $scope.viewPath = 'src/app/gbp/';

        var resolvedPolicies = ResolvedPolicyService.createObject();
        resolvedPolicies.get(fillTopologyData);

        /* methods */
        $scope.fillTopologyData = fillTopologyData;
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
        init();

        /* implementations */

        /**
         *
         * @param eventName
         * @param val
         */
        function broadcastFromRoot(eventName, val) {
            $scope.$broadcast(eventName, val);
        }

        /**
         *
         */
        function closeSidePanel() {
            $scope.sidePanelPage = false;
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
        }

        /**
         *
         * @param arr
         */
        function collapseAll(arr) {
            arr.forEach(function(element) {
                element.expanded = false;
            });
        }

        /**
         *
         * @param source
         * @param target
         * @param contract
         * @param tenant
         * @returns {{id: string, source: *, target: *, tenant: *}}
         */
        function createLink( source, target, contract, tenant) {
            return {
                'id': generateLinkId(contract, source, target),
                'source': source,
                'target': target,
                'tenant': tenant,
            };
        }

        /**
         *
         * @param nodeName
         * @param tenantId
         * @returns {{id: *, tenantId: *, node-id: *, label: *}}
         */
        function createNode(nodeName, tenantId) {
            return {
                'id': nodeName,
                'tenantId' : tenantId,
                'node-id': nodeName,
                'label': nodeName,
            };
        }

        /**
         *
         */
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

        /**
         *
         */
        function deselectEpg() {
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
            var elements;

            $scope.sidePanelPage = 'resolved-policy/epg-sidepanel';
            elements = EpgListService.createList();
            elements.get($scope.apiType, $scope.rootTenant);
            $scope.sidePanelObject = elements;
            $scope.selectedNode = null;
        }

        /**
         *
         */
        function enableButtons() {
            $scope.policyDisabled = false;
        }

        /**
         *
         * @param arr
         */
        function expandAll(arr) {
            arr.forEach(function(element) {
                element.expanded = true;
            });
        }

        /**
         *
         */
        function fadeAll() {
            NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
        }

        /**
         *
         * @param data
         */
        function fillResolvedPolicy(data) {
            if(data['policy-rule-group-with-endpoint-constraints']) {
                processPolicyRuleGroupWithEpConstraints(
                    data['policy-rule-group-with-endpoint-constraints'],
                    data['provider-epg-id'],
                    data['consumer-epg-id']);
            }

        }

        /**
         *
         */
        function fillTopologyData() {
            var topoData = {nodes: [], links: [],},
                filteredResolvedPolicies = $filter('filter')(resolvedPolicies.data, {'consumer-tenant-id': $scope.rootTenant, 'provider-tenant-id': $scope.rootTenant});


            filteredResolvedPolicies && filteredResolvedPolicies.forEach(function(rp) {
                if(rp['consumer-tenant-id'] === $scope.rootTenant) {
                    topoData.nodes.push(createNode(rp['consumer-epg-id'], rp['consumer-tenant-id']));
                }
                topoData.nodes.push(createNode(rp['provider-epg-id'], rp['provider-tenant-id']));

                fillResolvedPolicy(rp);
                topoData.links = getContracts(rp);
            });

            $scope.topologyData = topoData;
            $scope.topologyLoaded = true;
        }

        /**
         *
         * @param contractId
         * @param providerEpgId
         * @param consumerEpgId
         * @returns {string}
         */
        function generateLinkId(contractId, providerEpgId, consumerEpgId) {
            return contractId + '_' + providerEpgId + '_' + consumerEpgId;
        }

        /**
         *
         * @param data
         * @returns {Array}
         */
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

        /**
         *
         * @param obj
         * @returns {*}
         */
        function getObjectsCount(obj) {
            if(obj)
                return Object.keys(obj).length;
            else
                return 0;
        }

        /**
         *
         * @param node
         */
        function highlightNode(node) {
            NextTopologyService.highlightNode($rootScope.nxTopology, node);
        }

        /**
         *
         * @param link
         */
        function highlightLink(link) {
            NextTopologyService.highlightLink($rootScope.nxTopology, link);
        }

        /**
         *
         */
        function init() {
            $scope.rootTenants.clearData();
            $scope.rootTenants.get('config');
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

        /**
         *
         * @param idElement
         */
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

        /**
         * .
         * @param index
         * @param type
         */
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

        /**
         *
         * @param tpl
         */
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

        /**
         *
         * @param data
         * @param providerEpgId
         * @param consumerEpgId
         */
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

        /**
         *
         */
        function setRootTenant() {
            $scope.broadcastFromRoot('ROOT_TENANT_CHANGED');

            if($scope.stateUrl.startsWith('/resolved-policy')) {
                fillTopologyData();
            }
        }

        /**
         * fills $scope.stateUrl with loaded url
         * It's called on $viewContentLoaded event
         */
        function setStateUrl() {
            $scope.stateUrl = $state.current.url;
            closeSidePanel();

            if($scope.stateUrl.startsWith('/resolved-policy')) {
                fillTopologyData();
            }
        }

        /**
         *
         * @param element
         */
        function toggleExpanded(element) {
            if(typeof element !== 'string') {
                if(element.expanded)
                    element.expanded = false;
                else
                    element.expanded = true;
            }
        }

        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);
    }
});
