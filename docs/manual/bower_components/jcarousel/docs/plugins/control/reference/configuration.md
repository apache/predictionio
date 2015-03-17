Configuration
=============

The plugin accepts the following options:

* [target](#target)
* [event](#event)
* [method](#method)
* [carousel](#carousel)

Options can be set either on [initialization](installation.md#setup) or at
[runtime](api.md#reload).


target
------

The target for the control. This is basically the same as the first argument
the [scroll](../../../reference/api.md#scroll) method acceppts.

See [Available formats for the target argument](../../../reference/api.md#available-formats-for-the-target-argument)
for more information about the target argument.

### Example

```javascript
$('.jcarousel-control').jcarouselControl({
    target: '+=3'
});
```

### Default

`+=1`


event
-----

The event which triggers the control.

### Example

```javascript
$('.jcarousel-control').jcarouselControl({
    event: 'mouseover'
});
````

### Default

`click`


method
------

The method to call on the carousel.

Plugins may provide alternate methods for scrolling the carousel, e.g.
[scrollIntoView](../../scrollintoview/).

### Example

```javascript
$('.jcarousel-control').jcarouselControl({
    method: 'scrollIntoView'
});
```

Alternatively, `method` can be a function which will be called in the context of
the plugin instance.

### Example

```javascript
$('.jcarousel-control').jcarouselControl({
    method: function() {
        this.carousel()
            .jcarousel('scroll', false, this.options('target'), function() {
                // Do something
            });
    }
});
```

### Default

`scroll`


carousel
--------

The corresponding carousel as jQuery object.

This is optional. By default, the plugin tries to autodetect the carousel.

### Example

```javascript
$('.jcarousel-control').jcarouselControl({
    carousel: $('.jcarousel')
});
```

### Default

`null`
