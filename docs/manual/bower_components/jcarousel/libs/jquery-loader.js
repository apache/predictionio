(function() {
    // Default path.
    var path = '../../libs/jquery/jquery.js';

    // From: https://github.com/ckeditor/ckeditor-dev/blob/master/core/ckeditor_base.js#L115 
    var scripts = document.getElementsByTagName('script');

    for (var i = 0; i < scripts.length; i++) {
        var match = scripts[i].src.match(/(^|.*[\\\/])jquery\-loader.js(?:\?.*)?$/i);

        if (match) {
            path = match[1] + 'jquery/jquery.js';
            break;
        }
    }

    if (path.indexOf(':/') === -1) {
        if (path.indexOf('/') === 0) {
            path = location.href.match(/^.*?:\/\/[^\/]*/)[ 0 ] + path;
        } else {
            path = location.href.match(/^[^\?]*\/(?:)/)[ 0 ] + path;
        }
    }

    // Get any jquery=___ param from the query string.
    var jqversion = location.search.match(/[?&]jquery=(.*?)(?=&|$)/);
    // If a version was specified, use that version from code.jquery.com.
    if (jqversion) {
        path = 'http://code.jquery.com/jquery-' + jqversion[1] + '.js';
    }
    // This is the only time I'll ever use document.write, I promise!
    document.write('<script src="' + path + '"></script>');
}());
