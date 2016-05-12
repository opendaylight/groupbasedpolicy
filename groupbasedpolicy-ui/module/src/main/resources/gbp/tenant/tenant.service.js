define([], function () {
    'use strict';

    angular.module('app.gbp').service('TenantService', TenantService);

    TenantService.$inject = ['Restangular'];

    function TenantService(Restangular) {
        /* methods */
        this.createObject = createObject;


        /**
         * Tenant constructor
         * @constructor
         */
        function Tenant() {
            /* properties */
            this.data = {};
            /* methods */
            this.setData = setData;
            this.get = get;

            /* Implementation */
            /**
             * fills Tenant object with data
             * @param data
             */
            function setData(data) {
                this.data.id = data.id;
                this.data.name = data.name;
                this.data.description = data.description;

                // TODO: use objects
                this.data['forwarding-context'] = data['forwarding-context'];
                this.data.policy = data.policy;
            }

            /**
             * gets one Tenant object from Restconf
             * @param id
             * @returns {*}
             */
            function get(id) {
                var self = this;

                var restObj = Restangular.one('restconf').one('config').one('policy:tenants').one('tenant')
                    .one(this.data.id || id);

                return restObj.get().then(function (data) {
                    self.setData(data.tenant[0]);
                });
            }
        }

        /**
         * creates Tenant object and fills it with data if available
         * @param data
         * @returns {Tenant}
         */
        function createObject(data) {
            var obj = new Tenant();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return TenantService;
});
