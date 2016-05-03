define([
    'app/routingConfig',
    'Restangular',
    'angular-translate-loader-partial'], function () {

    var gbp = angular.module('app.gbp',
        [
            'app.core', 'ui.router.state', 'restangular'
        ]);

    gbp.register = gbp; // for adding services, controllers, directives etc. to angular module before bootstrap

    gbp.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider,
                         $translateProvider, $translatePartialLoaderProvider) {
        gbp.register = {
            controller: $controllerProvider.register,
            directive: $compileProvider.directive,
            factory: $provide.factory,
            service: $provide.service
        };

        //$translatePartialLoaderProvider.addPart('app/gbp/assets/data/locale');

        NavHelperProvider.addControllerUrl('app/gbp/gbp.controller');

        NavHelperProvider.addToMenu('gbp', {
            "link": "#/gbp/index",
            "active": "main.gbp",
            "title": "GBP",
            "icon": "icon-level-down",
            "page": {
                "title": "GBP",
                "description": "GBP ui"
            }
        });

        var access = routingConfig.accessLevels;

        $stateProvider.state('main.gbp', {
            url: 'gbp',
            abstract: true,
            // access: access.public,
            views: {
                'content': {
                    templateUrl: 'src/app/gbp/common/views/root.tpl.html',

                }
            }
        });

        $stateProvider.state('main.gbp.index', {
            url: '/index',
            access: access.admin,
            views: {
                '': {
                    controller: 'rootGbpCtrl',
                    templateUrl: 'src/app/gbp/common/views/index.tpl.html'
                }
            }
        });

        // TODO: serve it
        /*$stateProvider.state('main.gbp.tenant', {
            url: '/tenant',
            access: access.admin,
            views: {
                'gbp': {
                    controller: 'gbpTenantController',
                    controllerAs: 'tenantCtrl',
                    templateUrl: 'src/app/gbp/views/tenant.tpl.html'
                }
            }
        });*/

    });

    return gbp;
});
