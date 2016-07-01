define([
    'app/gbp/sfc/sfc.service'
], function () {
    'use strict';

    angular.module('app.gbp').controller('SfcTopologyController', SfcTopologyController);

    SfcTopologyController.$inject = ['$filter', '$mdDialog', '$scope', 'chainName', 'SfcService'];
    /* @ngInject */
    function SfcTopologyController($filter, $mdDialog, $scope, chainName, SfcService) {
        /* properties */
        $scope.chain = SfcService.createObject({name: chainName});

        /* methods */
        $scope.closeDialog = closeDialog;

        /* Implementations */

        $scope.chain.get(function() {
            $scope.chain.data && $scope.chain.data['sfc-service-function'] &&
                $scope.chain.data['sfc-service-function'].length && $scope.viewTopology();

            $scope.topologyLoaded = true;
        });

        function closeDialog(){
            $mdDialog.cancel();
        }

        function nodeTooltip() {
            nx.define('MyNodeTooltip', nx.ui.Component, {
                properties: {
                    node: {},
                    topology: {},
                },
                view: {
                    content: [{
                        tag: 'p',
                        content: [{
                            tag: 'label',
                            content: '{#node.model.data.type}',
                        }],
                    }],
                },
            });
        }

        $scope.viewTopology = function() {
            nodeTooltip();
            $scope.topologySfc = new nx.graphic.Topology({
                height: 400,
                width: 600,
                scalable: true,
                theme: 'blue',
                enableGradualScaling: true,
                nodeConfig: {
                    color: '#0386d2',
                    //label: 'model.label',
                    label: function (vertex) {
                        return vertex.get().label + ' (' + vertex.get().type + ')';
                    },
                    scale: 'model.scale',
                    iconType: function (vertex) {
                        var type = vertex.get().type;
                        switch (type) {
                            case 'firewall':
                                return 'firewall';
                            case 'dpi':
                                return 'accesspoint';
                            case 'qos':
                                return 'wlc';
                            default:
                                return 'unknown';
                        }
                    },
                },
                linkConfig: {
                    label: 'model.label',
                    linkType: 'parallel',
                    color: '#0386d2',
                    width: 5,
                },
                showIcon: true,
                enableSmartNode: false,
                tooltipManagerConfig: {
                    showLinkTooltip: false,
                    showNodeTooltip: false,
                    //nodeTooltipContentClass: 'MyNodeTooltip',
                },
            });
            $scope.app =  new nx.ui.Application;

            var nodes = [];
            var links = [];

            $scope.chain.data['sfc-service-function'].forEach(function(sf, index) {
                nodes.push({
                    id: sf.name,
                    label: sf.name,
                    type: SfcService.getSfTypeShort(sf.type),
                    x: 100 * (index + 1),
                    y: 400,
                });

                index>0 && links.push({
                    source: index - 1,
                    target: index,
                });
            })

            $scope.topologySfc.data({
                nodes: nodes,
                links: links,
            });

            $scope.app.container(document.getElementById('next-vpp-topo'));
            $scope.topologySfc.attach($scope.app);
        };


    }
});
