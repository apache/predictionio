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

    <!-- Controls -->
    <a class="jcarousel-prev" href="#">Prev</a>
    <a class="jcarousel-next" href="#">Next</a>
</div>
```


Setup
-----

To setup the controls, call the `jcarouselControl()` plugin method on the
control elements after you have initialized the carousel:

```javascript
$(function() {
    $('.jcarousel').jcarousel({
        // Core configuration goes here
    });

    $('.jcarousel-prev').jcarouselControl({
        target: '-=1'
    });

    $('.jcarousel-next').jcarouselControl({
        target: '+=1'
    });
});
```

See [Configuration](configuration.md) for more information about the
configuration options.

As you can see, you setup the controls independently from the carousel and the
plugin tries to autodetect the carousel.

This works best if you enclose the carousel and its controls inside a mutual
wrapper element.

If that fails or isn't possible, you can pass the related carousel instance as
an option:

```javascript
var carousel = $('.jcarousel').jcarousel({
    // Core configuration goes here
});

$('.jcarousel-prev').jcarouselControl({
    target: '-=1',
    carousel: carousel
});
```
