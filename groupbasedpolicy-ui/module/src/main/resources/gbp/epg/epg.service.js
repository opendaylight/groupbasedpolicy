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

            /* Implementation */
            /**
             * fills Epg object with data
             * @param data
             */
            function setData(data) {
                this.data.id = data.id;
                this.data.name = data.name;
                this.data.description = data.description;
                this.data.intraGroupPolicy = data['intra-group-policy'];

                this.data.consumerNamedSelector = data['consumer-named-selector'];
                this.data.providerNamedSelector = data['provider-named-selector'];
                this.data.consumerTargetSelector = data['consumer-target-selector'];
                this.data.providerTargerSelector = data['provider-target-selector'];

                this.data.networkDomain = data['network-domain'];
                this.data.parent = data.parent;

                this.data.requirement = data.requirement;
                this.data.capability = data.capability;
            }

            /**
             * gets one Epg object from Restconf
             * @param id
             * @returns {*}
             */
            function get(id, idTenant) {
                var self = this;

                var restObj = Restangular.one('restconf').one('config').one('policy:tenants').one('tenant')
                    .one(idTenant).one('policy').one('endpoint-group').one(this.data.id || id);

                return restObj.get().then(function(data) {
                    self.setData(data['endpoint-group'][0]);
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