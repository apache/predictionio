API
===

After initialization, you can call the following methods on the plugin
instance:

* [reload](#reload)
* [destroy](#destroy)
* [reloadCarouselItems](#reloadcarouselitems)


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
$('.jcarousel-pagination').jcarouselPagination('reload', {
    'perPage': 3
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
$('.jcarousel-pagination').jcarouselPagination('destroy');
```

reloadCarouselItems
-------------------

### Description

```javascript
reloadCarouselItems()
```

Reloads the carousel items. Call this method after you have added/removed items
from the carousel.

### Example

```javascript
$('.jcarousel-pagination').jcarouselPagination('reloadCarouselItems');
```
