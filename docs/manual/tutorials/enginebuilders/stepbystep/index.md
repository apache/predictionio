---
layout: docs
title: Step-by-Step Engine Building
---

# Step-by-Step Engine Building

## Overview

These series of tutorials will walk through each components of **PredictionIO**.
We will demonstrate how to develop your machine learning algorithms and
prediction engines, deploy them and serve real time prediction queries, develop
your metrics to run offline evaluations, and improve prediction engine by using
multiple algoritms. We will build a simple **Java single machine recommendation
engine** which predicts item's rating value rated by the user. [MovieLens
100k](http://grouplens.org/datasets/movielens/) data set will be used as an
example.

Execute the following command to download MovieLens 100k to data/ml-100k/.

```
$ cd $PIO_HOME/examples
$ src/main/java/recommendations/fetch.sh
```
where `$PIO_HOME` is the root directory of the PredictionIO code tree.

## Concept

A prediction **Engine** consists of the following controller components:
**DataSource**, **Preparator**, **Algorithm**, and **Serving**. Another component
**Metrics** is used to evaluate the *Engine*.

- *DataSource* is responsible for reading data from the source (Eg. database or
  text file, etc) and prepare the **Training Data (TD)**.
- *Preparator* takes the *Training Data* and generates **Prepared Data (PD)**
  for the *Algorithm*
- *Algorithm* takes the *Prepared Data* to train a **Model (M)** which is used
  make **Prediction (P)** outputs based on input **Query (Q)**.
- *Serving* serves the input *Query* with *Algorithm*'s *Prediction* outputs.

An **Engine Factory** is a factory which returns an *Engine* with the above
components defined.

To evaluate a prediction *Engine*:
- *DataSource* can also generate *Test Data* which is a list of input *Query*
  and **Actual (A)** result.
- *Metrics* computes the **Metric Result (MR)** by comparing the *Prediction*
  output with the *Actual* result. PredictionIO feeds the input *Query* to the
  *Engine* to get the *Prediction* outputs which are compared with *Actual*
  results.

As you can see, the controller components (*DataSource, Preparator, Algorithm,
Serving and Metrics*) are the building blocks of the data pipeline and the data
types (*Training Data*, *Prepared Data*, *Model*, *Query*, *Prediction* and
*Actual*) defines the types of data being passed between each component.

Note that if the *Algorithm* can directly use *Training Data* without any
pre-processing, the *Prepared Data* can be the same as *Training Data* and a
default **Identity Preparator** can be used, which simply passes the *Training
Data* as *Prepared Data*.

Also, if there is only one *Algorithm* in the *Engine*, and there is no special
post-processing on the *Prediction* outputs, a default **First Serving** can be
use, which simply uses the *Algorithm*'s *Prediction* output to serve the query.

In this first tutorial, we will demonstrate how to build an simple Item
Recommendation Engine with the *DataSource* and *Algorithm* components. You can
find all sources code of this tutorial in the directory
`src/main/java/recommendations/tutorial1/`.

## Getting Started

Let's begin with [implementing a new Engine with Data and Algorithm](dataalgorithm.html).
