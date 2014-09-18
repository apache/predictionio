---
layout: docs
title: Building the "HelloWorld" Engine
---

# Building the "HelloWorld" Engine

This is a step-by-step guide on building your first predictive engine on PredictionIO. The engine will use historical temperature data to predict the temperature of a certain day in a week.

Completed source code can also be found at $PIO_HOME/examples/scala-local-helloworld and $PIO_HOME/examples/java-local-helloworld

## 1. Create a new Engine

<div class="codetabs">
<div data-lang="Scala">
{% highlight bash %}
$ $PIO_HOME/bin/pio new HelloWorld
$ cd HelloWorld

Now you need to edit 'src/main/scala/Engine.scala'
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight bash %}
$ pio new --java HelloWorld
$ cd HelloWorld

Now you need to edit 'HelloWorld.java'
{% endhighlight %}
</div>
</div>



## 2. Define Data Types

### Define Training Data

<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
class MyTrainingData(
  val temperatures: List[(String, Double)]
) extends Serializable
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
class MyQuery(
  val day: String
) extends Serializable
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
import scala.collection.immutable.HashMap

class MyModel(
  val temperatures: HashMap[String, Double]
) extends Serializable {
  override def toString = temperatures.toString
}

{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

### Define Prediction Result
<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
class MyPrediction(
  val temperature: Double
) extends Serializable
{% endhighlight %}
</div>
<div data-lang="Java">
{% highlight java %}
Java code here...
{% endhighlight %}
</div>
</div>

## 3. Implement the Data Source
<div class="codetabs">
<div data-lang="Scala">
{% highlight scala %}
import scala.io.Source

class MyDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams,
                                MyTrainingData, MyQuery, EmptyActual] {

  override def readTraining(): MyTrainingData = {
    val lines = Source.fromFile("path/to/data.csv").getLines()
      .toList.map { line =>
        val data = line.split(",")
        (data(0), data(1).toDouble)
      }
    new MyTrainingData(lines)
  }

}
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
class MyAlgorithm extends LAlgorithm[EmptyAlgorithmParams, MyTrainingData,
  MyModel, MyQuery, MyPrediction] {

  override
  def train(pd: MyTrainingData): MyModel = {
    // calculate average value of each day
    val average = pd.temperatures
      .groupBy(_._1) // group by day
      .mapValues{ list =>
        val tempList = list.map(_._2) // get the temperature
        tempList.sum / tempList.size
      }

    // trait Map is not serializable, use concrete class HashMap
    new MyModel(HashMap[String, Double]() ++ average)
  }

  override
  def predict(model: MyModel, query: MyQuery): MyPrediction = {
    val temp = model.temperatures(query.day)
    new MyPrediction(temp)
  }
}
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

Prepare training data:

{% highlight bash %}
$ cp $PIO_HOME/examples/data/helloworld/data1.csv path/to/data.csv
{% endhighlight %}

Register engine:

{% highlight bash %}
$ $PIO_HOME/bin/pio register
{% endhighlight %}

Train:

{% highlight bash %}
$ $PIO_HOME/bin/pio train
{% endhighlight %}

Example output:

```
2014-09-18 15:44:57,568 INFO  spark.SparkContext - Job finished: collect at Workflow.scala:677, took 0.138356 s
2014-09-18 15:44:57,757 INFO  workflow.CoreWorkflow$ - Saved engine instance with ID: zdoo7SGAT2GVX8dMJFzT5w
```

Deploy:

{% highlight bash %}
$ $PIO_HOME/bin/pio deploy
{% endhighlight %}

Retrieve prediction:

{% highlight bash %}
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000
{% endhighlight %}

Output:

```
{"temperature":75.5}
```

Retrieve prediction:

{% highlight bash %}
$ curl -H "Content-Type: application/json" -d '{ "day": "Tue" }' http://localhost:8000
{% endhighlight %}

Output:

```
{"temperature":80.5}
```

## Re-training

Re-train with new data:

{% highlight bash %}
$ cp $PIO_HOME/examples/data/helloworld/data2.csv path/to/data.csv
{% endhighlight %}

{% highlight bash %}
$ $PIO_HOME/bin/pio train
$ $PIO_HOME/bin/pio deploy
{% endhighlight %}

Retrieve prediction:

{% highlight bash %}
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000
{% endhighlight %}

Output:

```
{"temperature":76.66666666666667}
```

