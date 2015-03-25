#!/usr/bin/env bash
accessKey=$1

# normal case
curl -i -X POST http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
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
