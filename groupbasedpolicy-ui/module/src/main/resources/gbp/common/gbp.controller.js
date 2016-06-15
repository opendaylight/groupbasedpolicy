define(['app/gbp/common/gbp.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$state', '$scope', 'RootGbpService', 'TenantListService', 'TenantService', 'ContractService', 'EpgService'];

    function RootGbpCtrl($state, $scope, RootGbpService, TenantListService, TenantService, ContractService, EpgService) {
        /* properties */
        $scope.stateUrl = null;
        $scope.sidePanelPage = false;
        $scope.sidePanelPageEndpoint = false;
        $scope.sidePanelObject = {};
        $scope.rootTenant = TenantService.createObject();
        $scope.rootTenants = TenantListService.createList();
        $scope.policyDisabled = true;
        $scope.viewPath = 'src/app/gbp/';

        /* methods */
        $scope.broadcastFromRoot = broadcastFromRoot;
        $scope.closeSidePanel = closeSidePanel;
        $scope.openSidePanel = openSidePanel;
        $scope.setRootTenant = setRootTenant;
        $scope.toggleExpanded = toggleExpanded;
        $scope.openSidePanelObjId = openSidePanelObjId;
        $scope.openSidePanelChild = openSidePanelChild;

        RootGbpService.setMainClass();
        console.log('RootGbpCtrl initialized');

        init();

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
            var samePage = page === $scope.sidePanelPage;

            $scope.sidePanelCbk = cbk;
            $scope.sidePanelPage = page;
            $scope.sidePanelObject = object;

            if ( samePage &&  $scope.sidePanelCbk) {
                $scope.sidePanelCbk();
            }
        }

        function openSidePanelObjId(idContract, idTenant, objType, apiType) {
            var element;

            switch(objType) {
            case 'epg':
                $scope.sidePanelPage = 'resolved-policy/epg-sidepanel';
                element = EpgService.createObject();
                break;
            case 'contract':
                $scope.sidePanelPage = 'resolved-policy/contract-sidepanel';
                element = ContractService.createObject();
                break;
            }

            element.get(idContract, idTenant, apiType);

            $scope.sidePanelObject = element;
        }

        function openSidePanelChild(parent, type) {
            switch(type) {
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

            $scope.sidePanelObject = parent;
        }

        function enableButtons() {
            $scope.policyDisabled = false;
        }

        function toggleExpanded(element) {
            if(element.expanded)
                element.expanded = false;
            else
                element.expanded = true;
        }

        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);
    }
});
