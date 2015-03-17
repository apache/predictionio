Configuration
=============

The plugin accepts the following options:

* [target](#target)
* [interval](#interval)
* [autostart](#autostart)

Options can be set either on [initialization](installation.md#setup) or at
[runtime](api.md#reload).


target
------

The target for the autoscrolling. This is basically the same as the first
argument the [scroll](../../../reference/api.md#scroll) method acceppts.

See [Available formats for the target argument](../../../reference/api.md#available-formats-for-the-target-argument)
for more information about the target argument.

### Example

```javascript
$('.jcarousel').jcarouselAutoscroll({
    target: '+=3'
});
```

### Default

`+=1`


interval
--------

The autoscrolling interval in milliseconds.

### Example

```javascript
$('.jcarousel').jcarouselAutoscroll({
    interval: 1000
});
```

### Default

`3000`


autostart
---------

Whether to autostart autoscrolling.

### Example

```javascript
$('.jcarousel').jcarouselAutoscroll({
    autostart: false
});
```

### Default

`true`
