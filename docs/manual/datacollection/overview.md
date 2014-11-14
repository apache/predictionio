---
layout: docs
title: Event Server Overview
---

# Overview

PredictionIO offers an EventServer that collects data in an event-based style via RESTful APIs. By default, EventServer uses HBase as data store. 

![EventServer Highlight]({{site.baseurl}}/images/eventserver-overview.png)


## What data should I collect? 
The EventServer can collect and store arbitrary events. At the beginning of your project, it is recommended to collect as much data as you can. Later on, you can exclude data that are not relevant to your predictive model in Data Preparator.

### Recommendation Engine

With Collaborative Filtering based Recommendation Engine, a common pattern is 

```
user — action — item
```

where users and items have properties associated with them. 

For example, for personalized product recommendation, the events would be
- User 1 purchased product X
- User 2 viewed product Y 
- User 1 added product Z in the cart 


Data collection varies quite a bit based on your application and your prediction goal. We are happy to [assist you with your questions](mailto:support@preidiction.io). 


### Classification Engine

<!-- ## Bulk import data -->
