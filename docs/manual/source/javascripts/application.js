//= require 'jquery'
//= require 'Tabslet'
//= require 'jcarousel'

$(document).ready(function() {
  function navExpand(link) {
    link.removeClass('expandible').addClass('collapsible');
    link.children('i').removeClass('fa-caret-right').addClass('fa-caret-down');
    link.next('ul').show();
  }
  
  function navCollapse(link) {
    link.removeClass('collapsible').addClass('expandible');
    link.children('i').removeClass('fa-caret-down').addClass('fa-caret-right');
    link.next('ul').hide();
  }
  
  // Main Navigation
  $('#nav-main a').on('click', function(event) {
    var $this = $(this);
    
    if ($this.hasClass('expandible')) {
      navExpand($this);
      event.preventDefault();
    } else if ($this.hasClass('collapsible')) {
      navCollapse($this);
      event.preventDefault();
    }
  });
  
  $('#nav-main .active').parentsUntil('#nav-main').each(function() {
    $(this).children('.expandible').each(function() {
      var $this = $(this);
      if (!$this.hasClass('active')) {
        navExpand($this);
      }
    });
  });

  $('#content').on('click', function(event) {
    $('body').removeClass('active-navigation')
  });

  $('#active-navigation').on('click', function(event) {
    event.preventDefault();
    $('body').toggleClass('active-navigation')
  });

  if ($('#table-of-contents').is(':empty')) {
    $('#table-of-contents').addClass('empty')
  }

  // Tabslet
  $('.tabs').tabslet();

  // Tab Syncing
  $('.control li').on('mousedown', function(event) {
    lang = $(this).data('lang')
    $('.control li[data-lang="' + lang + '"]').each(function() {
      $(this).children('a:first').trigger('click')
    });
  });

  // External Links
  $("a[href^='http']").each(function() {
    $(this).click(function(event) {
      event.preventDefault();
      window.open(this.href);
    }).addClass('external');
  });
  
  

});