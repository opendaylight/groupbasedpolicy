define([], function () {
    'use strict';

    angular.module('app.gbp').service('SfcService', SfcService);

    SfcService.$inject = ['Restangular'];

    function SfcService(Restangular) {
        /* methods */
        this.createObject = createObject;

        /**
         * Sfc constructor
         * @constructor
         */
        function Sfc() {
            /* properties */
            this.data = {};

            /* methods */
            this.setData = setData;
            this.get = get;

            /* Implementation */
            /**
             * fills Sfc object with data
             * @param data
             */

            function setData(data) {
                this.data = data;
            }
            /**
             * gets one Sfc object from Restconf
             * @param id
             * @returns {*}
             */

            function get(successCbk) {

                var self = this,
                    restObj = Restangular
                        .one('restconf')
                        .one('config')
                        .one('service-function-chain:service-function-chains')
                        .one('service-function-chain')
                        .one(self.data.name);

                return restObj.get().then(function(data) {
                    self.data = data['service-function-chain'][0];
                    (successCbk || angular.noop)();
                });
            }
        }

        /**
         * creates Endpoint object and fills it with data if available
         * @param data
         * @returns {Endpoint}
         */
        function createObject(data) {
            var obj = new Sfc();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return SfcService;
});
