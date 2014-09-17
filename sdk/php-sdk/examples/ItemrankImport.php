<?php
require_once("vendor/autoload.php");

use predictionio\EventClient;

// check Event Server status
$client = new EventClient(88);
$response=$client->getStatus();
echo($response);

// set user - generate 10 users
for ($u=1; $u<=10; $u++) {
  $response=$client->setUser($u, array('age'=>20+$u, 'gender'=>'M'));
  print_r($response);
}


// unset user
/*
$response=$client->unsetUser(1, array('age'=>20));
print_r($response);
*/

 
// set item - generate 50 items
for ($i=1; $i<=50; $i++) {
  $response=$client->setItem($i, array('pio_itypes'=>array('1')));
  print_r($response);
}

// record event - each user randomly views 10 items
for ($u=1; $u<=10; $u++) {
  for ($count=0; $count<10; $count++) {
    $i = rand(1,50);
    $response=$client->recordUserActionOnItem('view', $u, $i);
    print_r($response);
  }
} 


?>
