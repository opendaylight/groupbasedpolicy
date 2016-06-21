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
                this.data['endpoint-group'] = data['endpoint-group'] ? data['endpoint-group'] : [];
                this.data.condition = data.condition ? data.condition : [];
                this.data.timestamp = data.timestamp ? data.timestamp : Date();
                if (this.data['absolute-location']){
                    this.data['absolute-location']['internal-node'] = data['absolute-location']['internal-node'];
                    this.data['absolute-location']['internal-node-connector'] = data['absolute-location']['internal-node-connector'];
                }
            }
            /**
             * gets one Endpoint object from Restconf
             * @param id
             * @returns {*}
             */

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

            function deleteEndpoint(successCallback) {
                var self = this,
                    tmpEndpointObject = {
                        'context-type': self.data['context-type'],
                        'context-id': self.data['context-id'],
                        'address': self.data.address,
                        'address-type': self.data['address-type'],
                    };
                var restObj = Restangular
                        .one('restconf')
                        .one('operations')
                        .one('base-endpoint:unregister-endpoint'),
                    reqData = {
                        'input': {
                            'address-endpoint-unreg': [
                                tmpEndpointObject,
                            ],
                        },
                    };
                return restObj.customPOST(reqData).then(function (data) {
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
