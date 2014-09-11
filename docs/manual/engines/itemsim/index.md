---
layout: docs
title:  Item Similarity Engine | Built-in Engines
---

# ITEM SIMILARITY ENGINE: OVERVIEW

**People who like this may also like....**

This engine tries to suggest **N** items that are similar to a targeted item. By being 'similar', it does not necessarily mean that the two items look alike, nor they share similar attributes. The definition of similarity is independently defined by each algorithm and is usually calculated by a distance function. The built-in algorithms assume that similarity between two items means the likelihood any user would like (or buy, view etc) both of them.

The engine suggests similar items through a two-step process:


## Step 1: Estimate Item Similarity

![Item Sim Score Prediction](/images/engine-itemsim-score.png)

In this batch-mode process, the engine estimates a similarity score for every item-item pair. The scores are computed by the deployed algorithm in the engine.

## Step 2: Rank Top N Items

With the similarity scores, this engine can rank all available items according to their similarity to a targeted item. Advanced queries, such as Geo-based search, is supported. Top N most similar items will then be returned.


# Data Input through Data API

All built-in algorithms of this engine require the following data:

* User data
* Item data
* User-to-item behavioral data, such as like, rate and view.

(TODO)

# Prediction Query API

Item Similarity Engine supports the following API endpoints:

## Get Similar Items

To suggest top N items that are most similar to a targeted item, make an HTTP GET request to itemsim engine URI:

```
GET /<TODO>
```

The query is a targeted item while the output is a list of N items.

#### Required Parameters

(TODO)

> *Note*
>
>If multiple item IDs are specified (For example: pio_iid=item0,item1,item2), all the specified items will be taken into account when return the top N similar items. One typical usage is that you could keep track a list of recent viewed items of the user and then use this list of recently viewed items to recommend items to the user. This could also be used to provide recommendation to anonymous users as soon as they have viewed a few items.


#### Optional Parameters

(TODO)

#### Sample Response

(TODO)

# Changing Algorithm and Its Parameters

Item Similarity Engine comes with the following algorithms:

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

