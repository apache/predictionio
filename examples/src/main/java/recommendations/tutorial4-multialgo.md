# Tutorial 4 - Multiple Algorithms Engine

At this point you have already tasted a sense of implementing, deploying, and
evaluating a recommendation system with collaborative filtering techniques.
However, this technique suffers from a cold-start problem where new items have
no user action history. In this tutorial, we introduce a feature-based
recommendation technique to remedy this problem by constructing a user-profile
for each users. In addition, Prediction.IO infrastructure allows you to combine
  multiple recommendation systems together in a single engine. For a
  history-rich items, the engine can use results from the collaborative
  filtering algorithm, and for history-absent items, the engine returns
  prediction from the feature-based recommendation algorithm, moreover, we can
  ensemble multiple predictions too.

This tutorial guides you toward incorporating a feature-based algorithm into
the existing CF-recommendation engine introduced in tutorial 1, 2, and 3.

All code can be found in the
[engines.java.recommendations.tutorial4](tutorial4/)
directory.

## Overview
In the previous tutorial, we have covered `DataSource` and `Algorithm` as
crucial part of an engine. A complete engine workflows looks like the following
figure:
```
            DataSource.read
             (TrainingData)
                   v
           Preparator.prepare
             (PreparedData)
                   v
     +-------------+-------------+
     v             v             v
Algo1.train   Algo2.train   Algo3.train
  (Model1)      (Model2)      (Model3)
     v             v             v
Algo1.predict Algo2.predict Algo3.predict <- (Query)
(Prediction1) (Prediction2) (Prediction3)
     v             v             v
     +-------------+-------------+
                   v
              Serving.serve
              (Prediction)
```
`Preparator` is the class which preprocess the training data which will be used
by multiple algorithms. For example, it can be a NLP processor which generates
useful n-grams, or it can be some business logics.

Engine is designed to support multiple algorithms. The need to take the same
`PreparedData` as input for model construction, but each algorithm can have its
own model class). Algorithm takes a common `Query` as input and return a
`Prediction` as output.

Finally, the serving layer `Serving` combines result from multiple algorithms,
and possible apply some final business logic before returning.

This tutorial implements a simple `Preparator` for feature generation, a
feature based algorithm, and a serving layer which ensembles multiple
predictions.

## DataSource
We have to amend the [`DataSource`](tutorial4/DataSource.java) to
take into account of more information from MovieLens, as well as adding some
fake data for demonstration. We use the genre of movies as its feature vector.
This part is simliar to earlier tutorial.

```
$ cd $PIO_HOME/examples
$ ../bin/pio-run io.prediction.examples.java.recommendations.tutorial4.Runner4a
```
where `$PIO_HOME` is the root directory of the PredictionIO code tree.


## Preparator
As we have read the raw data from `DataSource`, we can *preprocess* the raw
data into a more useable form. In this tutorial, we generate a feature vector
for movies based on its genre.

You need to implement two classes: `Preparator` and `PreparedData`.
`Preparator` is a class implementing a method `prepare` which transform
`TrainingData` into `PreparedData`; `PreparedData` is the output and the object
being passed to `Algorithms` for training. `PreparedData` can be anything, very
often it is equivalent to `TrainingData`, or subclass of it. Here,
[`PreparedData`](tutorial4/PreparedData.java) is a subclass of `TrainingData`,
it adds a map from items (movies) to feature vectors. The merit of using
subclass is that, it makes the original `TrainingData` easily accessible.

The [`Preparator`](tutorial4/Preparator.java) class simply examines item info
and extract a feature vector from item info.

After implementing these two classes, you can add them to the workflow and try
out if things are really working. Add the preparator class to the engine
builder, as shown in [Runner4b.java](tutorial4/Runner4b.java):
```java
return new JavaEngineBuilder<
    TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
    .dataSourceClass(DataSource.class)
    .preparatorClass(Preparator.class)  // Add the new preparator
    .build();
```

And you can test it out with

```
$ cd $PIO_HOME/examples
$ ../bin/pio-run io.prediction.examples.java.recommendations.tutorial4.Runner4b
```


## Feature-Based Algorithm
This algorithm creates a feature profile for every user using the feature
vector in `PreparedData`. More specifically, if a user has rated 5 stars on
*Toy Story* but 1 star on *The Crucible*, the user profile would reflect that
this user likes comedy and animation but dislikes drama.

