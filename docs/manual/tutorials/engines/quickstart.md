---
layout: docs
title: Quick Start - Using a Built-in Engine
---

# Quick Start - Using a Built-in Engine

This is a quick start guide of using a PredictionIO's built-in engine and its
SDKs to write a very simple app. It assumes that you have [installed
PredictionIO server](/install/).

Let's start with a classic example in Machine Learning - build a recommendation
engine. We are going to launch a recommendation engine instance that can:

* collect *real-time event data* from your app through REST API or SDKs;
* update the predictive model with *new data* regularly and automatically;
* answer *prediction query* through REST API or SDKs.

# Create a Simple App Project

Create a new project directory for a simple app that will use the engine.

```
$ mkdir quickstartapp
$ cd quickstartapp
```

# Install SDK

To communicate with PredictionIO server, we can use a PredictionIO SDK of a specific programming language:

<div class="codetabs">
<div data-lang="PHP SDK">
<p>To use the PredictionIO PHP SDK, we are going to install it with Composer:</p>
<p>1. Create a file called ``composer.json`` in your project directory, which adds predictionio/predictionio as a dependency. It should look like this:</p>
{% highlight json %}
{
    "require": {
        "predictionio/predictionio": "~0.6.0"
    }
}
{% endhighlight %}

<p>2. Install Composer:</p>
{% highlight bash %}
$ curl -sS https://getcomposer.org/installer | php -d detect_unicode=Off
{% endhighlight %}

<p>3. Use Composer to install your dependencies:</p>
{% highlight bash %}
$ php composer.phar install
{% endhighlight %}

<p>Now you are ready to write the actual PHP code.</p>
</div>
<div data-lang="Python SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
<div data-lang="Ruby SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
<div data-lang="Java SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
</div>


# Collect Data into PredictionIO

## Launch the Data API Server

```
$ $PIO_HOME/bin/pio dataapi
```
where `$PIO_HOME` is the installation directory of PredictionIO. As long as the
Data API server is running, PredictionIO keeps listening to new data.

## Import Data

We are going to write a script that generates some random data and simulates
data collection. Data API can collect data from your application in real-time.
In the `quickstartapp` directory:

<div class="codetabs">
<div data-lang="PHP SDK">
<p>Create <em>import.php</em> as below.</p>
{% highlight php %}
<?php
    // use composer's autoloader to load PredictionIO PHP SDK
    require_once("vendor/autoload.php");
    use PredictionIO\PredictionIOClient;
    $client = PredictionIOClient::factory(array("appkey" => "<your app key>"));

    // generate 10 users, with user ids 1,2,....,10
    for ($i=1; $i<=10; $i++) {
        echo "Add user ". $i . "\n";
        $command = $client->getCommand('create_user', array('pio_uid' => $i));
        $response = $client->execute($command);
    }

    // generate 50 items, with item ids 1,2,....,50
    // assign type id 1 to all of them
    for ($i=1; $i<=50; $i++) {
        echo "Add item ". $i . "\n";
        $command = $client->getCommand('create_item', array('pio_iid' => $i, 'pio_itypes' => 1));
        $response = $client->execute($command);
    }

    // each user randomly views 10 items
    for ($u=1; $u<=10; $u++) {
        for ($count=0; $count<10; $count++) {
            $i = rand(1, 50); // randomly pick an item
            echo "User ". $u . " views item ". $i ."\n";
            $client->identify($u);
            $client->execute($client->getCommand('record_action_on_item', array('pio_action' => 'view', 'pio_iid' => $i)));
        }
    }
?>
{% endhighlight %}
and run it:
{% highlight bash %}
$ php import.php
{% endhighlight %}
</div>

<div data-lang="Python SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>

<div data-lang="Ruby SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>

<div data-lang="Java SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
</div>




# Launch an Engine Instance

Each engine deals with one specific prediction problem. For instance, Item
Recommendation Engine (itemrec) is responsible for making personalized item
(e.g. product or content) recommendation to each user.

> **What is an Engine Instance?**
>
> You can launch one or more *engine instance* from an engine. It means that you
can run multiple recommendation *engine instances* at the same time with
different settings, or even for different projects.

To launch an engine instance for this simple app, first create an engine
instance project:

```
$ $PIO_HOME/bin/pio instance io.prediction.engines.itemrec
$ cd io.prediction.engines.itemrec
```

Edit `params/datasource.json` and modify the value of `appId` to fit your app.

Now, you can kick start the predictive model training with:

```
$ $PIO_HOME/bin/pio train
...
2014-09-11 16:25:44,591 INFO  spark.SparkContext - Job finished: collect at Workflow.scala:674, took 0.078664 s
2014-09-11 16:25:44,737 INFO  workflow.CoreWorkflow$ - Saved engine instance with ID: KxOsC2FRSdGGe1lv0oaHiw
```

If your training was successful, you should see the lines shown above. Now you are ready to deploy the instance:

```
$ $PIO_HOME/bin/pio deploy
...
[INFO] [09/11/2014 16:26:16.525] [pio-server-akka.actor.default-dispatcher-2] [akka://pio-server/user/IO-HTTP/listener-0] Bound to localhost/127.0.0.1:8000
[INFO] [09/11/2014 16:26:16.526] [pio-server-akka.actor.default-dispatcher-5] [akka://pio-server/user/master] Bind successful. Ready to serve.
```

Notice that the `deploy` command runs the engine instance in the foreground. Now
we are ready to take a look at the results!

# Retrieve Prediction Results

<div class="codetabs">
<div data-lang="PHP SDK">
<p>Create a file <em>show.php</em> in quickstartapp directory with this code:</p>
<p>Replace <APP ID> with your engine name. It should be named 'TODO' in this example.</p>
{% highlight php %}
<?php
    // use composer's autoloader to load PredictionIO PHP SDK
    require_once("vendor/autoload.php");
    use PredictionIO\PredictionIOClient;
    $client = PredictionIOClient::factory(array("appkey" => "<your app key>"));

    // generate 10 users, with user ids 1,2,....,10
    for ($i=1; $i<=10; $i++) {
        echo "Add user ". $i . "\n";
        $command = $client->getCommand('create_user', array('pio_uid' => $i));
        $response = $client->execute($command);
    }

    // generate 50 items, with item ids 1,2,....,50
    // assign type id 1 to all of them
    for ($i=1; $i<=50; $i++) {
        echo "Add item ". $i . "\n";
        $command = $client->getCommand('create_item', array('pio_iid' => $i, 'pio_itypes' => 1));
        $response = $client->execute($command);
    }

    // each user randomly views 10 items
    for ($u=1; $u<=10; $u++) {
        for ($count=0; $count<10; $count++) {
            $i = rand(1, 50); // randomly pick an item
            echo "User ". $u . " views item ". $i ."\n";
            $client->identify($u);
            $client->execute($client->getCommand('record_action_on_item', array('pio_action' => 'view', 'pio_iid' => $i)));
        }
    }
?>
{% endhighlight %}
and run it:
{% highlight bash %}
$ php show.php
{% endhighlight %}
</div>

<div data-lang="Python SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>

<div data-lang="Ruby SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>

<div data-lang="Java SDK">
{% highlight bash %}
(TODO)
{% endhighlight %}
</div>
</div>

Well done! You have created a simple, but production-ready app with PredictionIO
recommendation engine.

Next: Learn more about [collecting data using Data API](/dataapi.html).
