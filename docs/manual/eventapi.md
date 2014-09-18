---
layout: docs
title: Event API
---

# Collecting Data through Event API

*Event Server* is designed to collect data into PredictionIO in an event-based
style. Once the Event Server is launched, your application can send data to it
through its *Event API* with HTTP requests or with the *EventClient* of
PredictionIO's SDKs.

> All PredictionIO-compliant engines support the data store (i.e. HBase) and
data format used by the Event Server.
> You may also [modify DataSource](/cookbook/existingdatasource.html) of an
engine to read data directly from your existing data store.


## Launching the Event Server

> Before launching the Event Server, make sure that your event data store
backend is properly configured and is running. By default, PredictionIO uses
HBase, and a quick configuration can be found
[here](/install/install-sourcecode.html#hbase).

Everything about PredictionIO can be done through the `bin/pio` command.

> For this section, `$PIO_HOME` refers to the location where you have installed
PredictionIO.

```
$ cd $PIO_HOME
$ bin/pio eventserver
```
### Check server status

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

## Using Event API

### Event Creation API

The event creation support many commonly used data.

Field | Description
:---- | :----------
`appId` | App ID for separating your data set between different
        | applications.
`event` | Name of the event. (Examples: "sign-up", "rate", "view", "buy")
`entityType` | The entity type. It is the namespace of the entityId and
             | analogous to the table name of a relational database. The
             | entityId must be unique within same entityType.
`entityId` | The entity ID. `entityType-entityId` becomes the unique
           | identifier of the entity. For example, you may have entityType
           | named `user`, and different entity IDs, say `1` and `2`. In this
           | case, `user-1` and `user-2` uniquely identifies | these two
           | entities.
`targetEntityType` | (Optional) The target entity type.
`targetEntityId` | (Optional) The target entity ID.
`properties` | (Optional) JSON object. See **Note** below.
`eventTime` | (Optional) The time of the event. Current time and UTC timezone
            | will be used if unspecified. Must be in ISO 8601 format (e.g.
            | `2004-12-13T21:39:45.618Z`, or `2014-09-09T16:17:42.937-08:00`).
`tags` | (Optional) JSON array of strings. Empty list will be used if
       | unspecified.
`predictionKey` | (Optional) Reserved. TBD.
`creationTime` | (Optional) Creation time of this event (not the time when this
               | event happened). Current time and UTC timezone will be used if
               | unspecified. Must be in ISO 8601 format (e.g.
               | `2004-12-13T21:39:45.618Z`, or
               | `2014-09-09T16:17:42.937-08:00`).

#### Note
`properties` can be associated with either an entity or an event.

-   `properties` associated with an entity:

    Entity `user-1` may have `properties` of `birthday` and `address`. To set
    and unset properties for the entity, use special event `$set` and
    `$unset` to create an event of the entity.

    ```
    {
      "appId" : 4,
      "event" : "$set",
      "entityType" : "user"
      "entityId" : "1",
      "properties" : {
        "birthday" : "1984-10-11",
        "address" : "1234 Street, San Francisco, CA 94107"
      }
    }
    ```

-   `properties` associated with an event:

    `user-1` may have a `rate` event on `item-1` with rating value of `4.5`.

    ```
    {
      "appId" : 4,
      "event" : "rate",
      "entityType" : "user"
      "entityId" : "1",
      "targetEntityType" : "item",
      "targetEntityId" : "1"
      "properties" : {
        "rating" : 4.5
      }
    }
    ```

You may connect to the Event Server with HTTP request or by using one of many
**PredictionIO SDKs**. You may also use [Bulk Loading](/bulkloading.html) for
your old data.

### Creating Your First Event

The following shows how one can create an event involving a single entity.

<div class="codetabs">
<div data-lang="Raw HTTP">
{% highlight bash %}
$ curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "predictionKey" : "my_prediction_key",
  "appId" : 4,
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
  "eventTime" : "2004-12-13T21:39:45.618-07:00",
  "tags" : ["tag1", "tag2"],
  "creationTime" : "2014-09-01T21:39:45.618-08:00"
}'
{% endhighlight %}
</div>
<div data-lang="PHP SDK">
{% highlight php %}
<?php
  require_once("vendor/autoload.php");

  use predictionio\EventClient;

  $appId = 4;
  $client = new EventClient($appId);
  $response = $client->createEvent(array(
                        'predictionKey' => 'my_prediction_key',
                        'appId' => 4,
                        'event' => 'my_event',
                        'entityType' => 'user',
                        'entityId' => 'uid',
                        'properties' => array('prop1'=>1, 
                                              'prop2'=>'value2',
                                              'prop3'=>array(1,2,3),
                                              'prop4'=>true,
                                              'prop5'=>array('a','b','c'),
                                              'prop6'=>4.56
                                        ),
                        'eventTime' => '2004-12-13T21:39:45.618-07:00',
                        'tags' => array('tag1', 'tag2'),
                        'creationTime' => '2014-09-01T21:39:45.618-08:00'
                       ));
