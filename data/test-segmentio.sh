#!/usr/bin/env bash
accessKey=$1
accessKey=Zzgwx1GnM7yBPCl7XKuITfKmR5pRYRvhxgPu9IszRAcBpi6BMI87QEsEhvAcsDHg

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


curl -i -X GET http://localhost:7070/webhooks/segmentio.json?accessKey=$accessKey \
-H "Content-Type: application/json" \
-w %{time_total}
