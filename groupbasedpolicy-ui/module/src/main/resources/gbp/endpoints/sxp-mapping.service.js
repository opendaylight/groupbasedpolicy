define([], function () {
    'use strict';

    angular.module('app.gbp').service('SxpMappingService', SxpMappingService);

    function SxpMappingService() {
        /* methods */
        this.createObject = createObject;

        /**
         * EndpointSgt constructor
         * @constructor
         */
        function EndpointSgt() {
            /* properties */
            this.data = {};

            /* methods */
            this.setData = setData;

            /* Implementation */
            /**
             * fills EndpointSgt object with data
             * @param data
             */

            function setData(data) {
                this.data = {};
                this.data.sgt = data.sgt;
                this.data['endpoint-groups'] = data['endpoint-groups'];
                this.data.conditions = data.conditions;
                this.data.tenant = data.tenant;
            }
        }

        /**
         * creates EndpointSgt object and fills it with data if available
         * @param data
         * @returns {EndpointSgt}
         */
        function createObject(data) {
            var obj = new EndpointSgt();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return SxpMappingService;
});
