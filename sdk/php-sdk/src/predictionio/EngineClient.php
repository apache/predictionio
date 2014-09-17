<?php

namespace predictionio;

class EngineClient extends BaseClient {

  public function __construct($appId, $baseUrl="http://localhost:8000",
                              $timeout=0, $connectTimeout=5 ) {
    parent::__construct($baseUrl, $timeout, $connectTimeout);
    $this->appId = $appId;
  }

  public function sendQuery(array $query) {
    return $this->sendRequest("POST", "/", json_encode($query));
  }
}

?>
