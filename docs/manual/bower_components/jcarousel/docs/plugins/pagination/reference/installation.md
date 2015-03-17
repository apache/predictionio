Installation
============

HTML markup
-----------

A simple basic HTML markup structure would be:

```html
<!-- Wrapper -->
<div>
    <!-- Carousel -->
    <div class="jcarousel">
        <ul>
            <li>...</li>
            <li>...</li>
        </ul>
    </div>

    <!-- Pagination -->
    <div class="jcarousel-pagination">
        <!-- Pagination items will be generated in here -->
    </div>
</div>
```


Setup
-----

To setup the pagination, call the `jcarouselPagination()` plugin method on the
pagination element after you have initialized the carousel:

```javascript
$(function() {
    $('.jcarousel').jcarousel({
        // Core configuration goes here
    });

    $('.jcarousel-pagination').jcarouselPagination({
        item: function(page) {
            return '<a href="#' + page + '">' + page + '</a>';
        }
    });
});
```

See [Configuration](configuration.md) for more information about the
configuration options.

As you can see, you setup the pagination independently from the carousel and the
plugin tries to autodetect the carousel.

This works best if you enclose the carousel and its pagination inside a mutual
wrapper element.

If that fails or isn't possible, you can pass the related carousel instance as
an option:

```javascript
var carousel = $('.jcarousel').jcarousel({
    // Core configuration goes here
});

$('.jcarousel-pagination').jcarouselPagination({
    carousel: carousel
});
```
