Usage
=====

* [Calling methods on the jCarousel instance](#calling-methods-on-the-jcarousel-instance)
* [Navigating the carousel](#navigating-the-carousel)
* [Defining the number of visible items](#defining-the-number-of-visible-items)
* [Vertical carousels](#vertical-carousels)
* [RTL (Right-To-Left) carousels](#rtl-right-to-left-carousels)
* [Manipulating the carousel](#manipulating-the-carousel)


Calling methods on the jCarousel instance
-----------------------------------------

If you have created a carousel like:

```javascript
$(function() {
    $('.jcarousel').jcarousel();
});
```

You can later call methods on the jCarousel instance like:

```javascript
$('.jcarousel').jcarousel('scroll', '+=2');
```

The first argument is the method name. The following arguments are the arguments
for the called method.

Alternatively, you can first grab the jCarousel instance and call the methods
directly on it:

```javascript
var instance = $('.jcarousel').data('jcarousel');

instance.scroll('+=2');
```

See [API](api.md) for all available methods.


Navigating the carousel
-----------------------

jCarousel offers no built in controls to navigate through the carousel. But you
can simply implement navigation controls using the [scroll](api.md#scroll)
method.

```javascript
$('.jcarousel').jcarousel('scroll', target);
```

A simple example for previous and next controls:

```javascript
$('.jcarousel-prev').click(function() {
    $('.jcarousel').jcarousel('scroll', '-=1');
});

$('.jcarousel-next').click(function() {
    $('.jcarousel').jcarousel('scroll', '+=1');
});
```

A more comfortable way is to use a navigation plugin:

   * [Control Plugin](../plugins/control/)
   * [Pagination Plugin](../plugins/pagination/)


Defining the number of visible items
------------------------------------

You simply define the number of visible items by defining the width (or height
for a vertical carousel) of the root element (if you use the default from this
document, you do that with the class `.jcarousel` in your stylesheet).

This offers a lot of flexibility, because you can define the width in pixel for
a fixed carousel or in percent for a flexible carousel.

### Fixed carousel

Always 3 visible items:

```css
.jcarousel {
    position: relative;
    overflow: hidden;
    width: 300px;
}

.jcarousel li {
    float: left;
    width: 100px;
}
```

### Flexible carousel

The number of visible items depend on the width of the root's parent element:

```css
.jcarousel {
    position: relative;
    overflow: hidden;
    width: 100%;
}

.jcarousel li {
    float: left;
    width: 100px;
}
```


Vertical carousels
------------------

jCarousel tries to auto-detect the orientation by simply checking if the list
elements’s height is greater than the list element’s width.

If auto-detection doesn't work, you can explicitly pass the `vertical` option:

```javascript
$('.jcarousel').jcarousel({
    vertical: true
});
```


RTL (Right-To-Left) carousels
-----------------------------

jCarousel tries to auto-detect if the carousel should run in RTL mode by looking
for a `dir` attribute with the value `rtl` on the root or any of its parent
elements.

```html
<div class="jcarousel" dir="rtl">
    <ul>
        <!-- The content goes in here -->
    </ul>
</div>
```

If auto-detection doesn't work, you can explicitly pass the `rtl` option.
**Note:** The `dir="rtl"` attribute is still required!

```javascript
$('.jcarousel').jcarousel({
    rtl: true
});
```

When running a carousel in RTL mode, you should ensure to float the items to the
right:

```css
.jcarousel[dir=rtl] li {
    float: right;
}
```


Manipulating the carousel
-------------------------

If you manipulate the carousel from the outside (eg. adding or removing items
from the list), ensure that you call the [reload](api.md) method afterwards so
that jCarousel becomes aware of the changes:

```javascript
$(function() {
    $('.jcarousel').jcarousel({
        // Configuration goes here
    });

    // Append items
    $('.jcarousel ul')
        .append('<li>Item 1</li>')
        .append('<li>Item 2</li>');

    // Reload carousel
    $('.jcarousel').jcarousel('reload');
});
```

Existing items should only be manipulated, not completely replaced:

```javascript
$(function() {
    // Don't do that
    $('.jcarousel li:eq(0)')
        .replaceWith('<li class="myclass">Item 1</li>');

    // Do this
    $('.jcarousel li:eq(0)')
        .addClass('myclass')
        .text('Item 1');
});
```

If you are removing items, make sure they are currently not visible:

```javascript
$(function() {
    var carousel = $('.jcarousel'),
        item = carousel.find('li:eq(0)');

    if (carousel.jcarousel('visible').index(item) < 0) {
        item.remove();
        carousel.jcarousel('reload');
    }
});
```
