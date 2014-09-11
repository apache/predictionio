---
layout: docs
title:  Item Recommendation Engine | Built-in Engines
---

# Item Recommendation Engine: Overview

**Recommend N items to a user personally**

With this engine, you can add discovery or recommendation features to your application. The engine makes recommendation in two steps:

## Step 1: Predict User Preferences

![Item Recommendation Score Prediction](/images/engine-itemrec-prediction.png)

In this batch-mode process, the engine predicts a preference score for every user-item pair. The scores are computed by the deployed algorithm in the engine.

## Step 2: Rank the Query Items

With the predicted scores, this engine can rank all available items for any user according to your REST API/SDK queries. Advanced queries, such as Geo-based search, is supported. Top N items will then be returned as prediction results.


# Data Input through Data API

All built-in algorithms of this engine require the following data:

* User data
* Item data
* User-to-item behavioral data, such as like, rate and view.

> **Note: Extra User and Item Data Attributes**
> 
> Your user data may contain additional attributes, such as age and gender. Your item data may also contain other attributes, such as price and title. What kind of data attribute you need to provide to PredictionIO depends on the algorithm you choose to build the model.
>
> Currently, all built-in algorithms in PreditionIO are Collaborative Filtering (CF) algorithms. CF algorithms derive the feature vectors of users and items from previous behaviors, i.e. score, only. Therefore, you simply need to identify each user and item with a unique ID. No extra data attribute is needed.
> 
> It does not mean that CF algorithms are less accurate though. In fact, researches (such as this) show the exact opposite. An algorithm that requires no data attribute can be the winning algorithm.

(TODO)

# Prediction Query API

Item Recommendation Engine supports the following API endpoints:

## Get Top N Recommendation

To recommend top N items to a user, make an HTTP GET request to itemrec engine URI:

```
GET /<TODO>
```

The query is a targeted user while the output is a list of N items.

#### Required Parameters

(TODO)

#### Optional Parameters

(TODO)

#### Sample Response

(TODO)

# Changing Algorithm and Its Parameters

Item Recommendation Engine comes with the following algorithms:

* (TODO)

By default, (TODO) is used. You can switch to another algorithm by:

```
(TODO)
```

and change the algorithm parameters by:

```
(TODO)
```

Please read [Selecting an Algorithm](/cookbook/choosingalgorithms.html) for tips on selecting the right algorithm and setting the parameters properly.

> You may also [implement and add your own algorithm](/cookbook/addalgorithm.html) to the engine easily.
