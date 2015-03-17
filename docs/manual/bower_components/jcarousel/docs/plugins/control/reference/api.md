API
===

After initialization, you can call the following methods on the plugin
instance:

* [reload](#reload)
* [destroy](#destroy)

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
$('.jcarousel-control').jcarouselControl('reload', {
    target: '+=3'
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
$('.jcarousel-control').jcarouselControl('destroy');
```
