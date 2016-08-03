#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# simple test script for dataapi
accessKey=$1

curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$delete",
  "entityType" : "pio_user",
  "entityId" : "123"
}'


curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$delete",
  "entityType" : "pio_item",
  "entityId" : "174"
}'


curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "pio_item",
  "entityId" : "174",
  "properties" : {
    "piox_a" : 1
  }
}'


curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : null,
  "targetEntityId" : null,
  "properties" : {
    "prop1" : 1,
    "prop2" : null,
  }
  "eventTime" : "2004-12-13T21:39:45.618Z"
}'


curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityType" : "my_entity_type",
  "entityId" : "my_entity_id",
  "targetEntityType" : null,
  "targetEntityId" : null,
  "properties" : null,
  "eventTime" : null
}'


## prId
curl -i -X POST "http://localhost:7070/events.json?accessKey=$accessKey" \
-H "Content-Type: application/json" \
-d '{
  "event" : "some_event",
  "entityType" : "pio_user",
  "entityId" : "123",
  "prId" : "AbcdefXXFFdsf1"
}'
