define(['app/gbp/common/gbp.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('RootGbpCtrl', RootGbpCtrl);

    RootGbpCtrl.$inject = ['$state', '$scope', 'RootGbpService'];

    function RootGbpCtrl($state, $scope, RootGbpService) {
        /* properties */
        $scope.stateUrl = null;
        $scope.sidePanelPage = false;

        /* methods */
        $scope.closeSidePanel = closeSidePanel;
        $scope.openSidePanel = openSidePanel;

        RootGbpService.setMainClass();
        console.log('RootGbpCtrl initialized');

        /* implementations */
        /**
         * Sets '$scope.sidePanelPage' to false. This variable is watched in index.tpl.html template
         * and opens/closes side panel
         */
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

        /* event listeners */
        /**
         * Event fired after content loaded, setStateUrl function is called to fill stateUrl method
         */
        $scope.$on('$viewContentLoaded', setStateUrl);

    }
});
