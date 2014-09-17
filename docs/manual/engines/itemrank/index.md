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

# Events Data Requirement

This built-in engine requires the following Events data:

Your Events data should involve two EntityTypes:

1. `pio_user` Entity: the user of your application
2. `pio_item` Entity: the item of your application with the following properties:
  - `pio_itypes`: array of String.
  - `pio_starttime`: (Optional) ISO 8601 timestamp
  - `pio_endtime`: (Optional) ISO 8601 timestamp
  - `pio_inactive`: (Optional) boolean

Events between these two Entity Types should be recorded:

- user-to-item action Events: such as like, rate and view, with following properties:
  - `pio_rating`: (Optional) integer rating


> **Note: Name of EntityType and Properties**
>
> Although you are allowed to use different names for the Entity Type and Properties as long as they represent same meaning (For example, use the name `user` instead of `pio_user` or use `t` instead of `pio_starttime`). We highly recommend to follow our name convention when using built-in engines. If you use diffrent names for these attributes, you need to modify the `attributeNames` field defined in the file `datasource.json`.

> **Note: Extra User and Item Entity Properties**
>
> Your user data may contain additional properties, such as age and gender. Your item data may also contain other properties, such as price and title. What kind of properties you need to provide depends on the algorithm you choose to build the model.
>
> Currently, all built-in algorithms in PredictionIO are Collaborative Filtering (CF) algorithms. CF algorithms derive the feature vectors of users and items from previous behaviors, i.e. score, only. Therefore, you simply need to identify each user and item with a unique ID. No extra data properties are needed.
>
> It does not mean that CF algorithms are less accurate though. In fact, researches (such as this) show the exact opposite. An algorithm that requires no data attribute can be the winning algorithm.


# Item Ranking Engine API

Item Ranking Engine supports the following query API endpoints:

## Sending Queries to Item Ranking Engine

To rank a list of items for a user, make an HTTP POST request to the Item Ranking Engine instance:

**POST** *Engine_Instance_IP*:*Engine_Instance_port*


with following JSON payload:

Field | Description
------ | :---------
`uid` | user Entity ID
`iids`| array of item Entity ID

#### Sample Query

```
$ curl -i -X POST http://localhost:8000 \
-d '{
  "uid" : 123,
  "iids" : [1, 3, 5, 10, 11]
}'

```

The API returns the following JSON response:

Field | Description
:---- | :----------
`items` | array of { item Entity ID : predicted preference score }
        | in ranked order
`isOriginal`| if set to true, the items are not ranked
            | (because the algorithm cannot predict)





#### Sample Response

```json
{"items":[{"10":35.15679457387672},{"11":14.929159003452385},{"1":6.950646135607128},{"3":6.894567600916194},{"5":3.9688094914951138}],"isOriginal":false}
```


# Algorithms

Item Ranking Engine comes with the following algorithms:

## Mahout Item Based Algorithm

Use Mahout Item Based algorithm to build similarity matrix. Then rank items based on user recent history and the item similarity matrix.

**Algorithm code name:** `"mahoutItemBased"`

**Parameters:**

Field | Type | Description
:---- | :----| :------
`booleanData` | boolean | Treat input data as having no preference values.
`itemSimilarity`| String | Item Similarity Measure. See **Note**
`weighted` | boolean | The Similarity score is weighted (only applied to Euclidean Distance, Pearson Correlation, Uncentered Cosine item similarity).
`nearestN` | integer | K-nearest rated item neighbors,
`threshold` | double | Similarity threshold
`numSimilarItems` | integer | Number of similar items in the Item Similarity Matrix model.
`numUserActions`| integer | Number of user-to-item actions in the user history model.
`freshness` | integer | Freshness scale 0 - 10. Must be >= 0. 0 means no freshness.
`freshnessTimeUnit` | integer | The time unit in seconds for freshness prioritization. As an example, if you set this to one day (86400), and freshness is set to 10, items that are one day old would have their score degraded by a bit more than 60%, or e^-1 remains to be exact.

**Note:**

Supported value for `itemSimilarity`

Name | Value
:---- | :----
City Block | `CityBlockSimilarity`
Euclidean Distance | `EuclideanDistanceSimilarity`
Log-Likelihood | `LogLikelihoodSimilarity`
Pearson Correlation | `PearsonCorrelationSimilarity`
Tanimoto Coefficient | `TanimotoCoefficientSimilarity`
Uncentered Cosine | `UncenteredCosineSimilarity`

**Default algorithm parameters:**

```json
[
  {
    "name": "mahoutItemBased",
    "params": {
      "booleanData": true,
      "itemSimilarity": "LogLikelihoodSimilarity",
      "weighted": false,
      "nearestN": 10,
      "threshold": 5e-324,
      "numSimilarItems": 50,
      "numUserActions": 50,
      "freshness" : 0,
      "freshnessTimeUnit" : 86400
    }
  }
]
```

## Feature Based Algorithm

Rank items based on item's feature vector (`pio_itypes`).

**Algorithm code name:** `"featurebased"`

**Parameters:**

This algorithm doesn't have parameters.

**Default algorithm parameters:**

```json
[
  {
    "name": "featurebased",
    "params": {}
  }
]

```

## Random Algorithm

Rank items randomly (mainly for baseline evaluation purpose).

**Algorithm code name:** `"rand"`

**Parameters:**

This algorithm doesn't have parameters.

**Default algorithm parameters:**

```json
[
  {
    "name": "rand",
    "params": {}
  }
]

```

## Non-cached Mahout Item Based Algorithm

Use Mahout Item Based algorithm to re-calculate predicted score every time when serve the query request. The item similarity matrix is not cached. (Serving performance is slower)

**Algorithm code name:** `"ncMahoutItemBased"`

**Parameters:**

Same as **Mahout Item Based Algorithm** *without* the following parameters:

- `numSimilarItems`
- `numUserActions`


**Default algorithm parameters:**

```json
[
  {
    "name": "ncMahoutItemBased",
    "params": {
      "booleanData": true,
      "itemSimilarity": "LogLikelihoodSimilarity",
      "weighted": false,
      "threshold": 5e-324,
      "nearestN": 10,
      "unseenOnly" : false,
      "freshness" : 0,
      "freshnessTimeUnit" : 86400
    }
  }
]
```

# Changing Algorithm and Its Parameters

By default, **Mahout Item Based Algorithm** (`"mahoutItemBased"`) is used. You can switch to another algorithm or modify parameters by modifying the file  `algorithms.json` with any of above algorithm's JSON parameters setting.

Please read [Selecting an Algorithm](/cookbook/choosingalgorithms.html) for tips on selecting the right algorithm and setting the parameters properly.

> You may also [implement and add your own algorithm](/cookbook/addalgorithm.html) to the engine easily.
