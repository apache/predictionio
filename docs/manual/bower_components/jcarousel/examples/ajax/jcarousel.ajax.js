(function($) {
    $(function() {
        var jcarousel = $('.jcarousel').jcarousel();

        $('.jcarousel-control-prev')
            .on('jcarouselcontrol:active', function() {
                $(this).removeClass('inactive');
            })
            .on('jcarouselcontrol:inactive', function() {
                $(this).addClass('inactive');
            })
            .jcarouselControl({
                target: '-=1'
            });

        $('.jcarousel-control-next')
            .on('jcarouselcontrol:active', function() {
                $(this).removeClass('inactive');
            })
            .on('jcarouselcontrol:inactive', function() {
                $(this).addClass('inactive');
            })
            .jcarouselControl({
                target: '+=1'
            });

        var setup = function(data) {
            var html = '<ul>';

            $.each(data.items, function() {
                html += '<li><img src="' + this.src + '" alt="' + this.title + '"></li>';
            });

            html += '</ul>';

            // Append items
            jcarousel
                .html(html);

            // Reload carousel
            jcarousel
                .jcarousel('reload');
        };

        $.getJSON('data.json', setup);
    });
})(jQuery);
