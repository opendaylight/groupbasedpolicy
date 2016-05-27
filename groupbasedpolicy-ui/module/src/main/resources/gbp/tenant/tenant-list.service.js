define([], function () {
    'use strict';

    angular.module('app.gbp').service('TenantListService', TenantListService);

    TenantListService.$inject = ['Restangular', 'TenantService'];

    function TenantListService(Restangular, TenantService) {
        /* methods */
        this.createList = createList;

        function TenantList() {
            /* properties */
            this.data = [];
            /* methods */
            this.clearData = clearData;
            this.setData = setData;
            this.get = get;

            /* Implementation */

            function clearData() {
                var self = this;

                self.data = [];
            }

            /**
             * fills TenantList object with data
             * @param data
             */
            function setData(data) {
                var self = this;
                data.forEach(function (dataElement) {
                    self.data.push(TenantService.createObject(dataElement));
                });
            }

            function get(dataStore) {
                /* jshint validthis:true */
                var self = this;

                var restObj = Restangular.one('restconf').one(dataStore).one('policy:tenants');

                return restObj.get().then(function(data) {
                    if (data.tenants.tenant) {
                        self.setData(data.tenants.tenant);
                    }
                });
            }
        }

        function createList() {
            var obj = new TenantList();

            return obj;
        }
    }

    return TenantListService;
});
