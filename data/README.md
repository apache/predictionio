
## Requirement

Install and run HBase

## Data Collection API

Start server:

```
data $ sbt "run-main io.prediction.data.api.Run"
```

Stop server:

    You may hit any key to stop the server.

### Check server status

```
$ curl -i -X GET http://localhost:8081
```

Sample response:
```
HTTP/1.1 200 OK
{"status":"alive"}
```

### Create event

Event of one entity:

```
$ curl -i -X POST http://localhost:8081/events \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityId" : "my_entity_id",
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
```

Event between two entities (with **targetEntityId**):

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

Sample response:

```
HTTP/1.1 201 Created
{"eventId":"pBXkP-GkRfShh-xIRTIG2A"}
```

The following fields are optional:
* **eventTime** : current time will be used if it's not specified
* **tags**: empty list of tag will be used if it's not specified
* **predictionKey**
* **properties**

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

### Get all events of appId
(*use cautiously*)

```
curl -i -X GET http://localhost:8081/events?appId=<your_appId> \
-H "Content-Type: application/json"
```

### Delete all events of appId
```
curl -i -X DELETE http://localhost:8081/events?appId=<your_appId>
```

### Get all events of appId within time range
* eventTime >= startTime:

```
http://localhost:8081/events?appId=<your_appId>&startTime=<time in ISO8601 format>
```
* eventTime < untilTime:

```
http://localhost:8081/events?appId=<your_appId>&untilTime=<time in ISO8601 format>
```

* eventTime >= startTime && eventTime < untilTime:

```
http://localhost:8081/events?appId=2&startTime=<time in ISO8601 format>&untilTime=<time in ISO8601 format>
```

Example,
```
http://localhost:8081/events?appId=2&startTime=2014-08-30T08:45:51.566Z&untilTime=2014-08-30T08:45:51.591Z
```
