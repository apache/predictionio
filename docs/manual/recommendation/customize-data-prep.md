---
layout: docs
title: Customizing Data Preparator
---

# Customizing Data Preparator ( Recommendation )

Data Preparator is where pre-processing actions occurs. For example, one may want to remove some very popular items from training data because she thinks that these items may not help finding individual person's tastes or one may have a black list of item that she want to remove from training data before feed the data to the algorithm.

This section demonstrates how to add a filtering logic to exclude a list of items from the [Recommendation Engine](/quickstart.html) based on the Recommendation Engine Template. It is highly recommended to go through the [Quckstart Guide](/quickstart.html) first.

Complete code example can be found in
`examples/scala-parallel-recommendation-advanced`.

If you simply want to use this customized code, you can skip to the last section.

## The Data Preparator Component
Recall [the DASE Architecture](/dase.html), a PredictionIO engine has 4 main components: Data Source, Data Preparator, Algorithm, and Serving components. When a Query comes in, it is passed to the Algorithm component for making Predictions.

The Data Preparator component can be found in `/src/main/scala/Preparator.scala` in the MyEngine directory. By default, it looks like the following:

```scala
class Preparator
  extends PPreparator[EmptyPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    new PreparedData(ratings = trainingData.ratings)
  }
}

class PreparedData(
  val ratings: RDD[Rating]
) extends Serializable
```

## The Preparator Interface

PredictionIO allows you to substitute any component in a prediction engine as long as interface is matched. In this case, the Preparator component takes the `TrainingData` and return `PreparedData`. The `prepare` method performs the filting logic.

```scala
import scala.io.Source

case class CustomPreparatorParams(
  val filepath: String
) extends Params

class CustomPreparator(pp: CustomPreparatorParams)
  extends PPreparator[CustomPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val noTrainItems = Source.fromFile(pp.filepath).getLines.map(_.toInt).toSet
    // exclude noTrainItems from original trainingData
    val ratings = trainingData.ratings.filter( r =>
      !noTrainItems.contains(r.product)
    )
    new PreparedData(ratings)
  }
}
```

We will store the black list items in a file, one item_id per line. Then the prepare() method will read this list and take out ratings from TrainigData if the rated item match any of this list.

> Notice that this is only for demonstration, you may generate this list based on TrainingData.

Then, we will implement a new engine factory using this new Data Prepartor component.

# Step-by-Step

Below are the step-by-step instruction of implementing a customized logic.

## Implement the new Preparator

Create a new `CustomPreparator.scala` file or modify from the original `Preparator.scala` and add following import:

```
import io.prediction.controller.PPreparator
import io.prediction.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.recommendation.Rating

import scala.io.Source
```

We need to define one parameter: The filepath of the blacklist file.

```scala
case class CustomPreparatorParams(
  val filepath: String
) extends Params
```

The Data Preparator component implementation is trivial. The `prepare()` reads the blacklisted file from disk. Then it removes these items from the `TrainingData`.

```scala
class CustomPreparator(pp: CustomPreparatorParams)
  extends PPreparator[CustomPreparatorParams, TrainingData, PreparedData] {

  def prepare(sc: SparkContext, trainingData: TrainingData): PreparedData = {
    val noTrainItems = Source.fromFile(pp.filepath).getLines.map(_.toInt).toSet
    // exclude noTrainItems from original trainingData
    val ratings = trainingData.ratings.filter( r =>
      !noTrainItems.contains(r.product)
    )
    new PreparedData(ratings)
  }
}
```

## Define a new engine factory

We need to implement a new engine factory to include this `CustomPreparator`. All we need to do is to modify `Engine.scala` to add a new Engine Factory `RecommendationEngineWithCustomPreparator` which uses the `CustomPreparator` instead of the original `Preparator`.

```scala
// use CustomPreparator
object RecommendationEngineWithCustomPreparator extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[CustomPreparator],
      Map("als" -> classOf[ALSAlgorithm]),
      classOf[Serving])
  }
}
```

Lastly, modify `engine.json` to use this new `RecommendationEngineWithCustomPreparator` and define the parameters for the Data Preparator.

```json
{
  ...
  "engineFactory": "org.template.recommendation.RecommendationEngineWithCustomPreparator",
  ...
  "preparator": {
    "filepath": "./data/sample_not_train_data.txt"
  },
  ...
}
```

A sample black list file is provided in ./data/sample_not_train_data.txt

## Deploy the Engine as a Service

Now you can deploy the engine as described in the [Quckstart Guide](/quickstart.html).

Make sure the appId defined in the file `engine.json` match your `App ID`:

```
...
"datasource": {
  "appId": 1
},
...
```

To build *MyEngine* and deploy it as a service:

```
$ pio build
$ pio train
$ pio deploy
```

This will deploy an engine that binds to http://localhost:8000. You can visit that page in your web browser to check its status.

Now, You can try to retrieve predicted results.
To recommend 4 movies to user whose id is 1, you send this JSON { "user": 1, "num": 4 } to the deployed engine and it will return a JSON of the recommended movies.

```
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' http://localhost:8000/queries.json

{"productScores":[{"product":22,"score":4.072304374729956},{"product":62,"score":4.058482414005789},{"product":75,"score":4.046063009943821},{"product":68,"score":3.8153661512945325}]}
```



#### [Next: Customizing Data Serving](customize-serving.html)
