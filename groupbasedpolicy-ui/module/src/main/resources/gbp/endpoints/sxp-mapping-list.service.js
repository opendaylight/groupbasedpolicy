define(['app/gbp/endpoints/sxp-mapping.service'], function () {
    'use strict';

    angular.module('app.gbp').service('SxpMappingListService', SxpMappingListService);

    SxpMappingListService.$inject = ['$filter', 'Restangular', 'SxpMappingService'];

    function SxpMappingListService($filter, Restangular, SxpMappingService) {
        /* methods */
        this.createList = createList;

        function EndpointsSgtList() {
            /* properties */
            this.data = [];

            /* methods */
            this.setData = setData;
            this.get = get;
            this.clearData = clearData;
            this.findSgtsForEndpoint = findSgtsForEndpoint;

            /* Implementation */
            /**
             * fills EndpointsSgtList object with data
             * @param data
             */
            function setData(data) {
                var self = this;

                data && data.forEach(function (dataElement) {
                    self.data.push(SxpMappingService.createObject(dataElement));
                });
            }

            function clearData() {
                var self = this;
                self.data = [];
            }

            function get() {
                /* jshint validthis:true */
                var self = this;
                var restObj = Restangular.one('restconf').one('config').one('sxp-mapper-model:sxp-mapper');

                return restObj.get().then(function (data) {
                    self.setData(data['sxp-mapper']['endpoint-policy-template-by-sgt']);
                });
            }

            function findSgtsForEndpoint(EPobject) {
                var self = this,
                    result = self.data.map(function (ele) {
                    // properties correction
                    var condition = ele.data.conditions === undefined ? [] : ele.data.conditions,
                        epg = ele.data['endpoint-groups'] === undefined ? [] : ele.data['endpoint-groups'];

                    return condition.length==EPobject.data.condition.length && condition.every(function(v,i) { return ($.inArray(v,EPobject.data.condition) != -1)}) &&
                           epg.length==EPobject.data['endpoint-group'].length && epg.every(function(v,i) { return ($.inArray(v,EPobject.data['endpoint-group']) != -1)}) &&
                           angular.equals(ele.data.tenant, EPobject.data.tenant) ? ele.data.sgt : false;
                }).filter(Boolean);
                return result.toString();
            }
        }

        function createList() {
            var obj = new EndpointsSgtList();

            return obj;
        }
    }

    return SxpMappingListService;
});
