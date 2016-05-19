define([], function () {
    'use strict';

    angular.module('app.gbp').service('ContractService', ContractService);

    ContractService.$inject = ['Restangular'];

    function ContractService(Restangular) {
        /* methods */
        this.createObject = createObject;

        /**
         * Contract constructor
         * @constructor
         */
        function Contract() {
            /* properties */
            this.data = {};
            /* methods */
            this.setData = setData;
            this.get = get;

            /* Implementation */
            /**
             * fills Contract object with data
             * @param data
             */
            function setData(data) {
                this.data.id = data.id;
                this.data.description = data.description;
                this.data.parent = data.parent;

                // TODO: use objects
                this.data['forwarding-context'] = data['forwarding-context'];
                this.data.target = data.target;
                this.data.subject = data.subject;
                this.data.clause = data.clause;
                this.data.quality = data.quality;
            }

            /**
             * gets one Contract object from Restconf
             * @param id
             * @returns {*}
             */
            function get(id) {
                var self = this;

                // var restObj = Restangular.one('restconf').one('config').one('policy:tenants')
                //             .one('tenant').one('tenant1').one('policy').one('contract').one(this.data.id || id);

                var restObj = Restangular.one('restconf').one('config').one('policy:tenants')
                            .one('tenant').one(id).one('policy').one('contract').one(id);

                return restObj.get().then(function (data) {
                    self.setData(data.contract[0]);
                });
            }
        }

        /**
         * creates Contract object and fills it with data if available
         * @param data
         * @returns {Contract}
         */
        function createObject(data) {
            var obj = new Contract();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return ContractService;
});
