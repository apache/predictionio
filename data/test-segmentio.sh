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

accessKey=$1

# normal case
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/identify | \
curl -X POST \
    -H "Content-Type: application/json" \
    -d @- \
    http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey
echo ''

# normal case api key in header for identify event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/identify | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# normal case api key in header for track event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/track | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# normal case api key in header for page event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/page | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# normal case api key in header for screen event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/screen | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# normal case api key in header for group event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/group | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# normal case api key in header for alias event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/alias | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json
echo ''

# invalid type
curl -i -X POST http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "type"      : "invalid_type",
  "userId"    : "019mr8mf4r",
  "sent_at":"2015-08-21T15:25:32.799Z",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}
echo ''

# invalid data format
curl -i -X POST http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "userId"    : "019mr8mf4r",
  "sent_at":"2015-08-21T15:25:32.799Z",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}
echo ''

# invalid webhooks path
curl -i -X POST http://localhost:7070/webhooks/invalidpath.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "type"      : "identify",
  "userId"    : "019mr8mf4r",
  "sent_at":"2015-08-21T15:25:32.799Z",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}
echo ''

# get request
curl -i -X GET http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-w %{time_total}
echo ''

# get invalid
curl -i -X GET http://localhost:7070/webhooks/invalidpath.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-w %{time_total}
echo ''
