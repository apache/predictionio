Installation
============

To setup autoscrolling, just call `jcarouselAutoscroll()` on the carousel root
element:

```javascript
$(function() {
    $('.jcarousel')
        .jcarousel({
            // Core configuration goes here
        })
        .jcarouselAutoscroll({
            interval: 3000,
            target: '+=1',
            autostart: true
        })
    ;
});
```

See [Configuration](configuration.md) for more information about the
configuration options.
