---
title: PredictionIO - A Quick Intro
---

## Overview

PredictionIO consist of the following components:

* **PredictionIO platform** - our open source machine learning stack for building, evaluating and deploying engines with machine learning algorithms.
* **Event Server** - our open source machine learning analytics layer for unifying events from multiple platforms
* **Template Gallery** - the place for you to download engine templates for different type of machine learning applications

![PredictionIO Overview](/images/overview-multiengines.png)

## Event Server

In a common scenario, PredictionIO's **Event Server** continuously collects data from your application.
A PredictionIO **engine** then builds predictive model(s) with one or more algorithms using the data.
After it is deployed as a web service, it listens to queries from your application and respond with predicted results in real-time.

![PredictionIO Single Engine Overview](/images/overview-singleengine.png)


[Event Server](/datacollection/) collects data from your application, in real-time or in batch. It can also unify data that are related to your application from multiple platforms.
After data is collected, it mainly serves two purposes:

1. Provide data to Engine(s) for model training and evaluation
2. Offer a unified view for data analysis

Like a database server, Event Server can host multiple applications. Data are separated for each application by a unique *app_name*.

Once Event Server is launched, you can send data to a specific *app_name*, identified by an Access Key, through its [Event API](/datacollection/eventapi.html) with HTTP requests or with [one of the SDKs](/sdk/).

In some special case, you may want your engine to read data from another datastore instead of Event Server.
It can be achieved by [making some modifications](#).

## Engine

Engine is responsible for making prediction.
It contains one or more machine learning algorithms. An engine reads training data and build predictive model(s).
It is then deployed as a web service. A deployed engine responds to prediction queries from your application through REST API in real-time.

PredictionIO's [template gallery](http://templates.prediction.io/) offers Engine Templates for all kinds of machine learning tasks.
You can easily create one or more engines from these templates .

The components of a template, namely **Data Source**, **Data Preparator**, **Algorithm(s)**, and **Serving**, are all [customizable](/start/customize/) for your specific needs.
