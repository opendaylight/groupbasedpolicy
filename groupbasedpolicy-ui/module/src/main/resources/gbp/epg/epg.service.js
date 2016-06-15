define([], function() {
    'use strict';

    angular.module('app.gbp').service('EpgService', EpgService);

    EpgService.$inject = ['Restangular'];

    function EpgService(Restangular) {
        /* methods */
        this.createObject = createObject;


        /**
         * Epg constructor
         * @constructor
         */
        function Epg() {
            /* properties */
            this.data = {};
            /* methods */
            this.setData = setData;
            this.get = get;
            this.put = put;
            this.deleteEpg = deleteEpg;

            /* Implementation */
            /**
             * fills Epg object with data
             * @param data
             */
            function setData(data) {
                this.data.id = data.id;
                this.data.name = data.name;
                this.data.description = data.description;
                this.data['intra-group-policy'] = data['intra-group-policy'];

                this.data['consumer-named-selector'] = data['consumer-named-selector'];
                this.data['provider-named-selector'] = data['provider-named-selector'];
                this.data['consumer-target-selector'] = data['consumer-target-selector'];
                this.data['provider-target-selector'] = data['provider-target-selector'];

                this.data['network-domain'] = data['network-domain'];
                this.data.parent = data.parent;

                this.data.requirement = data.requirement;
                this.data.capability = data.capability;
            }

            /**
             * gets one Epg object from Restconf
             * @param id
             * @returns {*}
             */
            function get(idEpg, idTenant, apiType) {
                var self = this;

                var restObj = Restangular.one('restconf').one(apiType).one('policy:tenants').one('tenant')
                    .one(idTenant).one('policy').one('endpoint-group').one(this.data.id || idEpg);

                return restObj.get().then(function(data) {
                    self.setData(data['endpoint-group'][0]);
                });
            }

            function put(idTenant, successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('config').one('policy:tenants').one('tenant')
                    .one(idTenant).one('policy').one('endpoint-group').one(self.data.id),
                    dataObj = {'endpoint-group': [self.data]};

                return restObj.customPUT(dataObj).then(function(data) {
                    successCallback(data);
                }, function(res) {

                });
            }

            function deleteEpg(idTenant, successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('config').one('policy:tenants').one('tenant')
                    .one(idTenant).one('policy').one('endpoint-group').one(self.data.id);

                return restObj.remove().then(function(data) {
                    successCallback(data);
                }, function(res) {

                });
            }
        }

        /**
         * creates Epg object and fills it with data if available
         * @param data
         * @returns {Epg}
         */
        function createObject(data) {
            var obj = new Epg();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return EpgService;
});