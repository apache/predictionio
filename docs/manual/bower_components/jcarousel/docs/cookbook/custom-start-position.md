How to define a custom start position
=====================================

Sometimes, you don't want to start your carousel at the first item but define a
custom start position.

This can be achieved in two ways: Either by listening to the `createend` event
or by defining the start position via CSS.

Setting the start position through an event
-------------------------------------------

You can set the start position by listening to the `createend` event and
calling the `scroll` method:

```javascript
$('.jcarousel')
    .on('jcarousel:createend', function() {
        // Arguments:
        // 1. The method to call
        // 2. The index of the item (note that indexes are 0-based)
        // 3. A flag telling jCarousel jumping to the index without animation
        $(this).jcarousel('scroll', 2, false);
    })
    .jcarousel();
```

Setting the start position through CSS
--------------------------------------

You can also set the initial position of the list through CSS and jCarousel will
figure out which item to use as the start item:

```css
.jcarousel ul {
    /* ...other styles left out... */
    left: -150px;
}
```

Assuming your items have a width of `75px`, the start item will be the third
item.
