$(document).ready(function() {
  // Carousel Initialization
  $('.jcarousel')
    .jcarousel({
      // Options go here
    });
  
  // Prev Control Initialization
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
  
  // Next Control Initialization
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
  
  // Pagination Initialization
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
    
  $('#tryit-start').on('click', function() {  
    $('.jcarousel').jcarousel('scroll', 1);
  });
});