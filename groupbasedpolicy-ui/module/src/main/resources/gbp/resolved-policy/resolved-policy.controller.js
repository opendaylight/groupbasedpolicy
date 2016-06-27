define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('ResolvedPolicyController', ResolvedPolicyController);

    ResolvedPolicyController.$inject = ['$rootScope', '$scope', '$mdDialog', 'EpgService', 'NextTopologyService'];

    /* @ngInject */
    function ResolvedPolicyController($rootScope, $scope, $mdDialog, EpgService,  NextTopologyService) {

        $scope.reloadTopology = reloadTopology;

        $scope.cbkFunctions = {
            clickNode: function(node){
                var epg = $scope.resolvedPolicy.epgs[node['_model']['_id']];

                epg.id = node['_model']['_id'];
                $scope.openSidePanel('resolved-policy/epg-sidepanel', epg, null);
                $scope.$apply();

                NextTopologyService.highlightNode($rootScope.nxTopology, node['_model']['_id']);
            },
            clickLink: function(link){
                var resolvedContract = $scope.resolvedPolicy.contracts[link['_model']['_id']];
                resolvedContract.linkId = link['_model']['_id'];
                $scope.openSidePanel('resolved-policy/contract-sidepanel', resolvedContract, null);
                $scope.$apply();

                NextTopologyService.highlightLink($rootScope.nxTopology, link['_model']['_id']);
            },
            topologyGenerated: function(){
            }
        };

        //function openSfcDialog(chainName) {
        //    $mdDialog.show({
        //        clickOutsideToClose: true,
        //        controller: 'SfcTopologyController',
        //        preserveScope: true,
        //        templateUrl: $scope.viewPath + 'sfc/dialog-sfc-topology.tpl.html',
        //        parent: angular.element(document.body),
        //        scope: $scope,
        //        locals: {
        //            chainName: chainName,
        //        },
        //    });
        //}

        function reloadTopology() {
            $scope.fillTopologyData();
        }

        $scope.$watch('nxTopology', function() {
            $rootScope.nxTopology = $scope.nxTopology;
        });
    }

});

