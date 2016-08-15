var gulp = require('gulp'),
    del = require('del'),
    gutil = require('gulp-util'),
    concat = require('gulp-concat'),
    runSequence = require('run-sequence'),
    install = require("gulp-install");

var config = require( './build.config.js' );

/**
 * Task for cleaning build directory
 */
gulp.task('clean', function() {
    // You can use multiple globbing patterns as you would with `gulp.src`
    return del([config.build_dir]);
});

/**
 * Copy assets
 */
gulp.task('copyCss', function () {
    return gulp.src(config.assets_files.css)
        .pipe(gulp.dest(config.build_dir + '/common'));
});

/**
 * Copy app files
 */
gulp.task('copyTemplates', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Template files'));
    // Copy html
    return gulp.src(config.app_files.templates)
        .pipe(gulp.dest(config.build_dir));
});

gulp.task('copyAppJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Controller JS files'));
    return gulp.src(config.app_files.js)
        .pipe(gulp.dest(config.build_dir));
});

gulp.task('copyRootJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying APP Root JS files'));
    return gulp.src(config.app_files.root_js)
        .pipe(gulp.dest(config.build_dir));
});

/**
  * Copy vendor files
 */
gulp.task('copyVendorCss', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying VENDOR css'));
    return gulp.src(config.vendor_files.css, { cwd : 'vendor/**' })
        .pipe(gulp.dest(config.build_dir + '/vendor'))
});

gulp.task('copyVendorFonts', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying VENDOR fonts'));
    return gulp.src(config.vendor_files.fonts, { cwd : 'vendor/**' })
        .pipe(gulp.dest(config.build_dir + '/vendor'))
});

gulp.task('copyVendorJs', function () {
    gutil.log(gutil.colors.cyan('INFO :: copying VENDOR js files'));
    return gulp.src(config.vendor_files.js, { cwd : 'vendor/**' })
        .pipe(gulp.dest(config.build_dir + '/vendor'))
});


/**
 * Copy task aggregated
 */
gulp.task('copy', function() {
    runSequence([
        'copyCss',
        'copyTemplates',
        'copyAppJs',
        'copyRootJs',
        'copyVendorCss',
        'copyVendorFonts'
    ], 'copyVendorJs');
});

/**
 * Build task
 */
gulp.task('build', function(){
    runSequence('clean', 'copy');
});
