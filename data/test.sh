#!/usr/bin/env bash

# simple test script for dataapi

function checkGET () {
  resp=$( curl -i -s -X GET "http://localhost:7070$1" )
  status=$( echo "$resp" | grep HTTP/1.1 )
  exp=$2
  if [[ $status =~ (.*HTTP/1.1 $exp [a-zA-Z]+) ]]; then
  echo "[pass] GET $1 $status"
  else
  echo "[fail] GET $1 $resp"
  echo "expect $exp"
  exit -1
  fi
}


function checkPOST () {
  resp=$( curl -i -s -X POST http://localhost:7070$1 \
  -H "Content-Type: application/json" \
  -d "$2" )
  status=$( echo "$resp" | grep HTTP/1.1 )
  exp=$3
  if [[ $status =~ (.*HTTP/1.1 $exp [a-zA-Z]+) ]]; then
  #echo "POST $1 $2 good $status"
  echo "[pass] POST $1 $status"
  else
  echo "[fail] POST $1 $2 $resp"
  echo "expect $exp"
  exit -1
  fi
}

function checkDELETE () {
  resp=$( curl -i -s -X DELETE "http://localhost:7070$1" )
  status=$( echo "$resp" | grep HTTP/1.1 )
  exp=$2
  if [[ $status =~ (.*HTTP/1.1 $exp [a-zA-Z]+) ]]; then
  #echo "POST $1 $2 good $status"
  echo "[pass] DELETE $1 $status"
  else
  echo "[fail] DELETE $1 $2 $resp"
  echo "expect $exp"
  exit -1
  fi
}


# ---------------
# status
# ----------------

checkGET "/" 200

# -------------
# create events
# -------------


# full
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
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

checkPOST "/events.json" "$testdata" 201

# no properties
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events.json" "$testdata" 201

testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "eventTime" : "2004-12-14T21:39:45.618Z",
  "properties": {}
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events.json" "$testdata" 201

# no tags
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-15T21:39:45.618Z",
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events.json" "$testdata" 201

## no eventTIme
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events.json" "$testdata" 201

## no prediction key
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4
}'

checkPOST "/events.json" "$testdata" 201

# minimum
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "appId" : 4
}'

checkPOST "/events.json" "$testdata" 201


# ----------------------------
# create events error cases
# ----------------------------

# missing event
testdata='{
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
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

# missing entityType
testdata='{
  "event" : "my_event",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
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

# missing entityId
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "appId" : 4,
  "predictionKey" : "my_prediction_key"
}'

checkPOST "/events.json" "$testdata" 400

# missing appId
testdata='{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
  "targetEntityId" : "my_target_entity_id",
  "properties" : {
    "prop1" : "value1",
    "prop2" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "predictionKey" : "my_prediction_key"
}'


checkPOST "/events.json" "$testdata" 400

# empty event string
testdata='{
  "event" : "",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : "my_target_entity_type",
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

checkPOST "/events.json" "$testdata" 400

# empty
testdata='{}'
checkPOST "/events.json" "$testdata" 400

# empty
testdata=''
checkPOST "/events.json" "$testdata" 400

# invalid data
testdata='asfd'
checkPOST "/events.json" "$testdata" 400

# -----
# get events
# ----

checkGET "/events.json?appId=4" 200

checkGET "/events.json?appId=999" 404

checkGET "/events.json?appId=4&startTime=abc" 400

checkGET "/events.json?appId=4&untilTime=abc" 400

checkGET "/events.json?appId=4&startTime=2004-12-13T21:39:45.618Z&untilTime=2004-12-15T21:39:45.618Z" 200


# -----
# delete
# -----

checkDELETE "/events.json?appId=4" 200

checkGET "/events.json?appId=4" 404
