---
title: Language Samples
hidden: true
---

## Plain Text

This is a sample code block with no language.

```
$ $PIO_HOME/bin/pio eventserver
$ cd /path/to/engine
$ ../bin/pio train
$ ../bin/pio deploy
```

## Scala

This is a sample Scala code block:

```scala
class Preparator
  extends PPreparator[TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(ratings = trainingData.ratings)
  }
}

class PreparedData(
  val ratings: RDD[Rating]
)
```

## Ruby

This is a sample Ruby code block:

```ruby
class UsersController < ApplicationController
  def index
    @users = User.order('reviews_count DESC').limit(20)
  end
end
```

## JSON

This is a sample JSON code block:

```json
{
  ...
  "algorithms": [
    {
      "name": "als",
      "params": {
        "rank": 10,
        "numIterations": 20,
        "lambda": 0.01
      }
    }
  ]
  ...
}
```

## PHP

This is a sample PHP code block:

```php
<?php
require_once("vendor/autoload.php");
use predictionio\EventClient;

$client = new EventClient(<ACCESS KEY>, <URL OF EVENTSERVER>);

// Set the 4 properties for a user
$client->createEvent(array(
  'event' => '$set',
  'entityType' => 'user',
  'entityId' => <USER ID>,
  'properties' => array(
    'attr0' => <VALUE OF ATTR0>,
    'attr1' => <VALUE OF ATTR1>,
    'attr2' => <VALUE OF ATTR2>,
    'plan' => <VALUE OF PLAN>
    )
  ));
?>
```

## Python

This is a sample Python code block:

```python
from predictionio import EventClient
from datetime import datetime
import pytz
client = EventClient(app_id=4, url="http://localhost:7070")

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
