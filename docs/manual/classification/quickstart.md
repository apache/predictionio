---
layout: docs
title: Classification Quick Start
---

# Quick Start - Classification Engine Template

An engine template is a basic skeleton of an engine. PredictionIO's Classification Engine Template (/templates/scala-parallel-classification) has integrated **Apache Spark MLlib**'s Naive Bayes algorithm by default.

The default use case of Classification Engine Template is to predict the service plan (*plan*) a user will subscribe to based on his 3 properties: *attr0*, *attr1* and *attr2*.

You can customize it easily to fit your specific use case and needs.

We are going to show you how to create your own classification engine for production use based on this template.

## Install PredictionIO

First you need to [install PredictionIO {{site.pio_version}}]({{site.baseurl}}/install/)

**0.8.2 contains schema changes from the previous versions, if you have installed the previous versions, you may need to clear both HBase and Elasticsearch. See more [here](../resources/schema-change.html).**


Let's say you have installed PredictionIO at */home/yourname/predictionio/*.
For convenience, add PredictionIO's binary command path to your PATH, i.e. /home/yourname/predictionio/bin:

```
$ PATH=$PATH:/home/yourname/predictionio/bin; export PATH
```

and please make sure that PredictionIO EventServer, which collects data, is running:

```
$ pio eventserver
```



## Create a Sample App

Let's create a sample app called "MyApp2" now. An app represents the application that generates the data, e.g. an enterprise service website.

```
$ pio app new MyApp2
```

You should find the following in the console output:

```
...
2014-11-18 12:40:57,424 INFO  tools.Console$ - Initialized Event Store for this app ID: 2.
2014-11-18 12:40:57,456 INFO  tools.Console$ - Created new app:
2014-11-18 12:40:57,457 INFO  tools.Console$ -       Name: MyApp2
2014-11-18 12:40:57,458 INFO  tools.Console$ -         ID: 2
2014-11-18 12:40:57,459 INFO  tools.Console$ - Access Key: 3pr1YWsSONhpalMGAB2Jry41PUuh7Mve3nOPx5draGD9CKHNXVtZXskBeSnJq3vz
```

Take note of the `Access Key` and `App ID`.
You will need the `Access Key` to refer to "MyApp2" when you collect data.
At the same time, you will use `App ID` to refer to "MyApp2" in engine code.

## Create a new Engine from an Engine Template

Now let's create a new engine called *MyClassification* by cloning theMLlib Classification engine template:

```
$ cp -r /home/yourname/predictionio/templates/scala-parallel-classification MyClassification
$ cd MyClassification
```
* Assuming /home/yourname/predictionio is the installation directory of PredictionIO.*

## Collecting Data

Next, let's collect some training data for the app of this Engine.
For model training, Classification Engine Template reads 4 properties of a user record: attr0, attr1, attr2 and plan.

You can send these data to PredictionIO EventServer in real-time easily by making a HTTP request or through the `EventClient` of a SDK


<div class="codetabs">
<div data-lang="Python SDK">

{% highlight python %}
import predictionio

client = predictionio.EventClient(
    access_key=<ACCESS KEY>,
    url=<URL OF EVENTSERVER>,
    threads=5,
    qsize=500
)

# Set the 4 properties for a user
client.create_event(
    event="$set",
    entity_type="user",
    entity_id=<USER ID>,
    properties= {
      "attr0" : int(<VALUE OF ATTR0>),
      "attr1" : int(<VALUE OF ATTR1>),
      "attr2" : int(<VALUE OF ATTR2>),
      "plan" : int(<VALUE OF PLAN>)
    }
)

'''
# You may also set the properties one by one
client.create_event(
    event="$set",
    entity_type="user",
    entity_id=<USER ID>,
    properties= {
      "attr0" : int(<VALUE OF ATTR0>)
    }
)
client.create_event(
    event="$set",
    entity_type="user",
    entity_id=<USER ID>,
    properties= {
      "attr1" : int(<VALUE OF ATTR1>)
    }
)
client.create_event(
    event="$set",
    entity_type="user",
    entity_id=<USER ID>,
    properties= {
      "attr2" : int(<VALUE OF ATTR2>)
    }
)
client.create_event(
    event="$set",
    entity_type="user",
    entity_id=<USER ID>,
    properties= {
      "plan" : int(<VALUE OF PLAN>)
    }
)
'''
{% endhighlight %}

</div>

<div data-lang="PHP SDK">

{% highlight php %}
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

// You may also set the properties one by one
$client->createEvent(array(
   'event' => '$set',
   'entityType' => 'user',
   'entityId' => <USER ID>,
   'properties' => array(
     'attr0' => <VALUE OF ATTR0>
   )
));

$client->createEvent(array(
   'event' => '$set',
   'entityType' => 'user',
   'entityId' => <USER ID>,
   'properties' => array(
     'attr1' => <VALUE OF ATTR1>
   )
));

$client->createEvent(array(
   'event' => '$set',
   'entityType' => 'user',
   'entityId' => <USER ID>,
   'properties' => array(
     'attr2' => <VALUE OF ATTR2>
   )
));

$client->createEvent(array(
   'event' => '$set',
   'entityType' => 'user',
   'entityId' => <USER ID>,
   'properties' => array(
     'plan' => <VALUE OF PLAN>
   )
));

?>
{% endhighlight %}
</div>


