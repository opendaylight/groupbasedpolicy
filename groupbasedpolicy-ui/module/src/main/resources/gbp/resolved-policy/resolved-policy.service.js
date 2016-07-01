define([], function() {
    'use strict';

    angular.module('app.gbp').service('ResolvedPolicyService', ResolvedPolicyService);

    ResolvedPolicyService.$inject = [];

    function ResolvedPolicyService() {
        /* methods */
        this.createObject = createObject;


        function ResolvedPolicy() {
            /* properties */
            this.data = {};
            /* methods */
            this.setData = setData;

            /* Implementation */

            function setData(data) {
                this.data['consumer-tenant-id'] = data['consumer-tenant-id'];
                this.data['consumer-epg-id'] = data['consumer-epg-id'];
                this.data['provider-tenant-id'] = data['provider-tenant-id'];
                this.data['provider-epg-id'] = data['provider-epg-id'];
                this.data['policy-rule-group-with-endpoint-constraints'] =
                    data['policy-rule-group-with-endpoint-constraints'];
            }


        }

        function createObject(data) {
            var obj = new ResolvedPolicy();

            if (data) {
                obj.setData(data);
            }

            return obj;
        }
    }

    return ResolvedPolicyService;
});
