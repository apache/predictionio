#!/usr/bin/env bash

set -e

## valid scenario:

curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : 1,
    "prop2" : "value2",
    "prop3" : [1, 2, 3],
    "prop4" : true,
    "prop5" : ["a", "b", "c"],
    "prop6" : 4.56
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

# no properties
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

# no tags
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

## no eventTIme
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

## no prediction key
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4
}'

# minimum
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "appId" : 4
}'

# error
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'


# missing appId
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "predictionKey" : "my_prediction_key"
}'


## empty event string
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'
