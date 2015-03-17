#!/usr/bin/env bash
accessKey=$1

curl -i -X POST http://localhost:7070/events.json?accessKey=$1 \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event1",
  "entityType" : "user"
  "entityId" : "uid",
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}' \
-w %{time_total}
