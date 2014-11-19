---
layout: docs
title: Recommendation Quick Start
---

# Quick Start - Recommendation Engine Template

An engine template is a basic skeleton of an engine. PredictionIO's
Recommendation Engine Template (/templates/scala-parallel-recommendation) has
integrated **Apache Spark MLlib**'s Collaborative Filtering algorithm by
default.  You can customize it easily to fit your specific needs.

We are going to show you how to create your own classification engine for
production use based on this template.

## Install PredictionIO

First you need to [install PredictionIO {{site.pio_version}}]({{site.baseurl}}/install/)


**0.8.2 contains schema changes from the previous versions, if you have
installed the previous versions, you may need to clear both HBase and
ElasticSearch. See more [here](../resources/schema-change.html).**


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

Let's create a sample app called "MyApp1" now. An app prepresents the application that generates the data, e.g. a movie rating app.

```
$ pio app new MyApp1
```

You should find the following in the console output:

```
...
2014-11-18 12:38:47,636 INFO  tools.Console$ - Initialized Event Store for this app ID: 1.
2014-11-18 12:38:47,721 INFO  tools.Console$ - Created new app:
2014-11-18 12:38:47,722 INFO  tools.Console$ -       Name: MyApp1
2014-11-18 12:38:47,723 INFO  tools.Console$ -         ID: 1
2014-11-18 12:38:47,724 INFO  tools.Console$ - Access Key: 3mZWDzci2D5YsqAnqNnXH9SB6Rg3dsTBs8iHkK6X2i54IQsIZI1eEeQQyMfs7b3F
```

Take note of the `Access Key` and `App ID`.
You will need the `Access Key` to refer to "MyApp1" when you collect data. 
At the same time, you will use `App ID` to refer to "MyApp1" in engine code.

## Create a new Engine from an Engine Template

Now let's create a new engine called *MyRecommendation* by cloning the MLlib Collaborative Filtering engine template:

```
$ cp -r /home/yourname/predictionio/templates/scala-parallel-recommendation MyRecommendation
$ cd MyRecommendation
```
* Assuming /home/yourname/predictionio is the installation directory of PredictionIO.*

## Collecting Data

Next, let's collect some training data for the app of this Engine.
By default, the Recommendation Engine Template supports 2 types of events: "rate" and "buy".  A user can give a rating score to an item or he can buy an item.

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

# A user rates an item
client.create_event(
    event="rate",
    entity_type="user",
    entity_id=<USER ID>,
    target_entity_type="item",
    target_entity_id=<ITEM ID>,
    properties= { "rating" : float(<RATING>) }
)

# A user buys an item
client.create_event(
        event="buy",
        entity_type="user",
        entity_id=<USER ID>,
        target_entity_type="item",
        target_entity_id=<ITEM ID>
)
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


You may use the sample movie data from MLlib repo for demonstration purpose. Execute the following to get the data set:

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_movielens_data.txt --create-dirs -o data/sample_movielens_data.txt
```

A python import script `import_eventserver.py` is provided to import the data to Event Server using Python SDK. Replace the value of access_key parameter by your `Access Key`.

```
$ python data/import_eventserver.py --access_key obbiTuSOiMzyFKsvjjkDnWk1vcaHjcjrv9oT3mtN3y6fOlpJoVH459O1bPmDzCdv
```

You should see the following output:

```
Importing data...
1501 events are imported.
```

> If you experience error simliar to the following, please update the
> Python SDK to the latest version.
>
> ```
> Traceback (most recent call last):
>  File "data/import_eventserver.py", line 55, in <module>
>      qsize=500)
>      TypeError: __init__() got an unexpected keyword argument 'access_key'
> ```

Now the movie ratings data is stored as events inside the Event Store.


## Deploy the Engine as a Service

Now you can deploy the engine.  Make sure the appId defined in the file `engine.json` match your `App ID`:

```
...
"datasource": {
  "appId": 1
},
...
```

To build *MyRecommendation* and deploy it as a service:

```
$ pio build
$ pio train
$ pio deploy
```

This will deploy an engine that binds to http://localhost:8000. You can visit that page in your web browser to check its status.

![Engine Status]({{ site.baseurl }}/images/engine-server.png)

|

Now, You can try to retrieve predicted results.
To recommend 4 movies to user whose id is 1, you send this JSON { "user": 1, "num": 4 } to the deployed engine and it will return a JSON of the recommended movies.
Simply send a query by making a HTTP request or through the `EngineClient` of a SDK:

<div class="codetabs">
<div data-lang="Python SDK">

{% highlight python %}
(coming soon -- see REST API)
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
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' http://localhost:8000/queries.json

{"productScores":[{"product":22,"score":4.072304374729956},{"product":62,"score":4.058482414005789},{"product":75,"score":4.046063009943821},{"product":68,"score":3.8153661512945325}]}
{% endhighlight %}

</div>
</div>

Your MyRecommendation is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.

#### [Next: DASE Components Explained](dase.html)
