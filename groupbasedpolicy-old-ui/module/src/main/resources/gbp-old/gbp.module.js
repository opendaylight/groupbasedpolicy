define(['angularAMD', 'app/routingConfig', 'ui-bootstrap', 'Restangular', 'angular-translate'], function() {

  var gbpOld = angular.module('app.gbpOld', ['ui.router.state','app.core', 'ui.bootstrap', 'restangular', 'pascalprecht.translate']);

    gbpOld.register = gbpOld;

    gbpOld.config(function ($stateProvider, $compileProvider, $controllerProvider, $provide, $translateProvider, NavHelperProvider, $filterProvider) {

    $translateProvider.useStaticFilesLoader({
      prefix: 'assets/data/locale-',
      suffix: '.json'
    });

        gbpOld.register = {
      directive : $compileProvider.directive,
      controller : $controllerProvider.register,
      filter: $filterProvider.register,
      factory : $provide.factory,
      service : $provide.service
    };

    NavHelperProvider.addControllerUrl('app/gbp-old/gbp.controller');
    NavHelperProvider.addToMenu('gbpOld', {
      "link": "#/gbpOld/index",
      "active": "main.gbpOld",
      "title": "GBP old",
      "icon": "icon-level-down",
      "page": {
        "title": "GBP old",
        "description": "GBP old ui"
      }
    });

    var access = routingConfig.accessLevels;
      $stateProvider.state('main.gbpOld', {
          url: 'gbpOld',
          abstract: true,
          views : {
            'content' : {
              templateUrl: 'src/app/gbp-old/views/root.tpl.html'
            }
          }
      });

      $stateProvider.state('main.gbpOld.index', {
          url: '/index',
          access: access.admin,
          views: {
              '': {
                  controller: 'gbpCtrl',
                  templateUrl: 'src/app/gbp-old/views/index.tpl.html'
              }
          }
      });
  });

  return gbpOld;
});
