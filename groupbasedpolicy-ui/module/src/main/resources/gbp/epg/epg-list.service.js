define([], function() {
    'use strict';

    angular.module('app.gbp').service('EpgListService', EpgListService);

    EpgListService.$inject = ['Restangular', 'EpgService'];

    function EpgListService(Restangular, EpgService) {
        /* methods */
        this.createList = createList;

        function EpgList() {
            /* properties */
            this.data = [];
            /* methods */
            this.setData = setData;
            this.get = get;

            /* Implementation */
            /**
             * fills EpgList object with data
             * @param data
             */
            function setData(data) {
                var self = this;
                data.forEach(function(dataElement) {
                    self.data.push(EpgService.createObject(dataElement));
                });
            }

            function get(dataStore, idTenant) {
                /* jshint validthis:true */
                var self = this;

                var restObj = Restangular.one('restconf').one(dataStore).one('policy:tenants').one('tenant')
                    .one(idTenant).one('policy');

                return restObj.get().then(function(data) {
                    if (data.policy['endpoint-group']) {
                        self.setData(data.policy['endpoint-group']);
                    }
                });
            }
        }

        function createList() {
            var obj = new EpgList();

            return obj;
        }
    }

    return EpgListService;
});