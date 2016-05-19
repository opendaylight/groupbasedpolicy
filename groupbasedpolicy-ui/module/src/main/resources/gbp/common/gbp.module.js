define([
    'app/routingConfig',
    'Restangular',
    'angular-translate-loader-partial',
    'angular-animate',
    'angular-aria',
    'angular-material',
    'angular-material-data-table',
    'angular-messages'], function () {

    var gbp = angular.module('app.gbp',
        [
            'app.core', 'ui.router.state', 'restangular', 'ngAnimate', 'ngAria', 'ngMaterial', 'md.data.table', 'ngMessages'
        ]);

    gbp.register = gbp; // for adding services, controllers, directives etc. to angular module before bootstrap

    gbp.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, NavHelperProvider,
                         $translateProvider, $translatePartialLoaderProvider, $mdThemingProvider) {
        gbp.register = {
            controller: $controllerProvider.register,
            directive: $compileProvider.directive,
            factory: $provide.factory,
            service: $provide.service,
        };

        /*$translatePartialLoaderProvider.addPart('app/gbp/assets/data/locale');*/

        NavHelperProvider.addControllerUrl('app/gbp/common/gbp.controller');
        NavHelperProvider.addControllerUrl('app/gbp/contract/contract.controller');
        NavHelperProvider.addControllerUrl('app/gbp/epg/epg.controller');
        NavHelperProvider.addControllerUrl('app/gbp/policy/policy.controller');
        NavHelperProvider.addControllerUrl('app/gbp/tenant/add-tenant.controller');
        NavHelperProvider.addControllerUrl('app/gbp/tenant/tenant.controller');

        NavHelperProvider.addToMenu('gbp', {
            'link': '#/gbp/index',
            'active': 'main.gbp',
            'title': 'GBP',
            'icon': 'icon-level-down',
            'page': {
                'title': 'GBP',
                'description': 'GBP ui'
            }
        });

        var access = routingConfig.accessLevels;

        $stateProvider.state('main.gbp', {
            url: 'gbp',
            abstract: true,
            // access: access.public,
            views: {
                'content': {
                    templateUrl: 'src/app/gbp/common/views/root.tpl.html'

                },
            },
        });

        $stateProvider.state('main.gbp.index', {
            url: '/index',
            access: access.admin,
            views: {
                '': {
                    controller: 'RootGbpCtrl',
                    templateUrl: 'src/app/gbp/common/views/index.tpl.html'
                },
            },
        });

        $stateProvider.state('main.gbp.index.tenant', {
            url: '/tenant',
            access: access.admin,
            templateUrl: 'src/app/gbp/common/views/index.tpl.html',
            views: {
                '': {
                    controller: 'TenantController',
                    templateUrl: 'src/app/gbp/tenant/tenant.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.index.policy', {
            url: '/policy',
            access: access.admin,
            templateUrl: 'src/app/gbp/common/views/index.tpl.html',
            views: {
                'main_top': {
                    controller: 'EpgController',
                    templateUrl: 'src/app/gbp/epg/epg.tpl.html',
                },
                'main_bottom': {
                    controller: 'ContractController',
                    templateUrl: 'src/app/gbp/contract/contract.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.index.policy.epg', {
            url: '/epg',
            access: access.admin,
            templateUrl: 'src/app/gbp/common/views/index.tpl.html',
            views: {
                '': {
                    controller: 'PolicyController',
                    templateUrl: 'src/app/gbp/policy/policy.tpl.html',
                },
                'sidePanel': {
                    controller: 'EpgController',
                    templateUrl: 'src/app/gbp/epg/epg.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.index.contract', {
            url: '/policy/contract',
            access: access.admin,
            templateUrl: 'src/app/gbp/common/views/index.tpl.html',
            views: {
                '': {
                    controller: 'ContractController',
                    templateUrl: 'src/app/gbp/contract/contract.tpl.html',
                },
            },
        });

        $stateProvider.state('main.gbp.index.contractId', {
            url: '/policy/contract/{contractId}',
            access: access.admin,
            templateUrl: 'src/app/gbp/common/views/index.tpl.html',
            views: {
                '': {
                    controller: 'ContractController',
                    templateUrl: 'src/app/gbp/contract/contract.tpl.html',
                },
            },
        });

        $mdThemingProvider.theme('default')
            .primaryPalette('blue')
            .accentPalette('blue-grey');
    });

    return gbp;
});
