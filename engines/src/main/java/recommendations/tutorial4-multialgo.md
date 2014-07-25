# Tutorial 4 - Multiple Algorithms Engine

At this point you have already tasted a sense of implementing, deploying, and evaluating a recommendation system with collaborative filtering techniques. However, this technique suffers from a cold-start problem where new items have no user action history. In this tutorial, we introduce a feature-based recommendation technique to remedy this problem by constructing a user-profile for each users. In addition, Prediction.IO infrastructure allows you to combine multiple recommendation systems together in a single engine. For a history-rich items, the engine can use results from the collaborative filtering algorithm, and for history-absent items, the engine returns prediction from the feature-based recommendation algorithm, moreover, we can ensemble multiple predictions too.

This tutorial guides you toward incorporating a feature-based algorithm into the existing CF-recommendation engine introduced in tutorial 1, 2, and 3.

All code can be found in the [engines.java.recommendations.multialgo](engines/src/main/java/recommendations/multialgo/)
directory.

## Overview
In the previous tutorial, we have covered `DataSource` and `Algorithm` as crucial part of an engine. A complete engine workflows looks like the following figure:
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
`Preparator` is the class which preprocess the training data which will be used by multiple algorithms. For example, it can be a NLP processor which generates useful n-grams, or it can be some business logics.

Engine is designed to support multiple algorithms. The need to take the same `PreparedData` as input for model construction, but each algorithm can have its own model class). Algorithm takes a common `Query` as input and return a `Prediction` as output.

Finally, the serving layer `Serving` combines result from multiple algorithms, and possible apply some final business logic before returning.

This tutorial implements a simple `Preparator` for feature generation, a feature based algorithm, and a serving layer which ensembles multiple predictions.

## DataSource
We have to amend the [`DataSource`](multialgo/DataSource.java) to take into account of more information from MovieLens. We use the genre of movies as its feature vector. This part is simliar to earlier tutorial.

```
$ bin/pio-run io.prediction.engines.java.recommendations.multialgo.Runner4a
```

## Preparator
As we have read the raw data from `DataSource`, we can *preprocess* the raw data into a more useable form. In this tutorial, we generate a feature vector for movies based on its genre.

You need to implement two classes: `Preparator` and `PreparedData`. `Preparator` is a class implementing a method `prepare` which transform `TrainingData` into `PreparedData`; `PreparedData` is the output and the object being passed to `Algorithms` for training. `PreparedData` can be anything, very often it is equivalent to `TrainingData`, or subclass of it. Here, [`PreparedData`](multialgo/PreparedData.java) is a subclass of `TrainingData`, it adds a map from items (movies) to feature vectors. The merit of using subclass is that, it makes the original `TrainingData` easily accessible.

The [`Preparator`](multialgo/Preparator.java) class simply examines item info and extract a feature vector from item info.

After implementing these two classes, you can add them to the workflow and try out if things are really working. Add the preparator class to the engine builder, as shown in [Runner4b.java](multialgo/Runner4b.java):
```java
return new JavaEngineBuilder<
    TrainingData, EmptyParams, PreparedData, Query, Float, Object> ()
    .dataSourceClass(DataSource.class)
    .preparatorClass(Preparator.class)  // Add the new preparator
    .build();
```

And you can test it out with

```
$ bin/pio-run io.prediction.engines.java.recommendations.multialgo.Runner4b
```
