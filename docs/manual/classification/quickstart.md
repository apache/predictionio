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

The engine is going to process the data of an app. Let's create a sample app called "MyApp" now:

```
$ pio app new MyApp
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

You can send these data to PredictionIO EventServer in real-time easily through the EventAPI with a SDK or HTTP call:

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
(coming soon)
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
(coming soon)
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

```
$ curl -H "Content-Type: application/json" -d '{ "features": [2, 0, 0] }' http://localhost:8000/queries.json

{"label":0.0}

```

To predict the label (i.e. *plan* in this case) of a user with attr0=4, attr1=3 and attr2=8, you send this JSON { "features": [4, 3, 8] } to the deployed engine and it will return a JSON of the predicted plan.

```
$ curl -H "Content-Type: application/json" -d '{ "features": [4, 3, 8] }' http://localhost:8000/queries.json

{"label":2.0}
```

Your MyEngine is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.

#### [Next: DASE Components Explained](dase.html)
