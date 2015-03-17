API
===

After initialization, you can call the following methods on the jCarousel
instance:

* [Carousel-related methods](#carousel-related-methods)
  * [scroll](#scroll)
  * [reload](#reload)
  * [destroy](#destroy)
  * [list](#list)
* [Item-related methods](#item-related-methods)
  * [items](#items)
  * [target](#target-1)
  * [first](#first)
  * [last](#last)
  * [visible](#visible)
  * [fullyvisible](#fullyvisible)

See [Calling methods on the jCarousel instance](usage.md#calling-methods-on-the-jcarousel-instance)
for how to call methods.


Carousel-related methods
========================


scroll
------

### Description

```javascript
scroll(target [, animate [, callback]])
```

Scrolls to a specific item or relative by a given offset.

### Arguments

  * #### target

    The target item to which the carousel should scroll.

    ##### Available formats for the *target* argument:

    * ###### index

      Scrolls to the item at the given index (Note that indexes are 0-based).

      **Example:**

      ```javascript
      $('.jcarousel').jcarousel('scroll', 0);
      ```

    * ###### -index

      Scrolls to the item at the given index counting backwards from the end of
      the list.

      **Example:**

      ```javascript
      $('.jcarousel').jcarousel('scroll', -1);
      ````

    * ###### object

      Scrolls to the given object.

      **Example:**

      ```javascript
      $('.jcarousel').jcarousel('scroll', $('.jcarousel li:eq(2)'));
      ```

    * ###### +=offset

      Scrolls the carousel forward by the given offset relatively from the
      current position.

      **Example:**

      ```javascript
      $('.jcarousel').jcarousel('scroll', '+=1');
      ```

    * ###### -=offset

      Scrolls the carousel backwards by the given offset relatively from the
      current position.

      **Example:**

      ```javascript
      $('.jcarousel').jcarousel('scroll', '-=1');
      ````


  * #### animate

    If the argument `animate` is given and `false`, it just jumps to the
    position without animation.

  * #### callback

    If the argument `callback` is given and a valid function, it is called
    after the animation is finished.

    The function receives a boolean as first argument indicating if a scrolling
    actually happend.

    It can be false for the following reasons:

    * The carousel is already animating
    * The target argument is invalid
    * The carousel is already on the requested position
    * An event has cancelled the scrolling

### Example

```javascript
$('.jcarousel').jcarousel('scroll', '+=1', true, function(scrolled) {
    if (scrolled) {
        console.log('The carousel has been scrolled');
    } else {
        console.log('The carousel has not been scrolled');
    }
});
````


reload
------

### Description

```javascript
reload([options])
```

Reloads the carousel. This method is useful to reinitialize the carousel if
you have changed the content of the list from the outside or want to change
options that affect appearance of the carousel at runtime.

### Arguments

  * #### options

    A set of [configuration options](configuration.md).

### Example

```javascript
$('.jcarousel').jcarousel('reload', {
    animation: 'slow'
});
```


destroy
-------

### Description

```javascript
destroy()
```

Removes the jCarousel functionality completely. This will return the element
back to its initial state.

### Example

```javascript
$('.jcarousel').jcarousel('destroy');
```


list
----

### Description

```javascript
list()
```

Returns the list element as jQuery object.

### Example

```javascript
var list = $('.jcarousel').jcarousel('list');
```


Item-related methods
====================

**Note:** The item-related methods return different results depending on
the state of the carousel. That means for example, that after each scroll,
these methods return a different set of items.

The following example illustrates how to use these methods inside event
callbacks:

```javascript
$('.jcarousel')
    .on('jcarousel:animateend', function(event, carousel) {
        var currentFirstItem = $(this).jcarousel('first');
        var currentLastItem  = $(this).jcarousel('last');
    });
```


items
-----

### Description

```javascript
list()
```

Returns all items as jQuery object.

### Example

```javascript
var items = $('.jcarousel').jcarousel('items');
```


target
------

### Description

```javascript
target()
```

Returns the *targeted* item as jQuery object.

### Example

```javascript
var target = $('.jcarousel').jcarousel('target');
```


first
-----

### Description

```javascript
first()
```

Returns the *first visible* item as jQuery object.

### Example

```javascript
var first = $('.jcarousel').jcarousel('first');
```


last
----

### Description

```javascript
last()
```

Returns the *last visible* item as jQuery object.

### Example

```javascript
var last = $('.jcarousel').jcarousel('last');
```


visible
-------

### Description

```javascript
visible()
```

Returns all *visible* items as jQuery object.

### Example

```javascript
var visible = $('.jcarousel').jcarousel('visible');
```


fullyvisible
------------

### Description

```javascript
fullyvisible()
```

Returns all *fully visible* items as jQuery object.

### Example

```javascript
var fullyvisible = $('.jcarousel').jcarousel('fullyvisible');
```
