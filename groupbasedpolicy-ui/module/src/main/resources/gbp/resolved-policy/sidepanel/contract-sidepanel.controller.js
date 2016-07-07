define([], function() {
    'use strict';

    angular.module('app.gbp').controller('ContractSidePanelController', ContractSidePanelController);

    ContractSidePanelController.$inject = ['$scope'];

    function ContractSidePanelController($scope) {
        $scope.getObjectsCount = getObjectsCount;

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
    }
});
