<?php

namespace predictionio;
use GuzzleHttp\Client;

class EventClient extends BaseClient {
  private $appId;

  public function __construct($appId, $baseUrl='http://localhost:7070',
                              $timeout=0, $connectTimeout=5 ) {
    parent::__construct($baseUrl, $timeout, $connectTimeout);
    $this->appId = $appId;
  }

  public function setUser($uid, array $properties=array()) {
    // casting to object so that an empty array would be represented as {}
    if (empty($properties)) $properties = (object)$properties;
    $json = json_encode([
        'event' => '$set',
        'entityType' => 'pio_user',
        'entityId' => $uid,
        'appId' => $this->appId,
        'properties' => $properties
    ]);
   
    return $this->sendRequest('POST', '/events.json', $json);
  }

  public function unsetUser($uid, array $properties=array()) {
    if (empty($properties)) $properties = (object)$properties;
    $json = json_encode([
        'event' => '$unset',
        'entityType' => 'pio_user',
        'entityId' => $uid,
        'appId' => $this->appId,
        'properties' => $properties
    ]);

    return $this->sendRequest('POST', '/events.json', $json);
   }

  public function setItem($iid, array $properties=array()) {
    if (empty($properties)) $properties = (object)$properties;
    $json = json_encode([
        'event' => '$set',
        'entityType' => 'pio_item',
        'entityId' => $iid,
        'appId' => $this->appId,
        'properties' => $properties
    ]);

    return $this->sendRequest('POST', '/events.json', $json);
  }

  public function unsetItem($iid, array $properties=array()) {
    if (empty($properties)) $properties = (object)$properties;
    $json = json_encode([
        'event' => '$unset',
        'entityType' => 'pio_item',
        'entityId' => $iid,
        'appId' => $this->appId,
        'properties' => $properties
    ]);

    return $this->sendRequest('POST', '/events.json', $json);
  }

  public function recordUserActionOnItem($event, $uid, $iid, 
                                         array $properties=array()) {
    if (empty($properties)) $properties = (object)$properties;
    $json = json_encode([
        'event' => $event,
        'entityType' => 'pio_user',
        'entityId' => $uid,
        'targetEntityType' => 'pio_item',
        'targetEntityId' => $iid,
        'appId' => $this->appId,
        'properties' => $properties
    ]);

    return $this->sendRequest('POST', '/events.json', $json);
  }

  public function createEvent(array $data) {
    $json = json_encode($data);

    return $this->sendRequest('POST', '/events.json', $json);
  }

  public function getEvent($eventId) {
    return $this->sendRequest('GET', "/events/$eventId.json", '');
  }
}

?>
