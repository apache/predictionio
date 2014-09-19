

curl -i -X POST "http://localhost:7070/events.json" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$delete",
  "entityType" : "pio_user",
  "entityId" : "123",
  "appId" : 2,
}'


curl -i -X POST "http://localhost:7070/events.json" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$delete",
  "entityType" : "pio_item",
  "entityId" : "174",
  "appId" : 2,
}'


curl -i -X POST "http://localhost:7070/events.json" \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "pio_item",
  "entityId" : "174",
  "appId" : 4,
  "properties" : {
    "piox_a" : 1
  }
}'