The movie lens rating is an integer ranged from 1 to 5, we incorporate it into
the algorithm with the following parameters:
```java
public class FeatureBasedAlgorithmParams implements JavaParams {
  public final double min;
  public final double max;
  public final double drift;
  public final double scale;
  ...
}
```
We only consider rating from `min` to `max`, and we normalize the rating with
this function: `f(rating) = (rating - drift) * scale`. As each movie is
associated with a binary feature vector, the user feature vector is essentially
a rating-weighted sum of all movies (s)he rated.  After that, we normalize all
user feature vector by L-inf norm, this will ensure that user feature is
bounded by [-1, 1]. In laymen term, -1 indicates that the user hates that
feature, whilst 1 suggests the opposite.  The following code snippet illustrate
the actual code. `data` is an instance of `PreparedData` that is passed as
argument to the `train` function.

```java
for (Integer uid : data.userInfo.keySet()) {
  userFeatures.put(uid, new ArrayRealVector(data.featureCount));
}

for (TrainingData.Rating rating : data.ratings) {
  final int uid = rating.uid;
  final int iid = rating.iid;
  final double rate = rating.rating;

  // Skip features outside the range.
  if (!(params.min <= rate && rate <= params.max))  continue;

  final double actualRate = (rate - params.drift) * params.scale;
  final RealVector userFeature = userFeatures.get(uid);
  final RealVector itemFeature = data.itemFeatures.get(iid);
  userFeature.combineToSelf(1, actualRate, itemFeature);
}

// Normalize userFeatures by l-inf-norm
for (Integer uid : userFeatures.keySet()) {
  final RealVector feature = userFeatures.get(uid);
  feature.mapDivideToSelf(feature.getLInfNorm());
}
```

[Runner4c.scala](tutorial4/Runner4c.java) illustrates the engine factory up to
this point. We use a default serving class as we only have one algorithm. (We
will demonstrate how to combine prediction results from multiple algorithm is
in the section). We are able to define [an end-to-end
engine](tutorial4/SingleEngineFactory.java).
```
$ cd $PIO_HOME/examples
$ ../bin/pio-run io.prediction.examples.java.recommendations.tutorial4.Runner4c
```

## Deployment
Likewise in tutorial 1, we can deploy this feature based engine. We have a
[engine manifest](tutorial4/single-manifest.json), and we register it:
```
$ cd $PIO_HOME/examples
$ ../bin/register-engine src/main/java/recommendations/tutorial4/single-manifest.json
```
The script automatically recompiles updated code. You will need to re-run this
script if you have update any code in your engine.

### Specify Engine Parameters
We need to use JSON files for deployment.

1.  [dataSourceParams.json](tutorial4/single-jsons/dataSourceParams.json):
    ```json
    {
      "dir" :  "data/ml-100k/",
      "addFakeData": true
    }
    ```

2.  [algorithmsParams.json](tutorial4/single-jsons/algorithmsParams.json):
    ```json
    [
      {
        "name": "featurebased",
        "params": {
          "min": 1.0,
          "max": 5.0,
          "drift": 3.0,
          "scale": 0.5
        }
      }
    ]
    ```
    Recall that we support multiple algorithms. This JSON file is actually a
    list of name-params pair where the name is the identifier of algorithm
    defined in EngineFactory, and the params value correspond to the algorithm
    parameter.


### Start training
The following command kick-starts the training, which will return an id when
the training is completed.
```
$ ../bin/run-train \
  --engineId io.prediction.examples.java.recommendations.tutorial4.SingleEngineFactory \
  --engineVersion 0.8.0-SNAPSHOT \
  --jsonBasePath src/main/java/recommendations/tutorial4/single-jsons/
```
You should be able to find the run id from console, something like this:
```
2014-08-05 15:36:50,521 INFO  APIDebugWorkflow$ - Run information saved with ID: xTFSs5seQBSKoAaq5k8G-A
```

### Start server
As the training is completed, you can start a server
```
$ ../bin/run-server --engineInstanceId xTFSs5seQBSKoAaq5k8G-A
```

### Try a few things
Fake user -1 (see [DataSource.FakeData](tutorial4/DataSource.java)) loves
action movies. If we pass item 27 (Bad Boys), we should get a high rating (i.e.
1). You can use our script bin/cjson to send the JSON request. The first
parameter is the JSON request, and the second parameter is the server address.
```
$ cd $PIO_HOME/examples
$ ../bin/cjson '{"uid": -1, "iid": 27}' http://localhost:8000
```
Fake item -2 is a cold item (i.e. has no rating). But from its data, we know
that it is a movie catagorized under "Action" genre, hence, it should also have
a high rating with Fake user -1.
```
$ cd $PIO_HOME/examples
$ ../bin/cjson '{"uid": -1, "iid": -2}' http://localhost:8000
```
However, there is nothing we can do with a cold user. Fake user -3 has no
rating history, we know nothing about him. If we request any rating with fake
user -3, we will get a NaN.
```
$ cd $PIO_HOME/examples
$ ../bin/cjson '{"uid": -3, "iid": 1}' http://localhost:8000
```

