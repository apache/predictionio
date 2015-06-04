#!/usr/bin/env bash
accessKey=$1

# normal case
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/identify | \
curl -X POST \
    -H "Content-Type: application/json" \
    -d @- \
    http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey


# normal case api key in header for identify event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/identify | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json

# normal case api key in header for track event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/track | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json


# normal case api key in header for page event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/page | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json

# normal case api key in header for screen event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/screen | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json

# normal case api key in header for group event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/group | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json

# normal case api key in header for alias event
curl -H "Accept: application/json; version=2.0" \
     http://spec.segment.com/generate/alias | \
curl -X POST \
     --user "$accessKey:" \
     -H "Content-Type: application/json" \
     -d @- \
     http://localhost:7070/webhooks/segmentio.json


# invalid type
curl -i -X POST http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "type"      : "invalid_type",
  "userId"    : "019mr8mf4r",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}

# invalid data format
curl -i -X POST http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "userId"    : "019mr8mf4r",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}


# invalid webhooks path
curl -i -X POST http://localhost:7070/webhooks/invalidpath.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-d '{
  "version"   : 1,
  "type"      : "identify",
  "userId"    : "019mr8mf4r",
  "traits"    : {
      "email"            : "achilles@segment.com",
      "name"             : "Achilles",
      "subscriptionPlan" : "Premium",
      "friendCount"      : 29
  },
  "timestamp" : "2012-12-02T00:30:08.276Z"
}' \
-w %{time_total}


# get request
curl -i -X GET http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-w %{time_total}

# get invalid
curl -i -X GET http://localhost:7070/webhooks/invalidpath.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-w %{time_total}