?>
{% endhighlight %}
</div>
<div data-lang="Python SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
<div data-lang="Ruby SDK">
{% highlight ruby %}
require 'predictionio'

event_client = PredictionIO::EventClient.new(4)
event_client.create_event('my_event', 'user', 'uid',
                          'predictionKey' => 'my_prediction_key',
                          'eventTime' => '2004-12-13T21:39:45.618-07:00',
                          'tags' => %w(tag1 tag2),
                          'creationTime' => '2014-09-01T21:39:45.618-08:00',
                          'properties' => { 'prop1' => 1,
                                            'prop2' => 'value2',
                                            'prop3' => [1, 2, 3],
                                            'prop4' => true,
                                            'prop5' => %w(a b c),
                                            'prop6' => 4.56 })
{% endhighlight %}
</div>
<div data-lang="Java SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
</div>

The following shows how one can create an event involving two entities (with
`targetEntity`).

<div class="codetabs">
<div data-lang="Raw HTTP">
{% highlight bash %}
$ curl -i -X POST http://localhost:7070/events.json \
-H "Content-Type: application/json" \
-d '{
  "appId" : 4,
  "predictionKey" : "my_prediction_key",
  "event" : "my_event",
  "entityType" : "user",
  "entityId" : "uid",
  "targetEntityType" : "item",
  "targetEntityId" : "iid",
  "properties" : {
    "someProperty" : "value1",
    "anotherProperty" : "value2"
  }
  "eventTime" : "2004-12-13T21:39:45.618Z",
  "tags" : ["tag1", "tag2"],
  "creationTime" : "2014-09-01T21:40:45.123+01:00"
}'
{% endhighlight %}
</div>
<div data-lang="PHP SDK">
{% highlight php %}
<?php
  require_once("vendor/autoload.php");

  use predictionio\EventClient;

  $appId = 4;
  $client = new EventClient($appId);
  $response = $client->createEvent(array(
                        'appId' => 4,
                        'predictionKey' => 'my_prediction_key',
                        'event' => 'my_event',
                        'entityType' => 'user',
                        'entityId' => 'uid',
                        'targetEntityType' => 'item',
                        'targetEntityId' => 'iid',
                        'properties' => array('someProperty'=>'value1',
                                              'anotherProperty'=>'value2'),
                        'eventTime' => '2004-12-13T21:39:45.618Z',
                        'tags' => array('tag1', 'tag2'),
                        'creationTime' => '2004-12-13T21:39:45.618Z',
                       ));
?>
{% endhighlight %}
</div>
<div data-lang="Python SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
<div data-lang="Ruby SDK">
{% highlight ruby %}
require 'predictionio'

event_client = PredictionIO::EventClient.new(4)
event_client.create_event('my_event', 'user', 'uid',
                          'targetEntityType' => 'item',
                          'targetEntityId' => 'iid'
                          'predictionKey' => 'my_prediction_key',
                          'eventTime' => '2004-12-13T21:39:45.618Z',
                          'tags' => %w(tag1 tag2),
                          'creationTime' => '2014-09-01T21:40:45.123+01:00',
                          'properties' => { 'someProperty' => 'value1',
                                            'anotherProperty' => 'value2' })
{% endhighlight %}
</div>
<div data-lang="Java SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
</div>


Sample response:

```
HTTP/1.1 201 Created
Server: spray-can/1.2.1
Date: Wed, 10 Sep 2014 22:51:33 GMT
Content-Type: application/json; charset=UTF-8
Content-Length: 68

{"eventId":"4-1102999185618-my_event-user-uid--7574530243528847393"}
```


## Debugging Recipes

Replace `<your_eventId>` by a real one in the following.

### Get an Event

```
$ curl -i -X GET http://localhost:7070/events/<your_eventId>.json
```

### Delete an Event

```
$ curl -i -X DELETE http://localhost:7070/events/<your_eventId>.json
```

### Get All Events of an appId

> Use cautiously!

```
$ curl -i -X GET http://localhost:7070/events.json?appId=<your_appId>
```

### Delete All Events of an appId

```
$ curl -i -X DELETE http://localhost:7070/events.json?appId=<your_appId>
```

### Get All Events of an appId within a Time Range

-   `eventTime >= startTime`

    ```
    $ curl -i -X GET http://localhost:7070/events.json?appId=<your_appId>&startTime=<time in ISO8601 format>
    ```
-   `eventTime < untilTime`

    ```
    $ curl -i -X GET http://localhost:7070/events.json?appId=<your_appId>&untilTime=<time in ISO8601 format>
    ```

-   `eventTime >= startTime && eventTime < untilTime`

    ```
    $ curl -i -X GET http://localhost:7070/events.json?appId=2&startTime=<time in ISO8601 format>&untilTime=<time in ISO8601 format>
    ```

Example:

```
$ curl -i -X GET http://localhost:7070/events.json?appId=2&startTime=2014-08-30T08:45:51.566Z&untilTime=2014-08-30T08:45:51.591Z
```
