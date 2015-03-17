How to create a responsive carousel
===================================

Responsive carousels are quite easy to implement with jCarousel.

We simply listen to the `create` and `reload` events to resize each item to fit
in the viewport.

```javascript
$('.jcarousel')
    .on('jcarousel:create jcarousel:reload', function() {
        var element = $(this),
            width = element.innerWidth();

        // This shows 1 item at a time.
        // Divide `width` to the number of items you want to display,
        // eg. `width = width / 3` to display 3 items at a time.
        element.jcarousel('items').css('width', width + 'px');
    })
    .jcarousel({
        // Your configurations options
    });
```

You can also display a different number of items depending on the container
size.

```javascript
$('.jcarousel')
    .on('jcarousel:create jcarousel:reload', function() {
        var element = $(this),
            width = element.innerWidth();

        if (width > 900) {
            width = width / 3;
        } else if (width > 600) {
            width = width / 2;
        }

        element.jcarousel('items').css('width', width + 'px');
    })
    .jcarousel({
        // Your configurations options
    });
```

If you are displaying images in your carousel, make sure they are
responsive-friendly and scale nicely to the parent element.

```css
.jcarousel img {
    display: block;
    max-width: 100%;
    height: auto !important;
}
```
