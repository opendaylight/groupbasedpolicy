define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('ResolvedPolicyController', ResolvedPolicyController);

    ResolvedPolicyController.$inject = ['$rootScope', '$scope', '$mdDialog', 'EpgService', 'NextTopologyService'];

    /* @ngInject */
    function ResolvedPolicyController($rootScope, $scope, $mdDialog, EpgService,  NextTopologyService) {

        $scope.reloadTopology = reloadTopology;

        $scope.cbkFunctions = {
            clickNode: function(node){
                var epg = EpgService.createObject();

                epg.get(node['_data-id'], node['_model']['_data']['tenantId'], 'operational', function() {
                    $scope.openSidePanel('resolved-policy/epg-sidepanel', epg, null);
                });

                $scope.$apply();
                $scope.parentTenant = node['_model']['_data']['tenantId'];

                NextTopologyService.highlightNode($rootScope.nxTopology, node['_data-id']);
            },
            clickLink: function(link){
                var resolvedContract = $scope.resolvedPolicy[link['_model']['_data'].id];
                resolvedContract.linkId = link['_model']['_data'].id;
                $scope.openSidePanel('resolved-policy/contract-sidepanel', resolvedContract, null);
                $scope.$apply();

                NextTopologyService.highlightLink($rootScope.nxTopology, link['_model']['_data'].id);
            },
            topologyGenerated: function(){
            }
        };

        function openSfcDialog(chainName) {
            $mdDialog.show({
                clickOutsideToClose: true,
                controller: 'SfcTopologyController',
                preserveScope: true,
                templateUrl: $scope.viewPath + 'sfc/dialog-sfc-topology.tpl.html',
                parent: angular.element(document.body),
                scope: $scope,
                locals: {
                    chainName: chainName,
                },
            });
        }

        function reloadTopology() {
            $scope.fillTopologyData();
        }

        $scope.$watch('nxTopology', function() {
            $rootScope.nxTopology = $scope.nxTopology;
        });
    }

});

