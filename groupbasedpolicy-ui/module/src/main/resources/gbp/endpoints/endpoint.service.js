define([], function () {
    'use strict';

    angular.module('app.gbp').service('EndpointService', EndpointService);

    EndpointService.$inject = ['Restangular'];

    function EndpointService(Restangular) {
        /* methods */
        this.createObject = createObject;

        /**
         * Endpoint constructor
         * @constructor
         */
        function Endpoint() {
            /* properties */
            this.data = {};
            this.data['endpoint-group'] = [];
            this.data.condition = [];
            /* methods */
            this.setData = setData;
            this.get = get;
            // this.put = put;
            this.post = post;
            this.deleteEndpoint = deleteEndpoint;

            /* Implementation */
            /**
             * fills Endpoint object with data
             * @param data
             */
            function setData(data) {
                this.data['context-type'] = data['context-type'];
                this.data['context-id'] = data['context-id'];
                this.data['address-type'] = data['address-type'];
                this.data.address = data.address;
                this.data['network-containment'] = data['network-containment'];
                this.data.tenant = data.tenant;
                this.data.timestamp = Date();
                this.data['endpoint-group'] = data['endpoint-group'];
            }
            /**
             * gets one Endpoint object from Restconf
             * @param id
             * @returns {*}
             */
            function get() {
                var self = this;

                var restObj = Restangular
                    .one('restconf')
                    .one('config')
                    .one('policy:tenants')
                    .one('tenant')
                    .one(id)
                    .one('policy')
                    .one('Endpoint')
                    .one(id);

                return restObj.get().then(function (data) {
                    self.setData(data.Endpoint[0]);
                });
            }

            function post(successCbk) {

                var self = this,
                    restObj = Restangular.one('restconf').one('operations').one('base-endpoint:register-endpoint'),
                    reqData = {
                        'input': {
                            'address-endpoint-reg': [
                                self.data,
                            ],
                        },
                    };
                restObj.customPOST(reqData).then(function (data) {
                    successCbk(data);
                }, function () {

                });
            }

            function deleteEndpoint(id, successCallback) {
                var self = this;

                var restObj = Restangular
                    .one('restconf')
                    .one('config')
                    .one('policy:tenants')
                    .one('tenant')
                    .one(id)
                    .one('policy')
                    .one('Endpoint')
                    .one(self.data.id);

                return restObj.remove().then(function (data) {
                    successCallback(data);
                }, function () {

                });
            }

        }

        /**
         * creates Endpoint object and fills it with data if available
         * @param data
         * @returns {Endpoint}
         */
        function createObject(data) {
            var obj = new Endpoint();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return EndpointService;
});
