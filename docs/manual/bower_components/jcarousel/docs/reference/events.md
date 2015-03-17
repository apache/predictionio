Events
======

After initialization, jCarousel triggers the following events on the root
and the items elements of the carousel:

* [Root element events](#root-element-events)
  * [create](#create)
  * [createend](#createend)
  * [reload](#reload)
  * [reloadend](#reloadend)
  * [destroy](#destroy)
  * [destroyend](#destroyend)
  * [scroll](#scroll)
  * [scrollend](#scrollend)
  * [animate](#animate)
  * [animateend](#animateend)
* [Item element events](#item-element-events)
  * [targetin](#targetin)
  * [targetout](#targetout)
  * [firstin](#firstin)
  * [firstout](#firstout)
  * [lastin](#lastin)
  * [lastout](#lastout)
  * [visiblein](#visiblein)
  * [visibleout](#visibleout)
  * [fullyvisiblein](#fullyvisiblein)
  * [fullyvisibleout](#fullyvisibleout)

**Note**: Some events are triggered from the constructor, so you have to bind
to the events **before** you initialize the plugin:

```javascript
$('.jcarousel')

    // Bind first
    .on('jcarousel:create', function(event, carousel) {
        // Do something
    })

    // Initialize at last step
    .jcarousel();
```

Root element events
===================

These events are triggered on the root element.


create
------

Triggered on creation of the carousel.

### Example

```javascript
$('.jcarousel').on('jcarousel:create', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


createend
---------

Triggered after creation of the carousel.

### Example

```javascript
$('.jcarousel').on('jcarousel:createend', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


reload
------

Triggered when the ``reload`` method is called.

### Example

```javascript
$('.jcarousel').on('jcarousel:reload', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


reloadend
---------

Triggered after the ``reload`` method is called.

### Example

```javascript
$('.jcarousel').on('jcarousel:reloadend', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


destroy
----------
Triggered when the ``destroy`` method is called.

### Example

```javascript
$('.jcarousel').on('jcarousel:destroy', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


destroyend
----------

Triggered after the ``destroy`` method is called.

### Example

```javascript
$('.jcarousel').on('jcarousel:destroyend', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


scroll
------

Triggered when the ``scroll`` method is called.

### Example

```javascript
$('.jcarousel').on('jcarousel:scroll', function(event, carousel, target, animate) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
    // "target" is the target argument passed to the `scroll` method
    // "animate" is the animate argument passed to the `scroll` method
    //      indicating whether jCarousel was requested to do an animation
});
```


scrollend
---------

Triggered after the ``scroll`` method is called.

**Note**: This method is triggered at the end of the scroll method and **not**
when the animation is finished.

### Example

```javascript
$('.jcarousel').on('jcarousel:scrollend', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


animate
-------

Triggered when the carousel starts a animation.

### Example

```javascript
$('.jcarousel').on('jcarousel:animate', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


animateend
----------

Triggered after the carousel has finished a animation.

### Example

```javascript
$('.jcarousel').on('jcarousel:animateend', function(event, carousel) {
    // "this" refers to the root element
    // "carousel" is the jCarousel instance
});
```


Item element events
===================

These events are triggered on the item elements. The recommended way is to bind
via delegated events:

```javascript
$('.jcarousel').on('jcarousel:targetin', 'li', function() {
    $(this).addClass('active');
});
```


targetin
--------

Triggered when the item becomes the targeted item.

### Example

```javascript
$('.jcarousel').on('jcarousel:targetin', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


targetout
---------

Triggered when the item is no longer the targeted item.

### Example

```javascript
$('.jcarousel').on('jcarousel:targetout', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


firstin
-------

Triggered when the item becomes the first visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:firstin', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


firstout
--------

Triggered when the item is no longer the first visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:firstout', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


lastin
------

Triggered when the item becomes the last visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:lastin', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


lastout
-------

Triggered when the item is no longer the last visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:lastout', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


visiblein
---------

Triggered when the item becomes a visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:visiblein', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```

visibleout
----------

Triggered when the item is no longer a visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:visibleout', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


fullyvisiblein
--------------

Triggered when the item becomes a fully visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:fullyvisiblein', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```


fullyvisibleout
---------------

Triggered when the item is no longer a fully visible item.

### Example

```javascript
$('.jcarousel').on('jcarousel:fullyvisibleout', 'li', function(event, carousel) {
    // "this" refers to the item element
    // "carousel" is the jCarousel instance
});
```
