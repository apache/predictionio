---
layout: docs
title: Building the "HelloWorld" Engine
---

# Building the "HelloWorld" Engine

This is a step-by-step guide on building your first predictive engine on PredictionIO.

Completed source code can also be found at $PIO_HOME/examples/scala-local-helloworld and $PIO_HOME/examples/java-local-helloworld

## 1. Create a new Engine

<div class="codetabs">
<div data-lang="Scala">
{% highlight bash %}
$ pio createEngine --scala HelloWorld
$ cd HelloWorld

Now you need to edit 'HelloWorld.scala'
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight bash %}
$ pio createEngine --java HelloWorld
$ cd HelloWorld

Now you need to edit 'HelloWorld.java'
{% endhighlight %}
</div>
</div>



## 2. Define Data Types

### Define TrainingData

<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
Scala code here...
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

### Define Query

<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
Scala code here...
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

### Define Model
<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
Scala code here...
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

### Define Prediction Result

## 3. Implement the Data Source
<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
Scala code here...
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

## 4. Implement an Algorithm
<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
Scala code here...
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

# Deploying the "HelloWorld" Engine Instance

After the new engine is built, it is time to deploy an engine instance of it.

Create an Engine Instance project:

```
$ (TODO)
```

Prepare training data:

```
$ cp $PIO_HOME/data/helloworld/data.csv data.csv
```

Register engine:

```
$ ../../bin/pio register
```

Train:

```
$ ../../bin/pio train
```

Example output:

```
2014-08-05 17:06:02,638 INFO  APIDebugWorkflow$ - Metrics is null. Stop here
2014-08-05 17:06:02,769 INFO  APIDebugWorkflow$ - Run information saved with ID: 201408050005
```

Deploy:

```
$ ../../bin/pio deploy
```

Retrieve prediction:

```
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000
```

Output:

```
{"temperature":75.5}
```

Retrieve prediction:

```
$ curl -H "Content-Type: application/json" -d '{ "day": "Tue" }' http://localhost:8000
```

Output:
```
{"temperature":80.5}
```

## Re-training

Re-train with new data:

```
$ cp $PIO_HOME/data/helloworld/data2.csv data.csv
```

```
$ ../../bin/pio train
$ ../../bin/pio deploy
```

```
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000

{"temperature":76.66666666666667}
```

