/**
 * Draft Tests using QUnit
 *
 */

$(".tabs").tabslet();
$(".tabs_2").tabslet();

test( "Initialization", function() {
  equal($('.tabs').find('DIV').is(':hidden'), true, "Tabs are hidden");
  equal($('.tabs').find('DIV:first').is(':visible'), true, "First tab is visible");
});

test( "Default action", function() {
  $('.tabs_2').find('UL LI:first').next().find('A').click();

  equal($('.tabs_2').find('DIV:first').is(':hidden'), true, "Tabs are hidden");
  equal($('.tabs_2').find('DIV:first').next().is(':visible'), true, "Second tab is visible");
});