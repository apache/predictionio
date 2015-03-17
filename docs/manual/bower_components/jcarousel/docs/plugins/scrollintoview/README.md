ScrollIntoView Plugin
=====================

The jCarousel ScrollIntoView Plugin extends the jCarousel core by a
`scrollIntoView` method.

Calling the method ensures that the passed target is fully visible inside the
carousel.

scrollIntoView
--------------

### Description

```javascript
scrollIntoView(target [, animate [, callback]])
```

Scrolls the carousel so that the targeted item is fully visible.
After scrolling, the item will be the first or last fully visible item.

### Arguments

See the arguments of the [scroll](../../reference/api.md#arguments) method.

### Example

```javascript
$('.jcarousel').jcarousel('scrollIntoView', 2, true, function(scrolled) {
    if (scrolled) {
        console.log('The carousel has been scrolled');
    } else {
        console.log('The carousel has not been scrolled');
    }
});
```