## Multiple Algorithms
We have two algorithms available, one is a collaborative filtering algorithm
and the other is a feature-based algorithm. Prediction.IO allows you to create
a engine that ensembles multiple algorithms prediction, you may use
feature-based algorithm for cold-start items (as CF algos cannot handle items
with no ratings), and use both algorithms for others.

### Combining Algorithms Output
[Serving Layer](tutorial4/Serving.java) is the last step of the pipeline. It
takes prediction results from all algorithms, combine it and return. In the
current case, we take an average of all valid (i.e. not NaN) predictions. In the
extreme case where all algorithms return NaN, we also return NaN. Engine
builders need to implement the `serve` method. We demonstrate with our case:
```java
public Float serve(Query query, Iterable<Float> predictions) {
  float sum = 0.0f;
  int count = 0;

  for (Float v: predictions) {
    if (!v.isNaN()) {
      sum += v;
      count += 1;
    }
  }
  return (count == 0) ? Float.NaN : sum / count;
}
```

### Complete Engine Factory
[EngineFactory.java](tutorial4/EngineFactory.java) demonstrates how to specify
multiple algorithms in the same engine. When we add algorithms to the builder
instance, we also need to specify a String which is served as the identifier.
For example, we use "featurebased" for the feature-based algorithm, and
"collaborative" for the collaborative-filtering algorithm.

```java
public class EngineFactory implements IJavaEngineFactory {
  public JavaEngine<TrainingData, EmptyParams, PreparedData, Query, Float, Object> apply() {
    return new JavaEngineBuilder<
      TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
      .dataSourceClass(DataSource.class)
      .preparatorClass(Preparator.class)
      .addAlgorithmClass("featurebased", FeatureBasedAlgorithm.class)
      .addAlgorithmClass("collaborative", CollaborativeFilteringAlgorithm.class)
      .servingClass(Serving.class)
      .build();
  }
}
```

Similar to the earlier example, we need to write a manifest of the engine, and
register it with PredictionIO. Manifest:
```json
{
  "id": "io.prediction.examples.java.recommendations.tutorial4.EngineFactory",
  "version": "0.8.0-SNAPSHOT",
  "name": "FeatureBased Recommendations Engine",
  "engineFactory": "io.prediction.examples.java.recommendations.tutorial4.EngineFactory"
}
```
The following script register the engines. Important to note that, the script
also copies all related files (jars, resources) of this engine to a permanent
storage, if you have updated the engine code or add new dependencies, you need
to rerun this command.
```
$ cd $PIO_HOME/examples
$ ../bin/register-engine src/main/java/recommendations/tutorial4/manifest.json
```

Now, we can specify the engine instance by passing the set of parameters to the
engine. Our engine can support multiple algorithms, and in addition, it also
support multiple instance of the same algorithms. We illustrates with
[algorithmsParams.json](tutorial4/jsons/algorithmsParams.json):
```json
[
  {
    "name": "featurebased",
    "params": {
      "min": 1.0,
      "max": 5.0,
      "drift": 3.0,
      "scale": 0.5
    }
  },
  {
    "name": "featurebased",
    "params": {
      "min": 4.0,
      "max": 5.0,
      "drift": 3.0,
      "scale": 0.5
    }
  },
  {
    "name": "collaborative",
    "params": {
      "threshold": 0.2
    }
  }
]
```
This JSON contains three algorithm parameters. The first two correspond to the
feature-based algorithm, and the third corresponds to the collaborative
filtering algorithm. The first allows all 5 ratings, and the second allows only
ratings higher than or equals to 4. This gives a bit more weight on the
high-rating features. Once [all parameter files are
specified](tutorial4/jsons/), we can start the training phase and start the API
server:

```
$ cd $PIO_HOME/examples
$ ../bin/run-train \
  --engineId io.prediction.examples.java.recommendations.tutorial4.EngineFactory \
  --engineVersion 0.8.0-SNAPSHOT  \
  --jsonBasePath src/main/java/recommendations/tutorial4/jsons/

...
2014-08-05 15:41:58,479 INFO  SparkContext - Job finished: collect at DebugWorkflow.scala:569, took 4.168001 s
2014-08-05 15:41:58,479 INFO  APIDebugWorkflow$ - Metrics is null. Stop here
2014-08-05 15:41:59,447 INFO  APIDebugWorkflow$ - Run information saved with ID: 41205x9wSo20Fsxm4Ic8BQ

$ ../bin/run-server --engineInstanceId 41205x9wSo20Fsxm4Ic8BQ
```

By default, the server starts on port 8000. Open it with your browser and you
will see all the meta information about this engine instance.

You can submit various queries to the server and see what you get.
