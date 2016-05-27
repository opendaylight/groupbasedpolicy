var controllers = [

];

var services = [
    'app/gbp/common/topology/next_topology.service'
];

var models = [

];

var directives = [
    'app/gbp/common/topology/next_topology.directive'
];

define(['angular'].concat(controllers).concat(services).concat(models).concat(directives),

    function(angular, NextTopologyService, NextTopology ) {
        'use strict';

        angular
            .module('app.nextTopo', [])
            .config(config);

        function config() {

        }

        angular.module('app.nextTopo')
            .service('NextTopologyService', NextTopologyService)
            .directive('nextTopology', NextTopology);

});
