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

curl -i -X POST http://localhost:7070/events.json?accessKey=testingkeyasdfasdf \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event1",
  "entityType" : "user"
  "entityId" : "uid",
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}'

curl -i -X POST http://localhost:7070/events.json?accessKey=yT8WHQMkQLBPxGdcGWstu6Z12XaNjANu7py98Ysve2NHwGNp825bkCt2G3LPU6aK \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event1",
  "entityType" : "user"
  "entityId" : "uid",
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}'


curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event2",
  "entityType" : "user"
  "entityId" : "uid",
  "eventTime" : "2004-12-14T21:39:45.618-07:00"
}'


curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event3",
  "entityType" : "user"
  "entityId" : "uid",
  "eventTime" : "2004-12-11T21:39:45.618-07:00"
}'

curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event4",
  "entityType" : "user"
  "entityId" : "uid2",
  "eventTime" : "2004-12-11T22:39:45.618-07:00"
}'

curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event5",
  "entityType" : "user"
  "entityId" : "uid2",
  "eventTime" : "2004-12-14T21:39:45.618-07:00"
}'

curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event1",
  "entityType" : "item"
  "entityId" : "uid",
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}'
