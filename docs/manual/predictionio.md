---
layout: docs
title: Overview
---

# Overview

PredictionIO acts as a server that collects data and serve prediction results through REST APIs/SDKs. 
At its core is a prediction engine which contains the following components: Data, Algorithm, Serving, and Evaluation. 


![System Overview](/images/system-overview.png)


## App

It usually corresponds to the application you are building. You can run one or more predictive engines for an app. App data is shared among these engines.

## Engine

An engine must belong to a prediction type, such as Item Recommendation or Item Similarity. Each Engine processes data and constructs predictive models independently. Therefore, every engine serves its own set of prediction results. In an app, for example, you may create two engines: one for recommending news to users and another one for suggesting new friends to users. At least one algorithm must be deployed in each engine.


## Data

The data layer consists of Data Source and Data Preparator. It preprocesses the data and forward it to the algrithm for trianing. 

## Algorithm

A number of built-in algorithms are available for use in each type of engine. An algorithm, and the setting of its parameters, determines how predictive model is constructed. In another word, prediction accuracy or performance can be improved by tuning a suitable algorithm. PredictionIO comes with tools and metrics for algorithm evaluation.

## Serving

The serviing layer returns result of the predictive model. If the engine has multiple algorithms, the serving layer will combine the results into one. Additionally, you can add your own business logic to the serving layer and further customzied the final returned results. 

## Evaluation

The prediction output is also sent to the Evaluation where you can compare the results of different algorithms. It currently supports offline evaluation only.  



