Configuration
=============

The plugin accepts the following options:

* [perPage](#perpage)
* [item](#item)
* [carousel](#carousel)

Options can be set either on [initialization](installation.md#setup) or at
[runtime](api.md#reload).


perPage
-------

The number of carousel items per page or a function returning the number.

If `perPage` is not set or `null`, the plugin calculates the pages depending
on the number of visible carousel items.

### Example

```javascript
$('.jcarousel-pagination').jcarouselPagination({
    'perPage': 3
});
```

### Default

`null`


item
----

A function returning the markup for a page item of the pagination either as
string or jQuery object.

The function will be called in the context of the plugin instance and
receives two arguments:

1. The page number.
2. A jQuery object containing the carousel items visible on this page.

### Example

```javascript
$('.jcarousel-pagination').jcarouselPagination({
    'item': function(page, carouselItems) {
        return '<li><a href="#' + page + '">Page ' + page + '</a></li>';
    }
});
```

### Default

```javascript
function(page, carouselItems) {
    return '<a href="#' + page + '">' + page + '</a>';
}
```

carousel
--------

The corresponding carousel as jQuery object.

This is optional. By default, the plugin tries to autodetect the carousel.

### Example

```javascript
$('.jcarousel-pagination').jcarouselPagination({
    'carousel': $('.jcarousel')
});
```

### Default

``null``
