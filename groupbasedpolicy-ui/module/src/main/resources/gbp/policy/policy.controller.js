define([], function () {
    'use strict';

    angular.module('app.gbp').controller('PolicyController', PolicyController);

    PolicyController.$inject = ['$scope'];

    /* @ngInject */
    function PolicyController($scope) {
        console.log('PolicyController initialized');
    }

});

