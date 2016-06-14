define([], function () {
    'use strict';

    angular.module('app.gbp').service('ForwardingService', ForwardingService);

    ForwardingService.$inject = ['Restangular'];

    function ForwardingService(Restangular) {
        /* methods */
        this.createObject = createObject;

        /**
         * Endpoint constructor
         * @constructor
         */
        function Forwarding() {
            /* properties */
            this.data = {};

            /* methods */
            this.setData = setData;
            this.get = get;

            /* Implementation */
            /**
             * fills Forwarding object with data
             * @param data
             */

            function setData(data) {
                this.data['forwarding-by-tenant'] = data['forwarding-by-tenant'];
            }

            function get(successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('config').one('forwarding:forwarding');

                return restObj.get().then(function(data) {
                    self.data = data['forwarding']['forwarding-by-tenant'];
                    (successCallback || angular.noop)();
                });
            }
        }



        /**
         * creates Endpoint object and fills it with data if available
         * @param data
         * @returns {Endpoint}
         */
        function createObject(data) {
            var obj = new Forwarding();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return ForwardingService;
});
