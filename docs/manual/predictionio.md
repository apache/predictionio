---
layout: docs
title: Overview
---

# Overview

PredictionIO is comprised with two main components: **Event Server** and **Engine**. It ingests data from an application and outputs prediction results.

![System Overview Simple](/images/system-overview-simple.png)


After [installing PredictionIO](/install/index.html), your application can send data to
PredictionIO's *Event Server* through its *Event API*.

###App###

You can send data to PredictionIO from any application, be it a website or mobile app. Each application can be identified by a unique app_id. You can use PredictionIO with multiple apps at the same time.

###Event Server###

Event Server is designed to collect data into PredictionIO in an event-based style. Once the Event Server is launched, your application can send data to it through its Event API with HTTP requests or with the EventClient of PredictionIO's SDKs. 

Alterantively, you can import data from your own datastore instead of using Event Server. 

###Engine and Engine Instance###

The core of PredictionIO. An Engine represents a type of Machine Learning task. PredictionIO comes with a few built-in *Engines* for different types of Machine Learning tasks, e.g. Personalized Item Recommendation and Item Ranking. 

An Engine has the following components: DataSource, Data Preparator(Preparator), Algorithm, and Serving. To learn more about Engine and Engine Instance, please refer to [Engine and Engine Instance: A Closer Look](/engines/concept/index.html).

You may also [build your own Engines](/enginebuilders/index.html) for specific type of prediction problems.

You can create one or more *Engine Instance(s)* from an Engine. An Engine
Instance trains a predictive model according to its own parameter settings. After
an Engine Instance is deployed by PredictionIO, your application can send query
to it through its *Engine API* to retrieve prediction results.



