---
layout: docs
title: System Overview
---

#  PredictionIO   Overview

PredictionIO is comprised with two main components: **Event Server** and
**Engine**. It ingests data from an application and outputs prediction results.

![System Overview Simple]({{site.baseurl}}/images/system-overview-simple.png)


After [installing PredictionIO]({{site.baseurl}}/install), your application can send data to
PredictionIO's [Event Server](../eventserver/overview.html) through [Event API](../eventserver/eventapi.html).

## App

You can send data to PredictionIO from any application, be it a website or
mobile app. Each application can be identified by a unique *app_name*. You can use
PredictionIO with multiple apps at the same time.

## Event Server

Event Server is designed to collect data into PredictionIO in an event-based
style. Once the Event Server is launched, your application can send data to it
through its Event API with HTTP requests or with the EventClient of
PredictionIO's SDKs.

Alternatively, you can import data from your own datastore instead of using
Event Server.

## Engine

Engine is the core of PredictionIO. It represents a type of Machine Learning task. Each Engine has the following components: DataSource, Data Preparator(Preparator),
Algorithm, and Serving.

PredictionIO comes with a few *Engines* templates for different types of Machine Learning, e.g. [Recommendation](../recommendation/quickstart.html) and [Classification](../classification/quickstart.html) that can be customized to support your unqiue machine learning needs. 

To learn more about Engine and Engine Templates, please
refer to [Engine: A Closer Look]({{site.baseurl}}/engines/concept).


#### [Next: Install PredictionIO](../install/)

