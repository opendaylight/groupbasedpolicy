define([
    'app/gbp/common/gbp.service',
    'app/gbp/resolved-policy/resolved-policy-list.service',
    'app/gbp/endpoints/sxp-mapping-list.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$filter', '$mdDialog', '$rootScope', '$scope', '$state',
        'EndpointsListService', 'NextTopologyService', 'ResolvedPolicyListService', 'RootGbpService',
        'TenantListService', 'SxpMappingListService'];

    function RootGbpCtrl($filter, $mdDialog, $rootScope, $scope, $state,
        EndpointsListService, NextTopologyService, ResolvedPolicyListService, RootGbpService,
        TenantListService, SxpMappingListService) {
        /* properties */
        $scope.apiType = 'operational';
        $scope.activeObject = null;
        $scope.endpoints = EndpointsListService.createList();
        $scope.endpointSgtList = SxpMappingListService.createList();
        $scope.rootTenant = null;
        $scope.rootTenants = TenantListService.createList();
        $scope.resolvedPolicy = {};
        $scope.selectedNode = {};
        $scope.sidePanelObject = {};
        $scope.sidePanelPage = false;
        $scope.sidePanelPageEndpoint = false;
        $scope.stateUrl = null;
        $scope.topologyData = { nodes: [], links: [] };
        $scope.viewPath = 'src/app/gbp/';

        var resolvedPolicies = ResolvedPolicyListService.createList();
        getResolvedPolicies();

        /* methods */
        $scope.broadcastFromRoot = broadcastFromRoot;
        $scope.closeSidePanel = closeSidePanel;
        $scope.openSfcDialog = openSfcDialog;
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
        $scope.rootOpenEndpointDialog = rootOpenEndpointDialog;
        $scope.rootDeleteEndpointDialog = rootDeleteEndpointDialog;
        $scope.getEndpointsList = getEndpointsList;

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
            if($scope.sidePanelPage) {
                $scope.sidePanelPage = false;
                $scope.fadeAll();
            }
        }

        /**
         *
         * @param arr
         */
        function collapseAll(arr) {
            arr.forEach(function (element) {
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
        function createLink( linkId) {
            var linkIdParts = linkId.split('_');
            return {
                'id': linkId,
                'source': linkIdParts[1],
                'target': linkIdParts[2],
                'tenant': $scope.rootTenant
            };
        }

        /**
         *
         * @param nodeName
         * @param tenantId
         * @returns {Object}
         */
        function createNode(nodeName) {
            return {
                'id': nodeName,
                'tenantId': $scope.rootTenant,
                'node-id': nodeName,
                'label': nodeName
            };
        }

        /**
         *
         */
        function deselectContract() {
            $scope.fadeAll();
            $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';

            $scope.sidePanelObject = Object.keys($scope.resolvedPolicy.contracts).map(function (k) {
                var ob = $scope.resolvedPolicy.contracts[k];
                ob.linkId = k;

                return ob;
            });

            $scope.selectedNode = null;
            $scope.activeObject = 'contract';
        }

        /**
         *
         */
        function deselectEpg() {
            $scope.fadeAll();
            $scope.sidePanelPage = 'resolved-policy/epg-sidepanel';

            $scope.sidePanelObject = Object.keys($scope.resolvedPolicy.epgs).map(function (k) {
                var ob = $scope.resolvedPolicy.epgs[k];
                ob.id = k;

                return ob;
            });
            $scope.selectedNode = null;
            $scope.activeObject = 'epg';
        }

        /**
         *
         * @param arr
         */
        function expandAll(arr) {
            arr.forEach(function (element) {
                element.expanded = true;
            });
        }

        /**
         *
         */
        function fadeAll() {
            $rootScope.nxTopology && NextTopologyService.fadeInAllLayers($rootScope.nxTopology);
        }

        /**
         * reads resolvedPolicies list, prepares nodes and links for topology and fills them
         */
        function fillTopologyData() {
            var tempTopoData = {nodes: [], links: []};

            $scope.resolvedPolicy = resolvedPolicies.aggregateResolvedPolicies();
            console.log('resolved and aggregated', $scope.resolvedPolicy);

            tempTopoData.nodes = Object.keys($scope.resolvedPolicy.epgs).map(function (key) {
                return createNode(key);
            });

            tempTopoData.links = Object.keys($scope.resolvedPolicy.contracts).map(function (key) {
                return createLink(key);
            });

            $scope.topologyData = tempTopoData;
            $scope.topologyLoaded = true;
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

        function getResolvedPolicies() {
            if($scope.rootTenant) {
                resolvedPolicies.get($scope.rootTenant, fillTopologyData);
            }
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
            $state.go('main.gbp.index.resolvedPolicy');
            $scope.endpointSgtList.get();
        }

        /**
         *
         * @param chainName
         */
        function openSfcDialog(chainName) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'SfcTopologyController',
                preserveScope: true,
                templateUrl: $scope.viewPath + 'sfc/dialog-sfc-topology.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    chainName: chainName
                }
            });
        }


        /**
         * Sets '$scope.sidePanelPage' to true. This variable is watched in index.tpl.html template
         * and opens/closes side panel
         */
        function openSidePanel(page, object, cbk) {
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
            var obj = $filter('filter')(Object.keys($scope.resolvedPolicy.contracts).map(function (k) {
                var obj = $scope.resolvedPolicy.contracts[k];
                obj.linkId = k;

                return obj;
            }), { 'contract-id': idElement });

            $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';
            $scope.sidePanelObject = obj[0];
            $scope.selectedNode = obj[0];
            $scope.activeObject = 'contract';

            NextTopologyService.highlightLink($rootScope.nxTopology, obj[0].linkId);
        }

        /**
         * .
         * @param index
         * @param type
         */
        function openSidePanelChild(index, type) {
            switch (type) {
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
            switch (tpl) {
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
         */
        function setRootTenant() {
            $scope.broadcastFromRoot('ROOT_TENANT_CHANGED');

            if ($scope.stateUrl.startsWith('/resolved-policy')) {
                getResolvedPolicies()
                if($scope.sidePanelPage) {
                    if($scope.activeObject == 'epg')
                        deselectEpg();
                    else if($scope.activeObject == 'contract')
                        deselectContract();
                }
            }
        }

        /**
         * fills $scope.stateUrl with loaded url
         * It's called on $viewContentLoaded event
         */
        function setStateUrl() {
            $scope.stateUrl = $state.current.url;
            closeSidePanel();

            if ($scope.stateUrl.startsWith('/resolved-policy')) {
                getResolvedPolicies()
            }
        }

        /**
         *
         * @param element
         */
        function toggleExpanded(element) {
            if (typeof element !== 'string') {
                element.expanded = !element.expanded;
            }
        }

        function rootOpenEndpointDialog(operation, endpointData) {
            $scope.disableKeyFieldsEditing = operation === 'edit';
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'AddEndpointController',
                preserveScope: true,
                templateUrl: $scope.viewPath + 'endpoints/dialog-add-endpoint.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    endpoint: endpointData
                }
            });
        }

        function rootDeleteEndpointDialog(endpointData) {
            var confirm = $mdDialog.confirm()
                .title('Delete endpoint')
                .textContent('Do you want to delete endpoint?')
                .ok('Delete')
                .cancel('Cancel');

            $mdDialog.show(confirm).then(function () {
                endpointData.deleteEndpoint(function () {
                    getEndpointsList();
                });
            }, function () {

            });
        }

        function getEndpointsList() {
            $scope.endpoints.clearData();
            $scope.endpoints.getByEpg($scope.selectedNode.data.id);
        }
        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);
    }
});
