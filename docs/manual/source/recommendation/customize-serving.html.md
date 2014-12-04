---
title: Customizing Serving Component (Recommendation)
---

Serving component is where post-processing occurs. For example, if you are
recommending products to users, you may want to remove items that are not
currently in stock from the list of recommendation.

This section is based on the [Recommendation Engine Template](quickstart.html).

A full end-to-end example can be found on
[GitHub](https://github.com/PredictionIO/PredictionIO/tree/master/examples/scala-parallel-recommendation-custom-serving).

<!--
This section demonstrates how to add a custom filtering logic to exclude a list
of blacklisted movies from the [Movie Recommendation Engine](/quickstart.html)
based on the Recommendation Engine Template. It is highly recommended to go
through the Quckstart guide first.

Complete code example can be found in
`examples/scala-parallel-recommendation-howto`.

If you simply want to use this customized code, you can skip to the last section.
-->

## The Serving Component

Recall [the DASE Architecture](../start/engines.html), a PredictionIO engine has
4 main components: Data Source, Data Preparator, Algorithm, and Serving
components. When a Query comes in, it is passed to the Algorithm component for
making Predictions.

The Engine's serving component can be found in `src/main/scala/Serving.scala` in
the *MyRecommendation* directory. By default, it looks like the following:

```scala
class Serving extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query,
    predictedResults: Seq[PredictedResult]): PredictedResult = {
    predictedResults.head
  }
}
```

We will customize the Serving component to remove temporarily disabled items
from the Prediction made by Algorithms.

## Modify the Serving Interface

We will use a file to specify a list of disabled items. When the `serve` method
is called, it loads the file and removes items in the disabled list from
`PredictedResult`. The following code snippet illustrates the logic:

```scala
import scala.io.Source  // ADDED

class Serving extends LServing[Query, PredictedResult] {

  override def serve(query: Query, predictedResults: Seq[PredictedResult])
  : PredictedResult = {
    // Read the disabled product from file.
    val disabledProducts: Set[Int] = Source
      .fromFile("./data/sample_disabled_products.txt")
      .getLines
      .map(_.toInt)
      .toSet

    val productScores = predictedResults.head.productScores
    // Remove products from the original predictedResult
    PredictedResult(productScores.filter(ps => !disabledProducts(ps.product)))
  }
}
```
INFO:We will show you how not to hardcode the path
`./data/sample_disabled_products.txt` soon.

WARNING: This example code uses a local relative path. For remote deployment, it is
recommended to use a globally accessible absolute path.

DANGER: This example is only for demonstration purpose. Reading from disk for every
query leads to terrible system performance. Use a more efficient
implementation for production deployment.

## Deploy the Modified Engine

Now you can deploy the modified engine as described in the [Quick
Start](/recommendation/quickstart.html) guide.

Make sure the `appId` defined in the file `engine.json` match your *App ID*:

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

This will deploy an engine that binds to http://localhost:8000. You can visit
that page in your web browser to check its status.

Now, you can try to retrieve predicted results. To recommend 4 movies to user
whose ID is 1, send this JSON `{ "user": 1, "num": 4 }` to the deployed
engine

```
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' \
  http://localhost:8000/queries.json
```

and it will return a JSON of recommended movies.

```json
{
  "productScores": [
    {"product": 65, "score": 6.537168137254073},
    {"product": 69, "score": 6.391430405762495},
    {"product": 38, "score": 5.829957095096519},
    {"product": 11, "score": 5.5991291456974}
  ]
}
```

Now, to verify the blacklisting logic, we add the product 69 (the second item)
to the blacklisting file `data/sample_disabled_products.txt`. Rerun the `curl`
query, and the change should take effect immediately as the disabled product
list is reloaded every time the `serve` method is called.

```
$ echo "69" >> ./data/sample_disabled_products.txt
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' \
  http://localhost:8000/queries.json
```

```json
{
  "productScores": [
    {"product": 65, "score": 6.537168137254073},
    {"product": 38, "score": 5.829957095096519},
    {"product": 11, "score": 5.5991291456974}
  ]
}
```

Congratulations! You have learned how to add customized realtime blacklisting
logic to your Serving component!

## Adding Serving Parameters

Optionally, you may want to take the hardcoded path
(`./data/sample_disabled_products.txt`) away from the source code.

PredictionIO offers `ServingParams` so you can read variable values from
`engine.json` instead. Prediction.IO transforms the JSON object specified in
`engine.json`'s `serving` field into the `ServingParams` class.

Modify `src/main/scala/Serving.scala` again in the *MyRecommendation*
directory to:

```scala
import scala.io.Source

import io.prediction.controller.Params  // ADDED

// ADDED ServingParams to specify the blacklisting file location.
case class ServingParams(val filepath: String) extends Params

class Serving(val params: ServingParams)
  extends LServing[Query, PredictedResult] {

  override
  def serve(query: Query, predictedResults: Seq[PredictedResult])
  : PredictedResult = {
    val disabledProducts: Set[Int] = Source
      .fromFile(params.filepath)
      .getLines
      .map(_.toInt)
      .toSet

    val productScores = predictedResults.head.productScores
    PredictedResult(productScores.filter(ps => !disabledProducts(ps.product)))
  }
}
```

In `engine.json`, you specify the parameter `serving` for the Serving component
(the JSON4S library automatically extract the JSON object into a Scala class
under the hood):

```json
{
  ...
  "serving": {
    "filepath": "./data/sample_disabled_products.txt"
  },
  ...
}
```

Again, to build *MyRecommendation* and deploy it as a service:

```
$ pio build
$ pio train
$ pio deploy
```

You can change the `filepath` value without re-building the code next time.
