define([
    'app/gbp/epg/epg.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('AddEpgController', AddEpgController);

    AddEpgController.$inject = ['$mdDialog', '$scope', 'EpgService', 'epg'];
    /* @ngInject */
    function AddEpgController($mdDialog, $scope, EpgService, epg) {
        /* properties */
        $scope.epg = epg ? epg : EpgService.createObject();

        /* methods */
        $scope.closeDialog = closeDialog;
        $scope.save = save;

        /* Implementations */

        function closeDialog(){
            $mdDialog.cancel();
            $scope.getEpgList();
        }

        function save() {
            $scope.epg.put($scope.rootTenant, function(data) {
                $scope.closeDialog();
            }, function(err) {
            } );
        }
    }
});
