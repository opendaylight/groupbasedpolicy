define([
    'app/gbp/common/gbp.service',
    'app/gbp/resolved-policy/resolved-policy-list.service',
    'app/gbp/endpoints/sxp-mapping-list.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$mdDialog', '$rootScope', '$scope', '$state',
        'EndpointsListService', 'NextTopologyService', 'ResolvedPolicyListService', 'RootGbpService',
        'TenantListService', 'SxpMappingListService'];

    function RootGbpCtrl($mdDialog, $rootScope, $scope, $state,
        EndpointsListService, NextTopologyService, ResolvedPolicyListService, RootGbpService,
        TenantListService, SxpMappingListService) {

        /* properties */
        $scope.apiType = 'operational';
        $scope.activeObject = null;
        $scope.endpoints = EndpointsListService.createList();
        $scope.endpointSgtList = SxpMappingListService.createList();
        $scope.innerObj = {};
        $scope.rootTenant = null;
        $scope.rootTenants = TenantListService.createList();
        $scope.resolvedPolicy = {};
        $scope.sidePanelObject = null;
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
        $scope.fillTopologyData = fillTopologyData;
        $scope.highlightNode = highlightNode;
        $scope.highlightLink = highlightLink;
        $scope.fadeAll = fadeAll;
        $scope.rootOpenEndpointDialog = rootOpenEndpointDialog;
        $scope.rootDeleteEndpointDialog = rootDeleteEndpointDialog;

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
                $scope.sidePanelObject = null;
                $scope.fadeAll();
            }
        }

        /**
         *
         * @param source
         * @param target
         * @param contract
         * @param tenant
         * @returns {{id: string, source: *, target: *, tenant: *}}
         */
        function createLink( linkId, type) {
            var linkIdParts = linkId.split('++');
            return {
                'id': linkId,
                'source': linkIdParts[1],
                'target': linkIdParts[2],
                'tenant': $scope.rootTenant,
                'type': type,
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
                'label': nodeName,
            };
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

            tempTopoData.nodes = Object.keys($scope.resolvedPolicy.epgs).map(function (key) {
                return createNode(key);
            });

            tempTopoData.links = Object.keys($scope.resolvedPolicy.contracts).map(function (key) {
                return createLink(key, $scope.resolvedPolicy.contracts[key].type);
            });

            $scope.topologyData = tempTopoData;
            $scope.topologyLoaded = true;
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
        function openSidePanel(page, object, type, element) {
            $scope.sidePanelPage = page;
            $scope.sidePanelObject = object;

            switch(type) {
                case 'subject':
                    $scope.innerObj.subject = element;
                    break;
                case 'clause':
                    $scope.innerObj.clause = element;
                    break;
                case 'rule':
                    $scope.innerObj.rule = element;
                    break;
                default:
            }
        }

        /**
         *
         */
        function setRootTenant() {
            $scope.broadcastFromRoot('ROOT_TENANT_CHANGED');

            if ($scope.stateUrl.startsWith('/resolved-policy')) {
                getResolvedPolicies();
                if($scope.sidePanelObject) {
                    if($scope.sidePanelObject['contract-id'])
                        openSidePanel('resolved-policy/sidepanel/views/contract-list-sidepanel');
                    else
                        openSidePanel('resolved-policy/sidepanel/views/epg-list-sidepanel');
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
                getResolvedPolicies();
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
                    $scope.$broadcast('endpointChanged');
                });
            }, function () {

            });
        }

        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);
    }
});
