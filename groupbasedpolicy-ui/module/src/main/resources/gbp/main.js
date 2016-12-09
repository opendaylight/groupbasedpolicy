require.config({
    paths: {
        'angular-material': 'app/gbp/vendor/angular-material/angular-material.min',
        'angular-animate': 'app/gbp/vendor/angular-animate/angular-animate.min',
        'angular-aria': 'app/gbp/vendor/angular-aria/angular-aria.min',
        'angular-material-data-table': 'app/gbp/vendor/angular-material-data-table/dist/md-data-table.min',
        'angular-messages': 'app/gbp/vendor/angular-messages/angular-messages.min',
        'next-ui': 'app/gbp/vendor/NeXt/js/next.min',
    },
    shim: {
        'angular-material': ['angular'],
        'angular-animate': ['angular'],
        'angular-aria': ['angular'],
        'angular-material-data-table': ['angular', 'angular-material'],
        'angular-messages': ['angular'],
    },
});

define(['app/gbp/common/gbp.module']);
