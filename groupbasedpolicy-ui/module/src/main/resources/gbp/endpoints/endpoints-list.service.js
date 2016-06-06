define([], function () {
    'use strict';

    angular.module('app.gbp').service('EndpointsListService', EndpointsListService);

    EndpointsListService.$inject = ['Restangular', 'EndpointService'];

    function EndpointsListService(Restangular, EndpointService) {
        /* methods */
        this.createList = createList;

        function EndpointsList() {
            /* properties */
            this.data = [];

            /* methods */
            this.setData = setData;
            this.get = get;
            this.clearData = clearData;

            /* Implementation */
            /**
             * fills EndpointsList object with data
             * @param data
             */
            function setData(data) {
                var self = this;
                data.forEach(function (dataElement) {
                    self.data.push(EndpointService.createObject(dataElement));
                });
            }

            function clearData() {
                var self = this;
                self.data = [];
            }

            function get() {
                /* jshint validthis:true */
                var self = this;
                var restObj = Restangular.one('restconf').one('operational').one('base-endpoint:endpoints');

                return restObj.get().then(function (data) {
                    self.setData(data.endpoints['address-endpoints']['address-endpoint']);
                });
            }
        }

        function createList() {
            var obj = new EndpointsList();

            return obj;
        }
    }

    return EndpointsListService;
});
