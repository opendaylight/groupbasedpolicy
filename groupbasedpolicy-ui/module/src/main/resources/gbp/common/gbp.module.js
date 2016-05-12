define([
    'app/routingConfig',
    'Restangular',
    'angular-translate-loader-partial'], function () {

    var gbp = angular.module('app.gbp',
        [
            'app.core', 'ui.router.state', 'restangular',
        ]);

    gbp.register = gbp; // for adding services, controllers, directives etc. to angular module before bootstrap

    gbp.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider,
                         $translateProvider, $translatePartialLoaderProvider) {
        gbp.register = {
            controller: $controllerProvider.register,
            directive: $compileProvider.directive,
            factory: $provide.factory,
            service: $provide.service,
        };

        /*$translatePartialLoaderProvider.addPart('app/gbp/assets/data/locale');*/

        NavHelperProvider.addControllerUrl('app/gbp/contract/contract.controller');
        NavHelperProvider.addControllerUrl('app/gbp/epg/epg.controller');
        NavHelperProvider.addControllerUrl('app/gbp/common/gbp.controller');
        NavHelperProvider.addControllerUrl('app/gbp/tenant/tenant.controller');

        NavHelperProvider.addToMenu('gbp', {
            'link': '#/gbp/index',
            'active': 'main.gbp',
            'title': 'GBP',
            'icon': 'icon-level-down',
            'page': {
                'title': 'GBP',
                'description': 'GBP ui',
            },
        });

        var access = routingConfig.accessLevels;

        $stateProvider.state('main.gbp', {
            url: 'gbp',
            abstract: true,
            // access: access.public,
            views: {
                'content': {
                    templateUrl: 'src/app/gbp/common/views/root.tpl.html',

                },
            },
        });

        $stateProvider.state('main.gbp.index', {
            url: '/index',
            access: access.admin,
            views: {
                '': {
                    controller: 'RootGbpCtrl',
                    templateUrl: 'src/app/gbp/common/views/index.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.tenant', {
            url: '/tenant',
            access: access.admin,
            views: {
                '': {
                    controller: 'TenantController',
                    templateUrl: 'src/app/gbp/tenant/tenant.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.epg', {
            url: '/epg',
            access: access.admin,
            views: {
                '': {
                    controller: 'EpgController',
                    templateUrl: 'src/app/gbp/epg/epg.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.contract', {
            url: '/contract',
            access: access.admin,
            views: {
                '': {
                    controller: 'ContractController',
                    templateUrl: 'src/app/gbp/contract/contract.tpl.html',
                },
            },
        });

    });

    return gbp;
});
