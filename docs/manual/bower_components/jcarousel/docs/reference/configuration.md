Configuration
=============

jCarousel accepts the following options:

* [list](#list)
* [items](#items)
* [animation](#animation)
* [transitions](#transitions)
* [wrap](#wrap)
* [vertical](#vertical)
* [rtl](#rtl)
* [center](#center)

Options can be set either on [initialization](installation.md#initialize-jcarousel)
or at [runtime](api.md#reload).


list
----

A function or a jQuery selector to select the list inside the root element.

A function will be called in the context of the carousel instance.

### Example

```javascript
$('.jcarousel').jcarousel({
    list: '.jcarousel-list'
});
```

### Default

```javascript
function() {
    return this.element().children().eq(0);
}
```


items
-----

A function or a jQuery selector to select the items inside the list element.

A function will be called in the context of the carousel instance.

### Example

```javascript
$('.jcarousel').jcarousel({
    items: '.jcarousel-item'
});
```

### Default

```javascript
function() {
    return this.list().children();
}
```


animation
---------

The speed of the scroll animation as string in jQuery terms (`"slow"` or
`"fast"`) or milliseconds as integer (See the documentation for
[jQuery animate](http://api.jquery.com/animate>)).

Alternatively, this can be a map of options like the one [jQuery.animate]
(http://api.jquery.com/animate/#animate-properties-options>) accepts as second
argument.

### Example

```javascript
$('.jcarousel').jcarousel({
    animation: 'slow'
});

$('.jcarousel').jcarousel({
    animation: {
        duration: 800,
        easing:   'linear',
        complete: function() {
        }
    }
});
```

### Default

`400`


transitions
-----------

If set to `true`, CSS3 transitions are used for animations.

Alternatively, this can be a map of the following options:

  * `transforms`:
    If set to `true`, 2D transforms are used for better hardware acceleration.
  * `transforms3d`:
    If set to `true`, 3D transforms are used for full hardware acceleration.
  * `easing`:
    Value will be used as the [transition-timing-function]
    (https://developer.mozilla.org/docs/CSS/transition-timing-function>)
    (e.g. `ease` or `linear`).

--------------------------------------------------------------------------------

**Note:**

jCarousel does **not** check if the user's browser supports transitions
and/or transforms. You have to do that yourself when setting the option.

You can for example use [Modernizr](http://modernizr.com>) for browser feature
detection. If you're not including it already in your site, you can use this
[minimal build](http://modernizr.com/download/#-csstransforms-csstransforms3d-csstransitions-teststyles-testprop-testallprops-prefixes-domprefixes).

--------------------------------------------------------------------------------

### Example

```javascript
$('.jcarousel').jcarousel({
    transitions: true
});

$('.jcarousel').jcarousel({
    transitions: Modernizr.csstransitions ? {
        transforms:   Modernizr.csstransforms,
        transforms3d: Modernizr.csstransforms3d,
        easing:       'ease'
    } : false
});
```

### Default

`false`


wrap
----

Specifies whether to wrap at the first or last item (or both) and jump back
to the start/end. Options are `"first"`, `"last"`, `"both"` or `"circular"` as
string.

If set to null, wrapping is turned off (default).

### Example

```javascript
$('.jcarousel').jcarousel({
    wrap: 'both'
});
```

### Default

`null`


vertical
--------

Specifies whether the carousel appears in vertical orientation. Changes the
carousel from a left/right style to a up/down style carousel.

If set to `null`, jCarousel tries to auto-detect the orientation by simply
checking if the list's height is greater than the list's width.

### Example

```javascript
$('.jcarousel').jcarousel({
    vertical: true
});
```

### Default

`null`


rtl
---

Specifies wether the carousel appears in RTL (Right-To-Left) mode.

If set to `null`, jCarousel looks for `dir="rtl"` attribute on the root
element (or to any of its parent elements) and if found, automatically sets
rtl to true.

### Example

```javascript
$('.jcarousel').jcarousel({
    rtl: true
});
```

**Example with dir attribute:**

```html
<div class="jcarousel" dir="rtl">
    <ul>
        <li>...</li>
    </ul>
</div>

<script>
$('.jcarousel').jcarousel();
</script>
```

### Default

`null`


center
------

Specifies wether the targeted item should be centered inside the root element.

**Note:** This feature is **experimental** and may not work with all setups.


### Example

```javascript
$('.jcarousel').jcarousel({
    center: true
});
```

### Default

`false`
