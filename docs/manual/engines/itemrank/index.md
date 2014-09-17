---
layout: docs
title:  Item Ranking Engine | Built-in Engines
---

# Item Ranking Engine: Overview

**Rank a list of items to a user personally**

With this engine, you can personalize a ranked list of items in your application. The engine rank items in two steps:

## Step 1: Predict User Preferences

![Item Ranking Score Prediction](/images/engine-itemrec-prediction.png)

In this batch-mode process, the engine predicts a preference score for the queried items. The scores are computed by the deployed algorithm in the engine.

## Step 2: Rank the Query Items

With the predicted scores, this engine can rank a list of items for the user according to queries. Ranked items with scores will then be returned. Original order of the items is preserved if the algorithm couldn't predict the score.

# Events Requirement

This built-in engine requires the following Events data:

Your Events data should involve two EntityTypes:
- pio_user entity: the user of your application
- pio_item entity: the item of your application
  with following properties:
  - pio_itypes: array of String
  - pio_starttime: ISO 8601 timestamp
  - pio_endtime: ISO 8601 timestamp
  - pio_inactive: boolean

and events between these two Entity Types:
- user-to-item action events: such as like, rate and view.
  optionally with following properties:
  - pio_rating: integer rating



> **Note: Extra User and Item Entity Properties**
>
> Your user data may contain additional properties, such as age and gender. Your item data may also contain other properties, such as price and title. What kind of data attribute you need to provide to PredictionIO depends on the algorithm you choose to build the model.
>
> Currently, all built-in algorithms in PredictionIO are Collaborative Filtering (CF) algorithms. CF algorithms derive the feature vectors of users and items from previous behaviors, i.e. score, only. Therefore, you simply need to identify each user and item with a unique ID. No extra data attribute is needed.
>
> It does not mean that CF algorithms are less accurate though. In fact, researches (such as this) show the exact opposite. An algorithm that requires no data attribute can be the winning algorithm.

(TODO)

# Item Ranking Engine API

Item Ranking Engine supports the following query API endpoints:

## Get Ranked Item

To rank a list items for a user, make an HTTP GET request to itemrank engine URI:

```
GET /<TODO>
```

The query is a targeted user and a list of item ids while the output is a list of ranked item ids.

#### Required Parameters

(TODO)

#### Optional Parameters

(TODO)

#### Sample Response

(TODO)

# Changing Algorithm and Its Parameters

Item Ranking Engine comes with the following algorithms:

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
