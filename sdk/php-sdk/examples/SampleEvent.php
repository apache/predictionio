<?php
require_once("vendor/autoload.php");

use predictionio\EventClient;
use predictionio\PredictionIOAPIError;

try {
  // check Event Server status
  $client = new EventClient(25);
  $response=$client->getStatus();
  echo($response);

  // set user 
  $response=$client->setUser(8, array('age'=>20, 'gender'=>'M'));
  print_r($response);

  // unset user
  $response=$client->unsetUser(8, array('age'=>20));
  print_r($response);
 
  // set item 
  $response=$client->setItem(2, array('pio_itypes'=>array('1')));
  print_r($response);

  // unset item 
  $response=$client->unsetItem(4, array('pio_itypes'=>array('1')));
  print_r($response);

  // record user action on item
  $response=$client->recordUserActionOnItem('view', 8, 2);
  print_r($response);

  // create event
  $response=$client->createEvent(array(
                        'appId' => 25,
                        'event' => 'my_event',
                        'entityType' => 'pio_user',
                        'entityId' => '8',
                        'properties' => array('prop1'=>1, 'prop2'=>2),
                        'tags' => array('tag1', 'tag2'),
                   ));
  print_r($response); 

  $response=$client->getEvent('event_id_goes_here');
  print_r($response);                       

} catch (PredictionIOAPIError $e) {
  echo $e->getMessage();
}
?>
