---
title: Adding Algorithm (Classification)
---

The classification template uses the Naive Bayes algorithm by default. You can easily add and use other MLlib classification algorithms. The following will demonstrate how to add the [MLlib Random Forests algorithm](https://spark.apache.org/docs/latest/mllib-ensembles.html) into the engine.

## 1. Create a new file `RandomForestAlgorithm.scala`.

Copy from `NaiveBayesAlgorithm.scala` and create a new file `RandomForestAlgorithm.scala`. You will modify this file and followimg the following instructions to define a new class RandomForestAlgorithm for the new algorithm.

## 2. Define the algorithm class and parameters

Modify the new file 'RandomForestAlgorithm.scala':

Instead of importing:

```scala
import org.apache.spark.mllib.classification.NaiveBayes
import org.apache.spark.mllib.classification.NaiveBayesModel
```

Change them to:

```scala
import org.apache.spark.mllib.tree.RandomForest // CHANGED
import org.apache.spark.mllib.tree.model.RandomForestModel // CHANGED
```

These are the necessary classes in order to use the MLLib's Random Forest algporithm.

Modify the `AlgorithmParams` class for the Random Forest algorithm:

```scala
// CHANGED
case class RandomForestAlgorithmParams(
  val numClasses: Int,
  val numTrees: Int,
  val featureSubsetStrategy: String,
  val impurity: String,
  val maxDepth: Int,
  val maxBins: Int
) extends Params
```

This class defines the parameters of the Random Forest algorithm (which later you can specify the value in engine.json). Please refer to [MLlib  documentation](https://spark.apache.org/docs/latest/mllib-ensembles.html) for the description and usage of these parameters.

Modify the `NaiveBayesAlgorithm` class to `RandomForestAlgorithm`. The difference from the original `NaiveBayesAlgorithm` class is that:

* The new `RandomForestAlgorithmParams` class is used as parameter
* `RandomForestModel` is used in type parameter. This is the model returned by the Random Forest algorithm.
* the `train()` function is modified and it returns the `RandomForestModel` instead of `NaiveBayesModel`.
* the `predict()` function takes the `RandomForestModel` as input.

Note that the MLlib Random Forest algorithm takes the same training data as the Navie Bayes algoithm (ie, RDD[LabeledPoint]) so you don't need to modify the `DataSource`, `TrainigData` and `PreparedData` classes. If the new algoritm to be added requires different types of training data, then you need to modify these classes accordingly to accomodate your new algorithm.

```scala
// extends P2LAlgorithm because the MLlib's RandomForestModel doesn't
// contain RDD.
class RandomForestAlgorithm(val ap: RandomForestAlgorithmParams) // CHANGED
  extends P2LAlgorithm[PreparedData, RandomForestModel, // CHANGED
  Query, PredictedResult] {

  def train(data: PreparedData): RandomForestModel = { // CHANGED
    // CHANGED
    // Empty categoricalFeaturesInfo indicates all features are continuous.
    val categoricalFeaturesInfo = Map[Int, Int]()
    RandomForest.trainClassifier(
      data.labeledPoints,
      ap.numClasses,
      categoricalFeaturesInfo,
      ap.numTrees,
      ap.featureSubsetStrategy,
      ap.impurity,
      ap.maxDepth,
      ap.maxBins)
  }

  def predict(
    model: RandomForestModel, // CHANGED
    query: Query): PredictedResult = {

    val label = model.predict(Vectors.dense(query.features))
    new PredictedResult(label)
  }

}
```

## 3. Update Engine.scala

Modify the EngineFactory to add the new algorithm class `RandomForestAlgorithm` you just defined and give it a name `"randomforest"`. The name will be used in `engne.json` to specify which algorithm to use.

```scala
object ClassificationEngine extends IEngineFactory {
  def apply() = {
    new Engine(
      classOf[DataSource],
      classOf[Preparator],
      Map("naive" -> classOf[NaiveBayesAlgorithm],
        "randomforest" -> classOf[RandomForestAlgorithm]), // ADDED
      classOf[Serving])
  }
}
```

This engine factory returns an engine with two algorithms and they are named as `"naive"` and `"randomforest"` respectively.

## 4. Update engine.json

In order to use the new algorithm, you need to modify `engine.json` to specify the name of the algorithm and the parameters.

Update the engine.json to following:

```json
...
"algorithms": [
  {
    "name": "randomforest",
    "params": {
      "numClasses": 3,
      "numTrees": 5,
      "featureSubsetStrategy": "auto",
      "impurity": "gini",
      "maxDepth": 4,
      "maxBins": 100
    }
  }
]
...
```

Now you are ready to build, train and deploy the engine as described in [quickstart](quickstart.html).

```
$ pio build
$ pio train
$ pio deploy
```
