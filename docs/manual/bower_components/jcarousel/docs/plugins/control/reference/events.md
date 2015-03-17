Events
======

After initialization, the plugin triggers the following events on the element:

* [active](#active)
* [inactive](#inactive)
* [create](#create)
* [createend](#createend)
* [reload](#reload)
* [reloadend](#reloadend)
* [destroy](#destroy)
* [destroyend](#destroyend)

**Note**: Some events are triggered from the constructor, so you have to bind
to the events **before** you initialize the plugin:

```javascript
$('.jcarousel-control')

    // Bind first
    .on('jcarouselcontrol:active', function(event, carousel) {
        // Do something
    })

    // Initialize at last step
    .jcarouselControl();
```

--------------------------------------------------------------------------------
### How the plugin understands active and inactive states:

If the `target` option is *relative*, `+=1` for example, the control is active
if there is at least one more item to scroll, inactive otherwise (if you're at
the last item in this case).

If the `target` option is *absolute*, `0` for example (always scrolls to the
first item), the control is active if the targeted item is at position 0,
inactive otherwise.

--------------------------------------------------------------------------------


active
------

Triggered when the control becomes active.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:active', function() {
    // Do something
});
```


inactive
--------

Triggered when the control becomes inactive.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:inactive', function() {
    // Do something
});
```


create
------

Triggered on creation of the plugin.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:create', function() {
    // Do something
});
```


createend
---------

Triggered after creation of the plugin.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:createend', function() {
    // Do something
});
```


reload
------

Triggered when the `reload` method is called.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:reload', function() {
    // Do something
});
```


reloadend
---------

Triggered after the `reload` method is called.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:reloadend', function() {
    // "this" refers to the element
});
```


destroy
-------

Triggered when the `destroy` method is called.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:destroy', function() {
    // Do something
});
```


destroyend
----------

Triggered after the `destroy` method is called.

### Example

```javascript
$('.jcarousel-control').on('jcarouselcontrol:destroyend', function() {
    // Do something
});
```
