API
===

After initialization, you can call the following methods on the plugin
instance:

* [start](#start)
* [stop](#stop)
* [reload](#reload)
* [destroy](#destroy)


start
------

### Description

```javascript
start()
```

Starts the autoscrolling.

**Example:**

```javascript
$('.jcarousel').jcarouselAutoscroll('start');
```


stop
----

### Description

```javascript
stop()
```

Stops the autoscrolling.

**Example:**

```javascript
$('.jcarousel').jcarouselAutoscroll('stop');
```


reload
------

### Description

```javascript
reload([options])
```

Reloads the plugin. This method is useful to reinitialize the plugin or if you
want to change options at runtime.

### Arguments

  * #### options

    A set of [configuration options](configuration.md).

### Example

```javascript
$('.jcarousel').jcarouselAutoscroll('reload', {
    interval: 1500
});
```


destroy
------

### Description

```javascript
destroy()
```

Removes the plugin functionality completely. This will return the element back
to its initial state.

### Example

```javascript
$('.jcarousel').jcarouselAutoscroll('destroy');
```
