---
title: Event Server Overview
---

Apache PredictionIO (incubating) offers an Event Server that collects data in an
event-based style via a RESTful API. By default, Event Server uses Apache HBase
as data store.

![EventServer Highlight](/images/eventserver-overview.png)


## What data should I collect?

The Event Server can collect and store arbitrary events. At the beginning of
your project, it is recommended to collect as much data as you can. Later on,
you can exclude data that are not relevant to your predictive model in Data
Preparator.

### Recommendation Engine

With Collaborative Filtering based Recommendation Engine, a common pattern is

```
user -- action -- item
```

where users and items have properties associated with them.

For example, for personalized book recommendation, some events to collect would
be

- User 1 purchased product X
- User 2 viewed product Y
- User 1 added product Z in the cart

User properties can be gender, age, location, etc. Item properties can be genre,
author, and other attributes that may be related to the the user's preference.

Data collection varies quite a bit based on your application and your prediction
goal. We are happy to [assist you with your
questions](mailto:support@prediction.io).
