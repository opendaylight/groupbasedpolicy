define([], function () {
    'use strict';

    angular.module('app.gbp').service('RootGbpService', RootGbpService);

    RootGbpService.$inject = [];

    function RootGbpService() {
        this.setMainClass = setMainClass;

        /**
         * Sets gbpUiGlobalWrapper to override padding on parent class
         */
        function setMainClass(){
            if ($('.gbpUiWrapper').length) {
                $('.gbpUiWrapper').closest('.col-xs-12').addClass('gbpUiGlobalWrapper');
            }
        }
    }

    return RootGbpService;
});
