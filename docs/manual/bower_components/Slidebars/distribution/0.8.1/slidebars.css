/* -----------------------------------
 * Slidebars
 * Version 0.8.1
 * http://plugins.adchsm.me/slidebars/
 *
 * Written by Adam Smith
 * http://www.adchsm.me/
 *
 * Released under MIT License
 * http://plugins.adchsm.me/slidebars/license.txt
 *
 * -------------------
 * Slidebars CSS Index
 *
 * 001 - Box Model, Html & Body
 * 002 - Site
 * 003 - Slidebars
 * 004 - Animation
 *
 * ----------------------------
 * 001 - Box Model, Html & Body
 */

html, body, #sb-site, .sb-slidebar {
	margin: 0;
	padding: 0;
	-webkit-box-sizing: border-box;
	   -moz-box-sizing: border-box;
	        box-sizing: border-box;
}

html, body {
	width: 100%;
	overflow-x: hidden; /* Stops horizontal scrolling. */
}

html {
	min-height: 100%;
}

body {
	height: 100%;
}

/* ----------
 * 002 - Site
 */

#sb-site {
	width: 100%;
	min-height: 100%; /* Initially set here but accurate height is set by slidebars.js */
	position: relative;
	z-index: 1; /* Site sits above Slidebars */
	background-color: #ffffff; /* Default background colour, overwrite this with your own css. */
}

/* ---------------
 * 003 - Slidebars
 */

.sb-slidebar {
	height: 100%;
	overflow-y: auto; /* Enable vertical scrolling on Slidebars when needed. */
	position: fixed;
	top: 0;
	z-index: 0; /* Slidebars sit behind sb-site. */
	visibility: hidden; /* Initially hide the Slidebars. */
	background-color: #222222; /* Default Slidebars background colour, overwrite this with your own css. */
}

.sb-static { /* Makes Slidebars scroll naturally with the site, and unfixes them for Android Browser 2.X. */
	position: absolute;
}

.sb-left {
	left: 0; /* Sets Slidebar to the left. */
}

.sb-right {
	right: 0; /* Sets Slidebar to the right. */
}

html.sb-active-left .sb-left,
html.sb-active-right .sb-right {
	visibility: visible; /* Makes Slidebars visibile when open. */
}

/* Slidebar Widths */

.sb-slidebar {
	width: 30%; /* Browsers that don't support media queries. */
}

.sb-width-custom {
	/* To set a custom width, add this class to your Slidebar and pass a px or % value as a data attribute 'data-sb-width'. */
}

@media (max-width: 480px) {
	.sb-slidebar {
		width: 70%; /* Slidebar width on extra small screens. */
	}
}

@media (min-width: 481px) {
	.sb-slidebar {
		width: 55%; /* Slidebar width on small screens. */
	}
}

@media (min-width: 768px) {
	.sb-slidebar {
		width: 40%; /* Slidebar width on small screens. */
	}
}

@media (min-width: 992px) {
	.sb-slidebar {
		width: 30%; /* Slidebar width on medium screens. */
	}
}

@media (min-width: 1200px) {
	.sb-slidebar {
		width: 20%; /* Slidebar width on large screens. */
	}
}

/* ---------------
 * 004 - Animation
 */

html.sb-anim-type-translate .sb-slide, html.sb-anim-type-side .sb-slide {
	-webkit-transition: -webkit-transform 400ms ease;
	   -moz-transition: -moz-transform 400ms ease;
	     -o-transition: -o-transform 400ms ease;
	        transition: transform 400ms ease;
	-webkit-transition-property: -webkit-transform, left; /* Add 'left' for Android < 4.4 */
	-webkit-backface-visibility: hidden; /* Prevents flickering. */
}