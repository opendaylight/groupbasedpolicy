define(['app/gbp/resolved-policy/resolved-policy.service'], function () {
    'use strict';

    angular.module('app.gbp').controller('ResolvedPolicyController', ResolvedPolicyController);

    ResolvedPolicyController.$inject = ['$rootScope', '$scope', 'ResolvedPolicyService'];

    /* @ngInject */
    function ResolvedPolicyController($rootScope, $scope, ResolvedPolicyService) {
        $scope.topologyData = {
            nodes: [],
            links: [],
        };

        $scope.cbkFunctions = {
            clickNode: function(node){

                /*$scope.openSidePanel('side_panel_nodes', null, function () {
                    $scope.broadcastFromRoot('SELECT_NODE', node);
                });
                $scope.$apply();*/
                console.log(node);

                //Example of highlighting
                //NextTopologyService.highlightNode($scope.nxTopology, 1);
                //NextTopologyService.highlightNode($scope.nxTopology, 1, true); without links around
                //NextTopologyService.highlightLink($scope.nxTopology, '1-7');
                //NextTopologyService.highlightPath($scope.nxTopology, [array of links obj]);

                //Fade out or in whole topology
                //NextTopologyService.fadeOutAllLayers();
                //NextTopologyService.fadeInAllLayers();
            },
             /*clickLink: function(link){
                $scope.openSidePanel('side_panel_links', null, function () {
                    $scope.broadcastFromRoot('SELECT_CONNECTION', link);
                });
                $scope.$apply();
            },*/
            topologyGenerated: function(){

                if ( $rootScope.updateTopoInterval ) {
                    clearInterval($rootScope.updateTopoInterval);
                }

                $rootScope.updateTopoInterval = setInterval(function () {
                    fillTopologyData();
                },5000);

            },
        };

        var resolvedPolicies = ResolvedPolicyService.createObject();
        resolvedPolicies.get(function () {
            fillTopologyData();
        });

        function fillTopologyData() {
            var topoData = {nodes: [], links: [],};
            resolvedPolicies.data.forEach(function(rp) {
                topoData.nodes.push(createNode(rp['consumer-epg-id']));
                topoData.nodes.push(createNode(rp['provider-epg-id']));
            });
            $scope.topologyData = topoData;
            $scope.topologyLoaded = true;
        }

        function createNode(nodeName) {
            return {
                'id': nodeName,
                'node-id': nodeName,
                'label': nodeName,
            };
        }
    }

});

