---
layout: docs
title: Evaluating the Prediction Results
---

<!--
# Evaluating the Prediction Results
-->

To chose the best algorithm and parameters, it is important to systematically
evaluate the quality of the engine instance. 

# ItemRank Evaluation

This section discusses how to evaluate the [ItemRank
Engine](../../engines/itemrank). An ItemRank Engine makes a
personalized ranking among the items in the input query.

## What You Want To Evaluate

The goal of evaluation is to find a set of parameters with which a engine
instance is created that maximize certain objective function(s). An objective
function is usually tie with a business objective. For example, 

- an e-commerce
company has an inventory of 1000+ items, but can only show 5 on their landing
page,
and the goal is to increase the click-thru-rate (hopefully the conversion rate)
of the top 5 items; 

- another e-commerce company wants to sell its expiring items,
hence have add a time component to the weight function; 

- yet another company may
only focus on long-term value of a customer, it only recommends what the user
likes the most (but maybe not profitable), etc.

Whilst the machine learning community has defined numerous quality metrics (e.g.
precision, recall, MAP, MSE, etc.) for measuring prediction, there are many
other considerations when we conduct the evaluation. Generating the test set
can be tricky and bias-prone if we don't do it careful enough.
It is important to
make sure that the evaluation methods we choose approximates the actual business
use case. 

## Example Evaluation: Movie Lens

We create a fictitious company MLC using the [MovieLens data
set](../tutorials/engines/itemrec/movielens.html).
It provides movie recommendations to users. For a given time, the user comes to
the MLC's website, it displays all movies in its inventory to the user, rank by
some mechanism (i.e. MLC uses the ItemRank engine for predicting user
preferences).
MLC's goal is to maximize its long term value, i.e. users like coming back to
the company's website for movie recommendation.

The MovieLens data set contains the following information:

1. User data: userid, age, and occupation.

2. Item (movie) data: itemid, movie name, release date, and genre.

3. Rating data: userid, itemid, rating, timestamp.

To evaluate the quality of a prediction engine, we need to create two sets of
data.

1. Training set, the data used to train machine learning models, it generally
   includes [labelled data](http://en.wikipedia.org/wiki/Supervised_learning). In
   this case the rating assigned to movies by users. The ItemRank engine takes
   these labelled data for training.

2. Testing set, the data used to evaluate the quality of prediction engines.
   The testing set also contains labelled data, but it is *not* send to
   prediction engines. Instead, they are used by the evalution metrics component
   to evaluate the quality of the prediction engine. 

### Sliding Window Evaluation

A common way to evaluate prediction is by using the sliding window method
(sometimes it is called rolling window method). Assuming the data set is
annotated by a timestamp, which is always the case if you are using Prediction.IO
Event Server, we can always divide the whole data set into training and testing
set using a cutoff timestamp. It is particularly appealing since 1. it can avoid
look-ahead bias; and 2. it mimmics actual use case.

Suppose MLC retrains the prediction model every night, it uses all data in the
database and creates a new model, the new model is then used to serve queries in
the next day. The next night, MLC retrains again with the additional data, and
the new model is used to serve the traffic the day after next. Hopefully you
will get why people call it *sliding windows*.

In the evaluation, we attempt to recreate the same scenario. We use three
parameters to define the sliding windows:

Field | Description
---- | :------
`firstTrainingUntilTime` | The cutoff time for the first training data. Only
events happened before this time will be used.
`evalDuration` | The duration of the test data. Events happened from
`firstTrainingUntilTime` until `firstTrainingUntilTime + evalDuration` will be
used to construct the *unseen* testing set.
`evalCount` | The number of training / testing sets used in the evaluation.

For example, the following parameters

```scala
EventsSlidingEvalParams(
  firstTrainingUntilTime = new DateTime(1998, 2, 1, 0, 0),
  evalDuration = Duration.standardDays(7),
  evalCount = 3)
```

create three training / testing sets of data (time is midnight):

Set | Training Event Time | Testing Event Time
--- | --- | ---
0 | [..., 1998-02-01) | [1998-02-01, 1998-02-08)
1 | [..., 1998-02-08) | [1998-02-08, 1998-02-15)
2 | [..., 1998-02-15) | [1998-02-15, 1998-02-22)

## Training Data













(coming soon)
