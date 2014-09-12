---
layout: docs
title: System Concepts and System Design
---

# PredictionIO - Concepts

(TODO: Diagram)

PredictionIO acts as a server that collects data and serve prediction results through REST APIs/SDKs. Conceptually, the 3 main building blocks are: App, Engine and Algorithm.

## App

App in PredictionIO Server is like a database or collection in a database server. It usually corresponds to the application you are building. You can run one or more predictive engines towards an app. App data is shared among these engines.

## Engine

An engine must belong to a prediction type (or engine type), such as Item Recommendation or Item Similarity. Each Engines process data and construct predictive model independently. Therefore, every engine serves its own set of prediction results. In an app, for example, you may create two engines: one for recommending news to users and another one for suggesting new friends to users. An algorithm must be deployed in each engine.

## Algorithm

A number of built-in algorithms are available for use in each type of engine. An algorithm, and the setting of its parameters, determines how predictive model is constructed. In another word, prediction accuracy or performance can be improved by tuning a suitable algorithm. PredictionIO comes with tools and metrics for algorithm evaluation.


# PredictionIO - System Design

(TODO: Diagram)

PredictionIO is mainly built with Scala. Scala runs on the JVM, so Java and Scala stacks can be freely mixed for totally seamless integration. PredictionIO Server consists of a few components:

* (TODO)
