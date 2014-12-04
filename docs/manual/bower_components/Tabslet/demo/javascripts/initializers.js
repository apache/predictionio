// Run javascript after DOM is initialized
$(document).ready(function() {

	$('#body').waypoint('sticky');

	$('.tabs_default').tabslet();

	$('.tabs_active').tabslet({
		active: 2
	});

	$('.tabs_hover').tabslet({
		mouseevent: 'hover',
		attribute: 'href',
		animation: false
	});

	$('.tabs_animate').tabslet({
		mouseevent: 'click',
		attribute: 'href',
		animation: true
	});

	$('.tabs_rotate').tabslet({
		autorotate: true,
		delay: 3000
	});

	$('.tabs_controls').tabslet();

	$('.before_event').tabslet();
	$('.before_event').on("_before", function() {
		alert('This alert comes before the tab change!')
	});

	$('.after_event').tabslet({
		animation: true
	});
	$('.after_event').on("_after", function() {
		alert('This alert comes after the tab change!')
	});

});