## Data Collection API

### Check server status

```
curl -i -X GET http://localhost:8081
```

### Create event

```
curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
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

```

## For Debug Purpose

### Get event

replace <your_eventId> by the returned eventId:

```
curl -i -X GET http://localhost:8081/events/<your_eventId> \
-H "Content-Type: application/json"
```

### Delete event

```
curl -i -X DELETE http://localhost:8081/events/<your_eventId>
```
