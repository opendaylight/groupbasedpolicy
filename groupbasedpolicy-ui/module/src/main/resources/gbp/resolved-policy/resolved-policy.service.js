define([], function() {
    'use strict';

    angular.module('app.gbp').service('ResolvedPolicyService', ResolvedPolicyService);

    ResolvedPolicyService.$inject = ['Restangular'];

    function ResolvedPolicyService(Restangular) {
        /* methods */
        this.createObject = createObject;


        function ResolvedPolicy() {
            /* properties */
            this.data = {};
            /* methods */
            this.get = get;

            /* Implementation */

            function get(successCallback) {
                var self = this;

                var restObj = Restangular.one('restconf').one('operational').one('resolved-policy:resolved-policies');

                return restObj.get().then(function(data) {
                    self.data = data['resolved-policies']['resolved-policy'];
                    successCallback();
                });
            }
        }

        function createObject() {
            return new ResolvedPolicy();
        }
    }

    return ResolvedPolicyService;
});
