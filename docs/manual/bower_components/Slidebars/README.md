Slidebars
=========

Slidebars is a jQuery plugin for quickly and easily implementing app style off-canvas menus and sidebars into your website.

See the [Slidebars website](http://plugins.adchsm.me/slidebars/) for more information, usage documentation and compatibility tables.

Whats New
---------

Version 0.10.2 - Released July 3, 2014

* Fixes an issue with scrolling locking when Slidebars are closed (thanks to [Jay Padua](https://github.com/jpadua)).
 
Version 0.10.1 - Released June 27, 2014

* Re-written close control class.
* Fixes an issue with scrolling Slidebar menu items on touch devices.
* Improved support for scroll locking on iOS.

Version 0.10 - Released June 24, 2014

* Links in Slidebars no longer close the containing Slidebar before following the link. Instead add class `.sb-close` to the `<a>`, or the parent of `<a>` to close first.
* Class `.sb-disable-close` has been depreciated, see above.
* API rework. All API methods are now prefixed with `.slidebars`. For example `yourVariable.slidebars.open('left');`.
* New API method `.destroy(side)`. Removes Slidebar from the DOM.
* New option `scrollLock` set to true or false, default: false. Prevents site scrolling when a Slidebar is open. Alternatively you may also add class `sb-scroll-lock` the `<html>`.
* You may now use class `.sb-site-container` instead of `#sb-site`.
* The script no longer checks positions of site container and Slidebar elements, or moves them if located incorrectly.
* .NET compatible.
* New modifier class `.sb-momentum-scrolling` for Slidebar elements that uses `-webkit-overflow-scrolling: touch;`.
* Fixes an issue with minimum heights for site container.
* Minimum heights are no longer set for screen sizes larger than disableOver (if provided).
* Fixes issues with push and overlay styles on iOS 7.
* Inactive Slidebars now use `display: none;` instead of `visibility: hidden;` to allow for native scrolling as above.
* Inline styling removed after closing animation.
* Fixes a bug with incorrect negative margins when using custom widths and push/overlay styles.
* Fixes a bug where animation snapped open on Android < 4.3.

Plans for Next Release
----------------------

* Callback functions
* Event listeners
* Additional animation styles
* Additional API methods and event handling
* Windows phone bug fixes
* Improved scroll locking
* Web app setting

Version History
---------------

Version 0.9.4 - Released April 1, 2014

* Fixes issue with site locking and scrolling difficulty.

Version 0.9.1, 0.9.2, 0.9.3

* Fixes for jQuery Plugin Registry and Bower support.

Version 0.9 - Released March 10, 2014

* New push animation style, add [modifier class](http://plugins.adchsm.me/slidebars/usage.php#slidebars) `.sb-style-push` to a Slidebar.
* New overlay animation style, add [modifier class](http://plugins.adchsm.me/slidebars/usage.php#slidebars) `.sb-style-overlay` to a Slidebar.
* New thin width, add [modifier class](http://plugins.adchsm.me/slidebars/usage.php#slidebars) `.sb-width-thin` to a Slidebar.
* New wide width, add [modifier class](http://plugins.adchsm.me/slidebars/usage.php#slidebars) `.sb-width-wide` to a Slidebar.
* Static Slidebars support for older versions of iOS which do not support `position: fixed;`
* More API functions, return when Slidebars has been initiated, and if either Slidebar is open or closed.
* Re-written window resize events.
* Improved static Slidebars.
* Improved inline `min-height` styling, to avoid additional spacing under site.
* Fixed a bug where control classes couldn't be hidden using CSS.
* [Helper class](http://plugins.adchsm.me/slidebars/usage.php#helper-classes) `html.sb-android` has been depreciated, see below.
* New [helper class](http://plugins.adchsm.me/slidebars/usage.php#helper-classes) `html.sb-static` has been added, use this to un-fix your fixed elements for use on Android &lt; 3 and iOS &lt; 5.

Version 0.8.2 - Released February 24, 2014

* Event handling has been re-written in this update for better Android support. Also fixed an error where unnecessary space was added below the site content on a window resize. Thanks to francis-ceril, ecclescake and dmmikkel.

Version 0.8.1 - Released February 17, 2014

* Fixed an error which caused Slidebars to fail when only using one Slidebar. Thanks to Damien Jarry (wibimaster) for the fix.

Version 0.8 - Released February 17, 2014

* Added Slidebars Theme! An optional theme which you are free to use and abuse. It features a top navigation bar, list menus in Slidebars with unlimited level of drop downs, widgets and other useful CSS snippets. You can find the [theme usage here] (http://plugins.adchsm.me/slidebars/theme.php).
* Mobile and tablet only support. Pass a width to [option disableOver] (http://plugins.adchsm.me/slidebars/usage.php#options) to disable Slidebars over a certain width screen.
* Show/Hide the Slidebar control classes over the disabled width with [option hideControlClasses] (http://plugins.adchsm.me/slidebars/usage.php#options).
* You can now easily set [custom widths for your Slidebars] (http://plugins.adchsm.me/slidebars/usage.php#custom-widths). Add modifier class .sb-width-custom to your Slidebar, and pass a pixel or percentage width as a data attribute data-sb-width.
* New modifier class [.sb-static] (http://plugins.adchsm.me/slidebars/usage.php#modifier-classes). Add this class to your Slidebar to unfix it and scroll naturally with the site.
* Removed touchend events from links in Slidebars for better scrolling support.
* Reworked Control Class events to prevent tap and click events travelling down to elements in Slidebars.
* Added another Slidebar width for screens between 480px and 768px.
* Better Slidebar closing from clicking on the site.
* Cleaned up code.
* Further code comments.

Version 0.7.1 - Released January 22, 2014

* Added Slidebars options.
* Added helper class html.sb-android for Android Browser version 2.*. Can be used to unfix your elements with position fixed.
* Added helper class .sb-disable-close. Apply to a link in a Slidebar to prevent it from closing on click.
* Smoother jQuery .animate() animations on window resize events.
* Cleaned up code.

Version 0.7 - Released January 13, 2014 - First public release.
