<?php

namespace predictionio;

use GuzzleHttp\Client;
use GuzzleHttp\Exception\ClientException;

abstract class BaseClient {
  private $baseUrl;
  private $client;

  public function __construct($baseUrl, $timeout, $connectTimeout) {
    $this->baseUrl = $baseUrl;
    $this->client = new Client([
           'base_url' => $this->baseUrl,
           'defaults' => ['timeout' => $timeout, 
                          'connect_timeout' => $connectTimeout]
    ]);

  }

  public function getStatus() {
    return $this->client->get('/')->getBody();
  }
 
  protected function sendRequest($method, $url, $body) {
    $options = ['headers' => ['Content-Type' => 'application/json'],
                'body' => $body]; 
    $request = $this->client->createRequest($method, $url, $options);

    try {
      $response = $this->client->send($request);
      return $response->json();
    } catch (ClientException $e) {
      throw new PredictionIOAPIError($e->getMessage()); 
    }
  }
}
?>
