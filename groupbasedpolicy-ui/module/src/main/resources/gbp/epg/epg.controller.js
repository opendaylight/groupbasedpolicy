define([
    'app/gbp/epg/epg.service',
    'app/gbp/epg/epg-list.service',
], function() {
    'use strict';

    angular.module('app.gbp').controller('EpgController', EpgController);

    EpgController.$inject = ['$scope', '$stateParams', '$mdDialog', 'EpgService', 'EpgListService'];

    function EpgController($scope, $stateParams, $mdDialog, EpgService, EpgListService) {
        $scope.epgsTableQuery = {};

        // $scope.epg = EpgService.createObject();
        // $scope.epg.get($stateParams.epgId, $stateParams.tenantId);

        $scope.epgs = EpgListService.createList();

        /* methods */
        $scope.getEpgList = getEpgList;
        $scope.openEpgDialog = openEpgDialog;
        $scope.deleteEpgDialog = deleteEpgDialog;

        init();

        /* Implementations */

        /**
         * fills $scope.epgs array with data from data store
         */
        function getEpgList() {
            if($stateParams.tenantId) {
                $scope.epgs = EpgListService.createList();
                $scope.epgs.get('config', $stateParams.tenantId);
            }
            else {
                $scope.epgs = EpgListService.createList();
                $scope.epgs.get('config', $scope.rootTenant);
            }
        }

        /**
         * Initializing function
         */
        function init() {
            $scope.epgsTableQuery = {
                order: 'data.id',
                limit: 25,
                page: 1,
                options: [25, 50, 100],
                filter: ''
            };

            getEpgList();
        }

        function openEpgDialog(epgData) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'AddEpgController',
                preserveScope: true,
                templateUrl: 'src/app/gbp/epg/dialog-add-epg.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    epg: epgData
                }
            });
        }

        function deleteEpgDialog(epgData) {
            var confirm = $mdDialog.confirm()
                .title('Delete EPG')
                .textContent('Do you want to delete EPG ' + epgData.data.name + '?')
                .ok('Delete')
                .cancel('Cancel');

            $mdDialog.show(confirm).then(function() {
                epgData.deleteEpg($scope.rootTenant,
                    function() {
                        $scope.getEpgList();
                    }
                );
            }, function() {

            });
        }
    }
});