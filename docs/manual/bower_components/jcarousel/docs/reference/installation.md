Installation
============

Include required files
----------------------

To use the jCarousel component, include the [jQuery](http://jquery.com)
library and the jCarousel source file into your HTML document:

```html
<script type="text/javascript" src="/path/to/jquery.js"></script>
<script type="text/javascript" src="/path/to/jquery.jcarousel.js"></script>
```

**Note:** The minimum required jQuery version is 1.7.

HTML markup
-----------

jCarousel expects a very basic HTML markup structure inside your HTML document:

```html
<div class="jcarousel">
    <ul>
        <li>...</li>
        <li>...</li>
    </ul>
</div>
```
--------------------------------------------------------------------------------

**Note:**

The documentation refers to the elements as **root** element, **list**
element and **item** element(s):

```text
<div class="jcarousel"> <--------------------------------| Root element
    <ul> <-------------------------------| List element  |
        <li>...</li> <---| Item element  |               |
        <li>...</li> <---| Item element  |               |
    </ul> <------------------------------|               |
</div> <-------------------------------------------------|
```

This structure is only an example and not required. You could also use a
structure like:

```text
<div class="mycarousel"> <-------------------------------| Root element
    <div> <------------------------------| List element  |
        <p>...</p> <-----| Item element  |               |
        <p>...</p> <-----| Item element  |               |
    </div> <-----------------------------|               |
</div> <-------------------------------------------------|
```

The only requirement is, that it consists of a root element, list element
and item elements.


Setup
-----

To setup the carousel, call the `.jcarousel()` plugin method on the root
element:

```javascript
$(function() {
    $('.jcarousel').jcarousel({
        // Configuration goes here
    });
});
```

See [Configuration](configuration.md) for all available configuration options.


Style the carousel
------------------

These are the minimal required CSS settings for a horizontal carousel:

```css
/*
This is the visible area of you carousel.
Set a width here to define how much items are visible.
The width can be either fixed in px or flexible in %.
Position must be relative!
*/
.jcarousel {
    position: relative;
    overflow: hidden;
}

/*
This is the container of the carousel items.
You must ensure that the position is relative or absolute and
that the width is big enough to contain all items.
*/
.jcarousel ul {
    width: 20000em;
    position: relative;

    /* Optional, required in this case since it's a <ul> element */
    list-style: none;
    margin: 0;
    padding: 0;
}

/*
These are the item elements. jCarousel works best, if the items
have a fixed width and height (but it's not required).
*/
.jcarousel li {
    /* Required only for block elements like <li>'s */
    float: left;
}
```
