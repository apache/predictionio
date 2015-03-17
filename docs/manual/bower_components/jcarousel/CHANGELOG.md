jCarousel Changelog
===================

Version 0.3.3 - 2015-02-28
--------------------------

  * Fixed hasNext() for centered carousels (#746)
  * Cleaned up .gitattributes to include package manager files

Version 0.3.2 - 2015-02-23
--------------------------

  * Fixed page calculation in pagination plugin (#617)
  * Fixed incompatibility with jQuery < 1.9 (#676)

Version 0.3.1 - 2014-04-26
--------------------------

  * Fixed hasNext/hasPrev in underflow mode
  * Fixed wrong page calculation on reload for pagination plugin (#660)
  * Added new method `reloadCarouselItems` to pagination plugin
  * Added component.json

Version 0.3.0 - 2013-11-22
--------------------------

  * Stable release of the completely rewritten plugin.

Version 0.3.0-rc.1 - 2013-11-12
--------------------------

  * First release candidate of the completely rewritten plugin.

Version 0.2.9 - 2013-04-19
--------------------------

  * Made compatible with jQuery 1.9.
  * Updated textscroller example and removed possible remote inclusion vulnerability.

Version 0.2.8 - 2011-04-14
--------------------------

  * Fixed selecting only direct childs of the current list (#61).
  * Added static method to set windowLoaded to true manually (#60).
  * Added setupCallback.
  * Optimized resize callback.
  * Added animationStepCallback option (Thanks [scy](https://github.com/scy)).
  * Wider support of border-radius, and support of :focus in addition to :hover (Thanks [lespacedunmatin](https://github.com/lespacedunmatin)).

Version 0.2.7 - 2010-10-06
--------------------------

  * Fixed bug with autoscrolling introduced while fixing #49.

Version 0.2.6 - 2010-10-05
--------------------------

  * Fixed item only partially visible when defined as start item (#22).
  * Fixed multiple binds on prev/next buttons (#26).
  * Added firing of button callbacks also if no buttons are specified (#39).
  * Fixed stopAuto() not stopping while in animation (#49).

Version 0.2.5 - 2010-08-13
--------------------------

  * Added RTL (Right-To-Left) support.
  * Added automatic deletion of cloned elements for circular carousels.
  * Added new option `itemFallbackDimension` (#7).
  * Added new section "Defining the number of visible items" to documentation.

Version 0.2.4 - 2010-04-19
--------------------------

  * Updated jQuery to version 1.4.2.
  * jCarousel instance can now be retrieved by `$(selector).data('jcarousel')`.
  * Support for static circular carousels out of the box.
  * Removed not longer needed core stylsheet jquery.jcarousel.css. Styles are now set by javascript or skin stylesheets.

Version 0.2.3 - 2008-04-07
--------------------------

  * Updated  jQuery to version 1.2.3.
  * Fixed (hopefully) issues with Safari.
  * Added new example "Multiple carousels on one page".

Version 0.2.2 - 2007-11-07
--------------------------

  * Fixed bug with nested li elements reported by John Fiala.
  * Fixed bug on initialization with too few elements reported by [Glenn Nilsson](http://groups.google.com/group/jquery-en/browse_thread/thread/455edd3814bf2d9c/86f35001bb483024).

Version 0.2.1 - 2007-10-12
--------------------------

  * Readded the option `start` for a custom start position. The old option `start` is renamed to `offset`.
  * New example for dynamic content loading via Ajax from a PHP script.
  * Fixed a bug with variable item widths/heights.

Version 0.2.0-beta - 2007-05-07
--------------------------

  * Complete rewrite of the plugin.

Version 0.1.6 - 2007-01-22
--------------------------

  * New public methods `size()` and `init()`.
  * Added new example "Carousel with external controls".

Version 0.1.5 - 2007-01-08
--------------------------

  * Code modifications to work with the jQuery 1.1.
  * Renamed the js file to jquery.jcarousel.pack.js as noted in the jquery docs.

Version 0.1.4 - 2006-12-12
--------------------------

  * New configuration option `autoScrollResumeOnMouseout`.

Version 0.1.3 - 2006-12-02
--------------------------

  * New configuration option `itemStart`. Sets the index of the item to start with.

Version 0.1.2 - 2006-11-28
--------------------------

  * New configuration option `wrapPrev`. Behaves like `wrap` but scrolls to the end when clicking the prev-button at the start of the carousel. (Note: This may produce unexpected results with dynamic loaded content. You must ensure to load the complete carousel on initialisation).
  * Moved call of the button handlers at the end of the buttons() method. This lets the callbacks change the behaviours assigned by jCarousel.

Version 0.1.1 - 2006-10-25
--------------------------

  * The item handler callback options accept now a hash of two functions which are triggered before and after animation.
  * The item handler callback functions accept now a fourth parameter `state` which holds one of three states: `next`, `prev` or `init`.
  * New configuration option `autoScrollStopOnMouseover`

Version 0.1.0 - 2006-09-21
--------------------------

  * Stable release.
  * Internal source code rewriting to fit more into the jQuery plugin guidelines.
  * Added inline documentation.
  * Changed licence to a dual licence model (<a href="http://www.opensource.org/licenses/mit-license.php">MIT</a> and <a href="http://www.opensource.org/licenses/gpl-license.php">GPL</a>).

Version 0.1.0-RC1 - 2006-09-13
--------------------------

  * Virtual item attribute `jCarouselItemIdx` is replaced by a class `jcarousel-item-<em>n</em>`.
  * The item callback functions accept a third parameter `idx` which holds the position of the item in the list (formerly the attribute `jCarouselItemIdx`).
  * Fixed bug with margin-right in Safari.

Version 0.1.0-gamma - 2006-09-07
--------------------------

  * Added auto-wrapping of the required html markup around lists (`ul` and `ol`) if `jQuery().jcarousel()` is assigned directly to them.
  * Added support for new callback functions `itemFirstInHandler`, `itemFirstOutHandler`, `itemLastInHandler`, `itemLastOutHandler`, `itemVisibleInHandler`, `itemVisibleOutHandler`.
  * General sourcecode rewriting.
  * Fixed bug not setting `<li>` index attributes correctly.
  * Changed default `itemWidth` and `itemHeight` to 75.

Version 0.1.0-beta - 2006-09-02
--------------------------

  * Initial release.
