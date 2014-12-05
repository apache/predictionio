/* -----------------------------------
 * Slidebars
 * Version 0.9.4
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
 * 005 - Helper Classes
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
	height: 100%;
}

body {
	min-height: 100%;
	position: relative;
}

/* ----------
 * 002 - Site
 */

#sb-site {
	width: 100%;
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

.sb-left {
	left: 0; /* Set Slidebar to the left. */
}

.sb-right {
	right: 0; /* Set Slidebar to the right. */
}

html.sb-static .sb-slidebar,
.sb-slidebar.sb-static {
	position: absolute; /* Makes Slidebars scroll naturally with the site, and unfixes them for Android Browser < 3 and iOS < 5. */
}

.sb-slidebar.sb-active {
	visibility: visible; /* Makes Slidebars visibile when open. */
}

.sb-slidebar.sb-style-overlay {
	z-index: 9999; /* Set z-index high to ensure it overlays any other site elements. */
}

/* Slidebar widths for devices that don't support media queries. */
	.sb-slidebar {
		width: 30%;
	}
	
	.sb-width-thin {
		width: 15%;
	}
	
	.sb-width-wide {
		width: 45%;
	}

@media (max-width: 480px) { /* Slidebar width on extra small screens. */
	.sb-slidebar {
		width: 70%;
	}
	
	.sb-width-thin {
		width: 55%;
	}
	
	.sb-width-wide {
		width: 85%;
	}
}

@media (min-width: 481px) { /* Slidebar width on small screens. */
	.sb-slidebar {
		width: 55%;
	}
	
	.sb-width-thin {
		width: 40%;
	}
	
	.sb-width-wide {
		width: 70%;
	}
}

@media (min-width: 768px) { /* Slidebar width on small screens. */
	.sb-slidebar {
		width: 40%;
	}
	
	.sb-width-thin {
		width: 25%;
	}
	
	.sb-width-wide {
		width: 55%;
	}
}

@media (min-width: 992px) { /* Slidebar width on medium screens. */
	.sb-slidebar {
		width: 30%;
	}
	
	.sb-width-thin {
		width: 15%;
	}
	
	.sb-width-wide {
		width: 45%;
	}
}

@media (min-width: 1200px) { /* Slidebar width on large screens. */
	.sb-slidebar {
		width: 20%;
	}
	
	.sb-width-thin {
		width: 5%;
	}
	
	.sb-width-wide {
		width: 35%;
	}
}

/* ---------------
 * 004 - Animation
 */

.sb-slide, #sb-site, .sb-slidebar {
	-webkit-transition: -webkit-transform 400ms ease;
	   -moz-transition: -moz-transform 400ms ease;
	     -o-transition: -o-transform 400ms ease;
	        transition: transform 400ms ease;
	-webkit-transition-property: -webkit-transform, left, right; /* Add left/right for Android < 4.4. */
	-webkit-backface-visibility: hidden; /* Prevents flickering. This is non essential, and you may remove it if your having problems with fixed background images in Chrome. */
}

/* --------------------
 * 005 - Helper Classes
 */
 
.sb-hide { 
	display: none; /* May be applied to control classes when Slidebars is disabled over a certain width. */
}