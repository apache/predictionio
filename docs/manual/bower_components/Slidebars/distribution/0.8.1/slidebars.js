// -----------------------------------
// Slidebars
// Version 0.8.1
// http://plugins.adchsm.me/slidebars/
//
// Written by Adam Smith
// http://www.adchsm.me/
//
// Released under MIT License
// http://plugins.adchsm.me/slidebars/license.txt
//
// ---------------------
// Index of Slidebars.js
//
// 001 - Default Settings
// 002 - Feature Detection
// 003 - User Agents
// 004 - Setup
// 005 - Animation
// 006 - Operations
// 007 - API
// 008 - Window Resizes
// 009 - User Input

;(function($) {

	$.slidebars = function(options) {

		// ----------------------
		// 001 - Default Settings

		var settings = $.extend({
			siteClose: true, // true or false - Enable closing of Slidebars by clicking on #sb-site.
			disableOver: false, // integer or false - Hide Slidebars over a specific width.
			hideControlClasses: false // true or false - Hide controls at same width as disableOver.
		}, options);

		// -----------------------
		// 002 - Feature Detection

		var test = document.createElement('div').style, // Create element to test on.
		supportTransition = false, // Variable for testing transitions.
		supportTransform = false; // variable for testing transforms.

		// Test for CSS Transitions
		if (test.MozTransition === '' || test.WebkitTransition === '' || test.OTransition === '' || test.transition === '') supportTransition = true;

		// Test for CSS Transforms
		if (test.MozTransform === '' || test.WebkitTransform === '' || test.OTransform === '' || test.transform === '') supportTransform = true;

		// -----------------
		// 003 - User Agents

		var ua = navigator.userAgent, // Get user agent string.
		android = false; // Variable for storing android version.

		if (ua.match(/Android/)) { // Detect for Android in user agent string.
			android = parseFloat(ua.slice(ua.indexOf('Android')+8)); // Get version of Android.
			if (android < 3) $('html').addClass('sb-android'); // Add 'sb-android' helper class for unfixing elements.
		}

		// -----------
		// 004 - Setup

		// Site Container
		if (!$('#sb-site').length) $('body').children().wrapAll('<div id="sb-site" />'); // Check if content is wrapped with sb-site, wrap if it isn't.
		var $site = $('#sb-site'); // Cache the selector.
		if (!$site.parent().is('body')) $site.appendTo('body'); // Check its location and move if necessary.
		$site.addClass('sb-slide'); // Add animation class.

		// Left Slidebar	
		if ($('.sb-left').length) { // Check if the left Slidebar exists.
			var $left = $('.sb-left'), // Cache the selector.
			leftActive = false; // Used to check whether the left Slidebar is open or closed.
			if (!$left.parent().is('body')) $left.appendTo('body'); // Check its location and move if necessary.
			if (android && android < 3) $left.addClass('sb-static'); // Add static class for older versions of Android.
			if ($left.hasClass('sb-width-custom')) $left.css('width', $left.attr('data-sb-width')); // Set user custom width.
		}

		// Right Slidebar
		if ($('.sb-right').length) { // Check if the right Slidebar exists.
			var $right = $('.sb-right'), // Cache the selector.
			rightActive = false; // Used to check whether the right Slidebar is open or closed.
			if (!$right.parent().is('body')) $right.appendTo('body'); // Check its location and move if necessary.
			if (android && android < 3) $right.addClass('sb-static'); // Add static class for older versions of Android.
			if ($right.hasClass('sb-width-custom')) $right.css('width', $right.attr('data-sb-width')); // Set user custom width.
		}
		
		// Set Minimum Heights
		function setMinHeights() {
			var minHeight = $('html').css('height'); // Get minimum height of the page.
			$site.css('minHeight', minHeight); // Set minimum height to the site.
			if ($left && $left.hasClass('sb-static')) $left.css('minHeight', minHeight);  // Set minimum height to the left Slidebar.
			if ($right && $right.hasClass('sb-static')) $right.css('minHeight', minHeight);  // Set minimum height to the right Slidebar.
		}
		setMinHeights(); // Set them
		
		// Control Classes
		var $controls = $('.sb-toggle-left, .sb-toggle-right, .sb-open-left, .sb-open-right, .sb-close');

		// Initialise
		function initialise() {
			var windowWidth = $(window).width(); // Get the window width.
			if (!settings.disableOver || (typeof settings.disableOver === 'number' && settings.disableOver >= windowWidth)) { // False or larger than window size. 
				this.init = true; // User check, returns true if Slidebars has been initiated.
				$('html').addClass('sb-init'); // Add helper class.
				if (settings.hideControlClasses) $controls.show();
			} else if (typeof settings.disableOver === 'number' && settings.disableOver < windowWidth) { // Less than window size.
				this.init = false; // User check, returns true if Slidebars has been initiated.
				$('html').removeClass('sb-init'); // Remove helper class.
				if (settings.hideControlClasses) $controls.hide(); // Hide controls
				if (leftActive || rightActive) close(); // Close Slidebars if open.
			}
		}
		initialise();

		// ---------------
		// 005 - Animation

		var animation, // Animation type.
		$slide = $('.sb-slide'); // Cache all elements to animate.

		// Set Animation Type
		if (supportTransition && supportTransform) { // Browser supports CSS Transitions
			animation = 'translate'; // Translate for browser that support transform and tranisions.
			if (android && android < 4.4) animation = 'side'; // Android supports both, but can't translate any fixed positions, so use left instead.
		} else {
			animation = 'jQuery'; // Browsers that don't support css transitions and transitions.
		}

		$('html').addClass('sb-anim-type-' + animation); // Add animation type class.

		// Animate Mixin
		function animate (selector, amount, side) {
			if (animation === 'translate') {
				selector.css({
					'transform': 'translate(' + amount + ')'
				});
			} else if (animation === 'side') {
				selector.css(side, amount);
			} else if (animation === 'jQuery') {
				var properties = {};
				properties[side] = amount;
				selector.stop().animate(properties, 400);
			}
		}

		// ----------------
		// 006 - Operations

		// Open a Slidebar
		function open(side) {
			// Check to see if opposite Slidebar is open.
			if (side === 'left' && $left && rightActive || side === 'right' && $right && leftActive) { // It's open, close it, then continue.
				close();
				setTimeout(proceed, 400);
			} else { // Its not open, continue.
				proceed();
			}

			// Open
			function proceed() {
				if (this.init && side === 'left' && $left) { // Slidebars is initiated, left is in use and called to open.
					var leftWidth = $left.css('width'); // Get the width of the left Slidebar.
					$('html').addClass('sb-active sb-active-left'); // Add active classes.
					animate($slide, leftWidth, 'left'); // Animation
					setTimeout(function() { leftActive = true; }, 400); // Set active variables.
				} else if (this.init && side === 'right' && $right) { // Slidebars is initiated, right is in use and called to open.
					var rightWidth = $right.css('width'); // Get the width of the right Slidebar.
					$('html').addClass('sb-active sb-active-right'); // Add active classes.
					animate($slide, '-' + rightWidth, 'left'); // Animation
					setTimeout(function() { rightActive = true; }, 400); // Set active variables.
				}
			}
		}
			
		// Close either Slidebar
		function close(link) {
			if (leftActive || rightActive) { // If a Slidebar is open.
				leftActive = false; // Set active variable.
				rightActive = false; // Set active variable.
				animate($slide, '0px', 'left'); // Animation
				setTimeout(function() { // Wait for closing animation to finish.
					$('html').removeClass('sb-active sb-active-left sb-active-right'); // Remove active classes.
					if (link) window.location = link; // If a link has been passed to the function, go to it.
				}, 400);
			}
		}
		
		// Toggle either Slidebar
		function toggle(side) {
			if (side === 'left' && $left) { // If left Slidebar is called and in use.
				if (leftActive) {
					close(); // Slidebar is open, close it.
				} else if (!leftActive) {
					open('left'); // Slidebar is closed, open it.
				}	
			} else if (side === 'right' && $right) { // If right Slidebar is called and in use.
				if (rightActive) {
					close(); // Slidebar is open, close it.
				} else if (!rightActive) {
					open('right'); // Slidebar is closed, open it.
				}
			}
		}

		// ---------
		// 007 - API

		this.open = open; // Maps user variable name to the open method.
		this.close = close; // Maps user variable name to the close method.
		this.toggle = toggle; // Maps user variable name to the toggle method.

		// --------------------
		// 008 - Window Resizes

		function resize() {
			setMinHeights(); // Reset the minimum heights.
			initialise(); // Check new screen sizes to see if Slidebars should still operate.
			if (leftActive) { // Left Slidebar is open whilst the window is resized.
				open('left'); // Running the open method will ensure the slidebar is the correct width for new screen size.
			} else if (rightActive) { // Right Slidebar is open whilst the window is resized.
				open('right'); // Running the open method will ensure the slidebar is the correct width for new screen size.
			}
		}
		$(window).resize(resize);

		// ----------------
		// 009 - User Input
		
		function input(event) { // Stop default behaviour and event bubbling.
			event.preventDefault();
			event.stopPropagation();
		}
		
		// Slidebar Toggle Left
		$('.sb-toggle-left').on('touchend click', function(event) {			
			input(event);
			toggle('left'); // Toggle left Slidebar.
		});
		
		// Slidebar Toggle Right
		$('.sb-toggle-right').on('touchend click', function(event) {
			input(event);
			toggle('right'); // Toggle right Slidebar.
		});
		
		// Slidebar Left Open
		$('.sb-open-left').on('touchend click', function(event) {
			input(event);
			if (!leftActive) open('left'); // Slidebar is closed, open it.
		});
		
		// Slidebar Right Open
		$('.sb-open-right').on('touchend click', function(event) {
			input(event);
			if (!rightActive) open('right'); // Slidebar is closed, open it.
		});
		
		// Slidebar Close
		$('.sb-close').on('touchend click', function(event) {
			input(event);
			if (leftActive || rightActive) close(); // A Slidebar is open, close it.
		});
		
		// Slidebar Close via Link
		$('.sb-slidebar a').not('.sb-disable-close').on('click', function(event) {
			if (leftActive || rightActive) { // Only proceed is a Slidebar is active.
				input(event);
				close( $(this).attr('href') ); // Call closing method and pass link.
			}
		});
		
		// Slidebar Close via Site
		$site.on('touchend click', function(event) {
			if (leftActive || rightActive) { // Only proceed if the left or the right Slidebar is active.
				input(event); // If active, stop the click bubbling.
				close(); // Close the Slidebar.
			}
		});

	}; // End slidebars function.

}) (jQuery);