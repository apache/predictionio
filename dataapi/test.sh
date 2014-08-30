#!/usr/bin/env bash

function checkGET () {
  status=$( curl -i -s -X GET http://localhost:8081$1 | grep HTTP/1.1)
  exp=$2
  if [[ $status =~ (.*HTTP/1.1 $exp [a-zA-Z]+) ]]; then
  echo "[pass] GET $1 $status"
  else
  echo "[fail] GET $1 $status"
  echo "expect $exp"
  exit -1
  fi
}


function checkPOST () {
  status=$( curl -i -s -X POST http://localhost:8081$1 \
  -H "Content-Type: application/json" \
  -d "$2" | grep HTTP/1.1 )
  exp=$3
  if [[ $status =~ (.*HTTP/1.1 $exp [a-zA-Z]+) ]]; then
  #echo "POST $1 $2 good $status"
  echo "[pass] POST $1 $status"
  else
  echo "[fail] POST $1 $2 $status"
  echo "expect $exp"
  exit -1
  fi
}

checkGET "/" 200

# ---------------
# valid scenario
# ----------------

# full
testdata='{
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

checkPOST "/events" "$testdata" 201

# no properties
testdata='{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 6,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events" "$testdata" 201

testdata='{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityId" : "my_target_entity_id",
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "properties": {}
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events" "$testdata" 201

# no tags
testdata='{
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

checkPOST "/events" "$testdata" 201

## no eventTIme
testdata='{
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

checkPOST "/events" "$testdata" 201

## no prediction key
testdata='{
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

checkPOST "/events" "$testdata" 201

# minimum
testdata='{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "appId" : 4
}'

checkPOST "/events" "$testdata" 201

# -------
# error
# -------

# missing entityId
testdata='{
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

checkPOST "/events" "$testdata" 400

# missing appId
testdata='{
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


checkPOST "/events" "$testdata" 400

# empty event string
testdata='{
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

checkPOST "/events" "$testdata" 400

# empty
testdata='{}'
checkPOST "/events" "$testdata" 400

# empty
testdata=''
checkPOST "/events" "$testdata" 400

# invalid data
testdata='asfd'
checkPOST "/events" "$testdata" 400
