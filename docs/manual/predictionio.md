---
layout: docs
title: Overview
---

# Overview

PredictionIO is comprised with two main components: **Event Server** and
**Engine**. It ingests data from an application and outputs prediction results.

![System Overview Simple]({{site.baseurl}}/images/system-overview-simple.png)


After [installing PredictionIO]({{site.baseurl}}/install), your application can send data to
PredictionIO's *Event Server* through its *Event API*.

## App

You can send data to PredictionIO from any application, be it a website or
mobile app. Each application can be identified by a unique *app_id*. You can use
PredictionIO with multiple apps at the same time.

## Event Server

Event Server is designed to collect data into PredictionIO in an event-based
style. Once the Event Server is launched, your application can send data to it
through its Event API with HTTP requests or with the EventClient of
PredictionIO's SDKs.

Alternatively, you can import data from your own datastore instead of using
Event Server.

## Engine and Engine Instance

Engine is the core of PredictionIO. It represents a type of Machine Learning task. Each Engine has the following components: DataSource, Data Preparator(Preparator),
Algorithm, and Serving.

PredictionIO comes with a few *Engines* examples for different types of Machine
Learning tasks, e.g. Personalized Item Recommendation and Item Ranking. You may run multiple types of Machine Learning tasks for one app at the same time:

![System Overview]({{site.baseurl}}/images/system-overview.png)

To learn more about Engine and Engine Instance, please
refer to [Engine and Engine Instance: A Closer Look]({{site.baseurl}}/engines/concept).


<!--
You may also [build your own Engines]({{site.baseurl}}/enginebuilders) for specific type of
prediction problems.
 -->
 
Next, you can follow the [Engine Templates Quick Start]({{site.baseurl}}/templates/) and build your first predictive engine. 