<div data-lang="Ruby SDK">

{% highlight ruby %}
# Create a client object.
client = PredictionIO::EventClient.new(<ACCESS KEY>, <URL OF EVENTSERVER>)

# Set the 4 properties for a user.
client.create_event(
  '$set',
  'user',
  <USER ID>, {
    'properties' => {
      'attr0' => <VALUE OF ATTR0 (integer)>,
      'attr1' => <VALUE OF ATTR1 (integer)>,
      'attr2' => <VALUE OF ATTR2 (integer)>,
      'plan' => <VALUE OF PLAN (integer)>,
    }
  }
)

# You may also set the properties one by one.
client.create_event(
  '$set',
  'user',
  <USER ID>, {
    'properties' => {
      'attr0' => <VALUE OF ATTR0 (integer)>
    }
  }
)

client.create_event(
  '$set',
  'user',
  <USER ID>, {
    'properties' => {
      'attr1' => <VALUE OF ATTR1 (integer)>,
    }
  }
)

# Etc...

{% endhighlight %}

</div>

<div data-lang="Java SDK">

{% highlight java %}
(coming soon)
{% endhighlight %}

</div>

<div data-lang="REST API">

{% highlight rest %}

# Set the 4 properties for a user

curl -i -X POST <URL OF EVENTSERVER>/events.json?accessKey=<ACCESS KEY> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : <USER ID>,
  "properties" : {
    "attr0" : 0,
    "attr1" : 1,
    "attr2" : 0,
    "plan" : 1
  }
  "eventTime" : <TIME OF THIS EVENT>
}'

# You may also set the properties one by one

curl -i -X POST <URL OF EVENTSERVER>/events.json?accessKey=<ACCESS KEY> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : <USER ID>,
  "properties" : {
    "attr0" : 0
  }
  "eventTime" : <TIME OF THIS EVENT>
}'

curl -i -X POST <URL OF EVENTSERVER>/events.json?accessKey=<ACCESS KEY> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : <USER ID>,
  "properties" : {
    "attr1" : 1
  }
  "eventTime" : <TIME OF THIS EVENT>
}'

curl -i -X POST <URL OF EVENTSERVER>/events.json?accessKey=<ACCESS KEY> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : <USER ID>,
  "properties" : {
    "attr2" : 0
  }
  "eventTime" : <TIME OF THIS EVENT>
}'

curl -i -X POST <URL OF EVENTSERVER>/events.json?accessKey=<ACCESS KEY> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : <USER ID>,
  "properties" : {
    "plan" : 1
  }
  "eventTime" : <TIME OF THIS EVENT>
}'
{% endhighlight %}

</div>
</div>


You may use the sample data from MLlib repo for demonstration purpose. Execute the following to get the data set:

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_naive_bayes_data.txt --create-dirs -o data/sample_naive_bayes_data.txt
```

A python import script `import_eventserver.py` is provided to import the data to Event Server using Python SDK. Replace the value of access_key parameter by your `Access Key`.

```
$ python data/import_eventserver.py --access_key obbiTuSOiMzyFKsvjjkDnWk1vcaHjcjrv9oT3mtN3y6fOlpJoVH459O1bPmDzCdv
```

You should see the following output:

```
Importing data...
6 events are imported.
```

Now the training data is stored as events inside the Event Store.


## Deploy the Engine as a Service

Now you can deploy the engine.  Make sure the appId defined in the file `engine.json` match your `App ID`:

```
...
"datasource": {
  "appId": 2
},
...
```

To build *MyClassification* and deploy it as a service:

```
$ pio build
$ pio train
$ pio deploy
```

This will deploy an engine that binds to http://localhost:8000. You can visit that page in your web browser to check its status.

![Engine Status]({{ site.baseurl }}/images/engine-server.png)

|

Now, You can try to retrieve predicted results.
For example, to predict the label (i.e. *plan* in this case) of a user with attr0=2, attr1=0 and attr2=0, you send this JSON { "features": [2, 0, 0] } to the deployed engine and it will return a JSON of the predicted plan.
Simply send a query by making a HTTP request or through the `EngineClient` of a SDK:

<div class="codetabs">
<div data-lang="Python SDK">

{% highlight python %}
(coming soon - see REST API)
{% endhighlight %}

</div>

<div data-lang="PHP SDK">

{% highlight php %}
<?php
require_once("vendor/autoload.php");
use predictionio\EngineClient;

$client = new EngineClient('http://localhost:8000');

$response = $client->sendQuery(array('features'=> array(2, 0, 0)));
print_r($response);

?>
{% endhighlight %}
</div>


<div data-lang="Ruby SDK">

{% highlight ruby %}
(coming soon)
{% endhighlight %}

</div>

<div data-lang="Java SDK">

{% highlight java %}
(coming soon)
{% endhighlight %}

</div>

<div data-lang="REST API">

{% highlight rest %}
$ curl -H "Content-Type: application/json" -d '{ "features": [2, 0, 0] }' http://localhost:8000/queries.json

{"label":0.0}
{% endhighlight %}

</div>
</div>


Similarly, to predict the label (i.e. *plan* in this case) of a user with attr0=4, attr1=3 and attr2=8, you send this JSON { "features": [4, 3, 8] } to the deployed engine and it will return a JSON of the predicted plan.

Your MyEngine is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.

#### [Next: DASE Components Explained](dase.html)
