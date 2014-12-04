//= require 'jquery'
//= require 'Tabslet'

$(document).ready(function() {

  // Main Navigation
  $('#nav-main > ul > li > a').on('click', function(event) {
    event.preventDefault();
    $(this).next().toggle();
  });

  $('#nav-main .active').parent().parent().show();

  $('#content').on('click', function(event) {
    $('body').removeClass('active-navigation active-complementary')
  });

  $('#active-navigation').on('click', function(event) {
    event.preventDefault();
    $('body').toggleClass('active-navigation').removeClass('active-complementary')
  });

  $('#active-complementary').on('click', function(event) {
    event.preventDefault();
    $('body').toggleClass('active-complementary').removeClass('active-navigation')
  });

  if ($('#table-of-contents').is(':empty')) {
    $('#table-of-contents').addClass('empty')
  }

  // Tabslet
  $('.tabs').tabslet();

  // External Links
  $("a[href^='http']").each(function() {
    $(this).click(function(event) {
      event.preventDefault();
      window.open(this.href);
    }).addClass('external');
  });

});