define(['app/gbp/common/gbp.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$state', '$scope', 'RootGbpService', 'TenantListService', 'TenantService', 'ContractService'];

    function RootGbpCtrl($state, $scope, RootGbpService, TenantListService, TenantService, ContractService) {
        /* properties */
        $scope.stateUrl = null;
        $scope.sidePanelPage = false;
        $scope.rootTenant = TenantService.createObject();
        $scope.rootTenants = TenantListService.createList();
        $scope.policyDisabled = true;

        /* methods */
        $scope.broadcastFromRoot = broadcastFromRoot;
        $scope.closeSidePanel = closeSidePanel;
        $scope.openSidePanel = openSidePanel;
        $scope.setRootTenant = setRootTenant;
        $scope.disableButton = disableButton;

        RootGbpService.setMainClass();
        console.log('RootGbpCtrl initialized');

        init();

        /* implementations */
        /**
         * Sets '$scope.sidePanelPage' to false. This variable is watched in index.tpl.html template
         * and opens/closes side panel
         */
        function init() {
            $scope.rootTenants.clear();
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
        function openSidePanel() {
            $scope.sidePanelPage = true;
        }

        function disableButton() {
            if (Object.keys($scope.rootTenant.data) > 0) {
                return true;
            }
            else {
                return false;
            }
        }

        function enableButtons() {
            $scope.policyDisabled = false;
        }
        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);

    }
});
