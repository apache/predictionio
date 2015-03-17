$(document).ready(function() {
  /*
  Carousel initialization
  */
  $('.jcarousel')
    .jcarousel({
      // Options go here
    });
  
  /*
  Prev control initialization
  */
  $('.jcarousel-control-prev')
    .on('jcarouselcontrol:active', function() {
      $(this).removeClass('inactive');
    })
    .on('jcarouselcontrol:inactive', function() {
      $(this).addClass('inactive');
    })
    .jcarouselControl({
      // Options go here
      target: '-=1'
    });
  
  /*
  Next control initialization
  */
  $('.jcarousel-control-next')
    .on('jcarouselcontrol:active', function() {
      $(this).removeClass('inactive');
    })
    .on('jcarouselcontrol:inactive', function() {
      $(this).addClass('inactive');
    })
    .jcarouselControl({
      // Options go here
      target: '+=1'
    });
  
  /*
  Pagination initialization
  */
  $('.jcarousel-pagination')
    .on('jcarouselpagination:active', 'li', function() {
      $(this).addClass('active');
    })
    .on('jcarouselpagination:inactive', 'li', function() {
      $(this).removeClass('active');
    })
    .jcarouselPagination({
      // Options go here
  
      'item': function(page, carouselItems) {
        return '<li><a href="#' + page + '">' + page + '</a></li>';
      }
    });
});