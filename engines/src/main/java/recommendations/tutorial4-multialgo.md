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


## DataSource
