---
layout: docs
title: Overview
---

# PredictionIO - Overview

PredictionIO acts as a server that collects data and serve prediction results through REST APIs/SDKs. 
At its core is a prediction engine which contains the following components: Data, Algorithm, Serving, and Evaluation. 

## App

It usually corresponds to the application you are building. You can run one or more predictive engines for an app. App data is shared among these engines.

## Engine

An engine must belong to a prediction type, such as Item Recommendation or Item Similarity. Each Engine processes data and constructs predictive models independently. Therefore, every engine serves its own set of prediction results. In an app, for example, you may create two engines: one for recommending news to users and another one for suggesting new friends to users. At least one algorithm must be deployed in each engine.


### Data

### Algorithm

A number of built-in algorithms are available for use in each type of engine. An algorithm, and the setting of its parameters, determines how predictive model is constructed. In another word, prediction accuracy or performance can be improved by tuning a suitable algorithm. PredictionIO comes with tools and metrics for algorithm evaluation.

### Serving

### Evaluation


<TODO: Insert Diagram Prediction Concept Simplfied>
