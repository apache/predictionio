---
layout: docs
title: Filtering Temporary Unavailable Items in ItemRec
---

This tutorial teaches how to implement custom filtering logic.
It is based [Movie Recommendation App with ItemRec Engine](../tutorials/engines/itemrec/movielens.html), we demonstrate how to add a custom filtering logic to the ItemRecommendation Engine.

### Task

The ItemRec Engine recommends items to user.
Some items may run out of stock temporarily, we would like to remove them from the recommendation.

# Customizing the ItemRec Engine

Recall [the DASE Architecture](../enginebuilders/), a PredictionIO engine has 4 main components: Data Source, Data Preparator, Algorithm, and Serving Layer.
When a Query comes in, it is passed to the Algorithm components for making Predictions (notice that we use plural as the infrastructure allows multiple algorithms to run concurrently), then the Serving component consolidates these
Predictions into one, and returns it.

The ItemRec Engine's component can be found it its static factory class
`io.prediction.engines.itemrec.ItemRecEngine`. It looks like the following:

```scala
object ItemRecEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[EventsDataSource],
      classOf[ItemRecPreparator],
      Map("ncMahoutItemBased" -> classOf[NCItemBasedAlgorithm]),
      classOf[ItemRecServing]
    )
  }
}
```

To add filtering logic to this engine,
we will implement a new Serving component, which removes temporarily disabled
items from the Prediction made by Algorithms.
For simplicity, we assume the engine only has one algorithm, the serving logic doesn't need to handle consolidation.

## The Serving Interface
PredictionIO allows you to substitute any component in a prediction engine as long as interface is matched. In this case, the Serving component has to use
the Query and Prediction class defined by the original engine. The `serve` method performs the filting logic.

```scala
class TempFilter(val params: TempFilterParams)
    extends LServing[TempFilterParams, Query, Prediction] {
  override def serve(query: Query, predictions: Seq[Prediction])
  : Prediction = {
    // Our filtering logic
  }
}
```

We will store the disabled items in a file, one item_id per line. Every time the `serve` method is invoked, it removes items whose id can be found in the file.

> Notice that this is only for demonstration, reading from disk for every query leads to terrible system performance. User can implement more efficient I/O.

Then, we will implement a new engine factory using this new Serving component.

# Step-by-Step

## Create a new engine project and using existing instance.
You can create new engine project in any directory.

```bash
$ $PIO_HOME/bin/pio new scala-local-movielens-filtering
$ $PIO_HOME/bin/pio instance --directory-name scala-local-movielens-filtering \
    io.prediction.engines.itemrec
$ cd scala-local-movielens-filtering
```

The default engine template doesn't apply in this case, we can safely delete it.
```bash
$ rm src/main/scala/Engine.scala
```

## Implement the New Filtering Component


## Edit build.sbt

add
```scala
"io.prediction"    %% "engines"       % "0.8.1-SNAPSHOT" % "provided",
```

## Edit engines.json

```json
{
  "id": "scala-local-movielens-filtering",
  "version": "0.0.1-SNAPSHOT",
  "name": "scala-local-movielens-filtering",
  "engineFactory": "myorg.TempFilterEngine"
}
```




(coming soon)
