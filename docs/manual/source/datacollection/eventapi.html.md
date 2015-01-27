---
title: Collecting Data through Event API
---

**Event Server** is designed to collect data into PredictionIO in an event-based
style. Once the Event Server is launched, your application can send data to it
through its **Event API** with HTTP requests or with `EventClient`s of
PredictionIO's SDKs.

INFO: All PredictionIO-compliant engines support accessing the Event Store (i.e. the
data store of Event Server) through [PredictionIO's Storage
API](http://docs.prediction.io/api/current/index.html#io.prediction.data.storage.package).

## Launching the Event Server

INFO: Before launching the Event Server, make sure that your event data store
backend is properly configured and is running. By default, PredictionIO uses
Apache HBase, and a quick configuration can be found
[here](/install/install-linux.html#hbase). Please allow a minute
(usually less than 30 seconds) after HBase is started for its initialization to
complete before starting the Event Server.


Everything about PredictionIO can be done through the `pio` command. Please add
PIO binary command path to to your `PATH` first. Assuming PredictionIO is
installed at `/home/yourname/predictionio/`, you can run

```
$ PATH=$PATH:/home/yourname/predictionio/bin; export PATH
```

To start the event server, run

```
$ pio eventserver
```

By default, the Event Server is bound to localhost, which serves only local
traffic. To serve global traffic, you can use 0.0.0.0, i.e.

```
$ pio eventserver --ip 0.0.0.0
```

### Check Server Status

```
$ curl -i -X GET http://localhost:7070
```

Sample response:

```
HTTP/1.1 200 OK
Server: spray-can/1.2.1
Date: Wed, 10 Sep 2014 22:37:30 GMT
Content-Type: application/json; charset=UTF-8
Content-Length: 18

{"status":"alive"}
```


### Create App

First, you need to create a new App before importing data to the Event Server:

```
$ pio app new MyTestApp
```

> You can replace `MyTestApp` with name of your App.

Take note of the *Access Key* and *App ID* generated. You need the *Access Key*
to use the Event API. You should see something like the following output:

```
2014-11-12 08:56:02,519 INFO  tools.Console$ - Created new app:
2014-11-12 08:56:02,519 INFO  tools.Console$ -         Name: MyTestApp
2014-11-12 08:56:02,520 INFO  tools.Console$ -           ID: 6
2014-11-12 08:56:02,520 INFO  tools.Console$ -   Access Key: WPgcXKd42FPQpZHVbVeMyqF4CQJUnXQmIMTHhX3ZUrSzvy1KXJjdFUrslifa9rnB
```

### Creating Your First Event

You may connect to the Event Server with HTTP request or by using one of many
**PredictionIO SDKs**.

The following shows how one can create an event involving a single entity.
Replace the value of `accessKey` by the *Access Key* generated for your App.

<div class="tabs">
  <div data-tab="Raw HTTP" data-lang="bash">
```bash
$ curl -i -X POST http://localhost:7070/events.json?accessKey=WPgcXKd42FPQpZHVbVeMyqF4CQJUnXQmIMTHhX3ZUrSzvy1KXJjdFUrslifa9rnB \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityType" : "user"
  "entityId" : "uid",
  "properties" : {
    "prop1" : 1,
    "prop2" : "value2",
    "prop3" : [1, 2, 3],
    "prop4" : true,
    "prop5" : ["a", "b", "c"],
    "prop6" : 4.56
  }
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}'
```
  </div>
  <div data-tab="PHP SDK" data-lang="php">
```php
<?php
  require_once("vendor/autoload.php");

  use predictionio\EventClient;

  $accessKey = 'YOUR_ACCESS_KEY';
  $client = new EventClient($accessKey);
  $response = $client->createEvent(array(
                        'event' => 'my_event',
                        'entityType' => 'user',
                        'entityId' => 'uid',
                        'properties' => array('prop1' => 1,
                                              'prop2' => 'value2',
                                              'prop3' => array(1,2,3),
                                              'prop4' => true,
                                              'prop5' => array('a','b','c'),
                                              'prop6' => 4.56
                                        ),
                        'eventTime' => '2004-12-13T21:39:45.618-07:00'
                       ));
?>
```
  </div>
  <div data-tab="Python SDK" data-lang="python">
```python
from predictionio import EventClient
from datetime import datetime
import pytz
client = EventClient('YOUR_ACCESS_KEY', "http://localhost:7070")

first_event_properties = {
    "prop1" : 1,
    "prop2" : "value2",
    "prop3" : [1, 2, 3],
    "prop4" : True,
    "prop5" : ["a", "b", "c"],
    "prop6" : 4.56 ,
    }
first_event_time = datetime(
  2004, 12, 13, 21, 39, 45, 618000, pytz.timezone('US/Mountain'))
first_event_response = client.create_event(
    event="my_event",
    entity_type="user",
    entity_id="uid",
    properties=first_event_properties,
    event_time=first_event_time,
)
```
  </div>
  <div data-tab="Ruby SDK" data-lang="ruby">
```ruby
require 'predictionio'

event_client = PredictionIO::EventClient.new('YOUR_ACCESS_KEY')
event_client.create_event('my_event', 'user', 'uid',
                          'eventTime' => '2004-12-13T21:39:45.618-07:00',
                          'properties' => { 'prop1' => 1,
                                            'prop2' => 'value2',
                                            'prop3' => [1, 2, 3],
                                            'prop4' => true,
                                            'prop5' => %w(a b c),
                                            'prop6' => 4.56 })
```
  </div>
  <div data-tab="Java SDK" data-lang="java">
```java
(coming soon)
```
  </div>
</div>

The following shows how one can create an event involving two entities (with
`targetEntity`).

<div class="tabs">
  <div data-tab="Raw HTTP" data-lang="bash">
```bash
$ curl -i -X POST http://localhost:7070/events.json?accessKey=WPgcXKd42FPQpZHVbVeMyqF4CQJUnXQmIMTHhX3ZUrSzvy1KXJjdFUrslifa9rnB \
-H "Content-Type: application/json" \
-d '{
  "event" : "my_event",
  "entityType" : "user",
  "entityId" : "uid",
  "targetEntityType" : "item",
  "targetEntityId" : "iid",
  "properties" : {
    "someProperty" : "value1",
    "anotherProperty" : "value2"
  },
  "eventTime" : "2004-12-13T21:39:45.618Z"
}'
```
  </div>
  <div data-tab="PHP SDK" data-lang="php">
```php
<?php
  require_once("vendor/autoload.php");

  use predictionio\EventClient;

  $accessKey = 'YOUR_ACCESS_KEY';
  $client = new EventClient($accessKey);
  $response = $client->createEvent(array(
                        'event' => 'my_event',
                        'entityType' => 'user',
                        'entityId' => 'uid',
                        'targetEntityType' => 'item',
                        'targetEntityId' => 'iid',
                        'properties' => array('someProperty'=>'value1',
                                              'anotherProperty'=>'value2'),
                        'eventTime' => '2004-12-13T21:39:45.618Z'
                       ));
?>
```
  </div>
  <div data-tab="Python SDK" data-lang="python">
```python
# Second Event
second_event_properties = {
    "someProperty" : "value1",
    "anotherProperty" : "value2",
    }
second_event_response = client.create_event(
    event="my_event",
    entity_type="user",
    entity_id="uid",
    target_entity_type="item",
    target_entity_id="iid",
    properties=second_event_properties,
    event_time=datetime(2014, 12, 13, 21, 38, 45, 618000, pytz.utc))
```
  </div>
  <div data-tab="Ruby SDK" data-lang="ruby">
```ruby
require 'predictionio'

event_client = PredictionIO::EventClient.new('YOUR_ACCESS_KEY')
event_client.create_event('my_event', 'user', 'uid',
                          'targetEntityType' => 'item',
                          'targetEntityId' => 'iid',
                          'eventTime' => '2004-12-13T21:39:45.618Z',
                          'properties' => { 'someProperty' => 'value1',
                                            'anotherProperty' => 'value2' })
```
  </div>
  <div data-tab="Java SDK" data-lang="java">
```java
(coming soon)
```
  </div>
</div>


Sample response:

```
HTTP/1.1 201 Created
Server: spray-can/1.2.1
Date: Wed, 10 Sep 2014 22:51:33 GMT
Content-Type: application/json; charset=UTF-8
Content-Length: 41

{"eventId":"AAAABAAAAQDP3-jSlTMGVu0waj8"}
```


## Using Event API

### Event Creation API

URL: `http://localhost:7070/events.json?accessKey=yourAccessKeyString`

Field | Type | Description
:---- | :----| :-----
`accessKey` | String | The Access Key for your App

The event creation support many commonly used data. POST request body:

Field | Type | Description
:---- | :----| :-----
`event` | String | Name of the event.
        | | (Examples: "sign-up", "rate", "view", "buy").
        | | **Note**: All event names start with "$" and "pio_" are reserved
        | | and shouldn't be used as your custom event name (eg. "$set").
`entityType` | String | The entity type. It is the namespace of the entityId and
             | | analogous to the table name of a relational database. The
             | | entityId must be unique within same entityType.
             | | **Note**: All entityType names start with "$" and "pio_" are
             | | reserved and shouldn't be used.
`entityId` | String | The entity ID. `entityType-entityId` becomes the unique
           | | identifier of the entity. For example, you may have entityType
           | | named `user`, and different entity IDs, say `1` and `2`. In this
           | | case, `user-1` and `user-2` uniquely identifies | these two
           | | entities.
`targetEntityType` | String | (Optional) The target entity type.
                   | | **Note**: All entityType names start with "$" and "pio_"
                   | | are reserved and shouldn't be used.
`targetEntityId` | String | (Optional) The target entity ID.
`properties` | JSON | (Optional) See **Note About Properties** below
             | | **Note**: All peroperty names start with "$" and "pio_"
             | | are reserved and shouldn't be used as keys inside `properties`.
`eventTime` | String | (Optional) The time of the event. Although Event Server's
            | | current system time and UTC timezone will be used if this is
            | | unspecified, it is highly recommended that this time should be
            | | generated by the client application in order to accurately
            | | record the time of the event.
            | |  Must be in ISO 8601 format (e.g.
            | | `2004-12-13T21:39:45.618Z`, or `2014-09-09T16:17:42.937-08:00`).


#### Note About Properties

`properties` can be associated with an *entity* or *event*:

1.  `properties` **associated with an *entity*:**

    The following special events are reserved for updating entities and their properties:
    -  `"$set"` event: Set properties of an entity (also implicitly create the entity). To change properties of entity, you simply set the corresponding properties with value again.
    -  `"$unset"` event: Unset properties of an entity. It means treating the specified properties as not existing anymore. Note that the field `properties` cannot be empty for `$unset` event.
    -  `"$delete"` event: delete the entity.

    There is no `targetEntityId` for these special events.

    For example, setting `properties` of `birthday` and `address` for entity `user-1`:

    ```json
    {
      "event" : "$set",
      "entityType" : "user",
      "entityId" : "1",
      "properties" : {
        "birthday" : "1984-10-11",
        "address" : "1234 Street, San Francisco, CA 94107"
      }
    }
    ```

    **Note** that the properties values of the entity will be aggregated based on these special events and the eventTime. The state of the entity is different depending on the time you are looking at the data.

    For example, let's say the following special events are recorded for user-2 with the given eventTime:

    On `2014-09-09T...`, create `$set` event for user-2 with properties a = 3 and b = 4:

    ```json
    {
      "event" : "$set",
      "entityType" : "user",
      "entityId" : "2",
      "properties" : {
        "a" : 3,
        "b" : 4
      },
      "eventTime" : "2014-09-09T16:17:42.937-08:00"
    }
    ```
    After this eventTime, user-2 is created and has properties of a = 3 and b = 4.

    Then, on `2014-09-10T...`, create `$set` event for user-2 with properties b = 5 and c = 6:

    ```json
    {
      "event" : "$set",
      "entityType" : "user",
      "entityId" : "2",
      "properties" : {
        "b" : 5,
        "c" : 6
      },
      "eventTime" : "2014-09-10T13:12:04.937-08:00"
    }
    ```
    After this eventTime, user-2 has properties of a = 3, b = 5 and c = 6. Note that property `b` is updated with latest value.

    Then, on `2014-09-11T...`, create `$unset` event for user-2 with properties b:

    ```json
    {
      "event" : "$unset",
      "entityType" : "user",
      "entityId" : "2",
      "properties" : {
        "b" : null
      },
      "eventTime" : "2014-09-11T14:17:42.456-08:00"
    }
    ```
    After this eventTime, user-2 has properties of a = 3, and c = 6. Note that property `b` is removed.

    Then, on `2014-09-12T...`, create `$delete` event for user-2:

    ```json
    {
      "event" : "$delete",
      "entityType" : "user",
      "entityId" : "2",
      "eventTime" : "2014-09-12T16:13:41.452-08:00"
    }
    ```
    After this eventTime, user-2 is removed.

    Then, on `2014-09-13T...`, create `$set` event for user-2 with empty properties:

    ```json
    {
      "event" : "$set",
      "entityType" : "user",
      "entityId" : "2",
      "eventTime" : "2014-09-13T16:17:42.143-08:00"
    }
    ```
    After this eventTime, user-2 is created again with empty properties.

    As you have seen in the example above, the state of user-2 is different depending on the time you are looking at the data.

2.   `properties` **associated with an *event*:**

    The properties can contain extra information about the event.

    For example `user-1` may have a `rate` event on `item-1` with rating value of `4`. The `rating` is stored inside the properties.

    ```json
    {
      "event" : "rate",
      "entityType" : "user",
      "entityId" : "1",
      "targetEntityType" : "item",
      "targetEntityId" : "1",
      "properties" : {
        "rating" : 4
      }
    }
    ```

## Debugging Recipes

WARNING: The following API are mainly for development or debugging purpose
only. They should not be supported by SDK nor used by real application under
normal circumstances and they are subject to changes.

The `accessKey` query parameter is mandatory.

Replace `<your_accessKey>` and `<your_eventId>` by a real one in the following:

### Get an Event

```
$ curl -i -X GET http://localhost:7070/events/<your_eventId>.json?accessKey=<your_accessKey>
```

### Delete an Event

```
$ curl -i -X DELETE http://localhost:7070/events/<your_eventId>.json?accessKey=<your_accessKey>
```

### Get All Events of an App

> Use cautiously!

```
$ curl -i -X GET http://localhost:7070/events.json?accessKey=<your_accessKey>
```

In addition, the following *optional* parameters are supported:

- `startTime`: time in ISO8601 format. Return events with `eventTime >= startTime`.
- `untilTime`: time in ISO8601 format. Return events with `eventTime < untilTime`.
- `entityType`: String. The entityType. Return events for this `entityType` only.
- `entityId`: String. The entityId. Return events for this `entityId` only.
- `limit`: Integer. The number of record events returned. Default is 20. -1 to
  get all.
- `reversed`: Boolean. When used with both `entityType` and `entityId` specified, returns events in reversed chronological order. Default is false.

> If you are using <code>curl</code> with the <code>&</code> symbol, you should quote the entire URL by using single or double quotes.

For example, get all events of an app with `eventTime >= startTime`

```
$ curl -i -X GET "http://localhost:7070/events.json?accessKey=<your_accessKey>&startTime=<time in ISO8601 format>"
```

For example, get all events of an app with `eventTime < untilTime`:

```
$ curl -i -X GET "http://localhost:7070/events.json?accessKey=<your_accessKey>&untilTime=<time in ISO8601 format>"
```

For example, get all events of an app with `eventTime >= startTime` and `eventTime < untilTime`:

```
$ curl -i -X GET "http://localhost:7070/events.json?accessKey=<your_accessKey>&startTime=<time in ISO8601 format>&untilTime=<time in ISO8601 format>"
```

For example, get all events of a specific entity with `eventTime < untilTime`:

```
$ curl -i -X GET "http://localhost:7070/events.json?accessKey=<your_accessKey>&entityType=<your_entityType>&entityId=<your_entityId>&untilTime=<time in ISO801 format>"
```

### Delete All Events of an App

> Please use the following CLI command.

```
$ pio app data-delete <your_app_name>
```
