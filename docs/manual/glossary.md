---
layout: docs
title: Glossary
---

**DataPreparator**
- Part of Engine. It reads data from source and transforms it to the desired format.

**DataSource**
- Part of Engine. It preprocesses the data and forward it to the algorithm for model training. 

**Engine**
- An Engine represents a type of prediction, e.g. product recommendation. It is comprised of four components: [D] Data Source and Data Preparator, [A] Algorithm, [S] Serving, [E] Evaluation Metrics. 

**Engine Instance**
- An Engine Instance is a deployable implementation of an Engine with defined parameter settings. It specifies: one Data Source, one Data Preparator. one or more algorithm(s), and one Serving. You can deploy one or more engine instances from an engine at the same time.


**EngineClient**
- Part of PredictionSDK. It sends queries to a deployed engine instance through the Engine API and retrives prediction results. 

**Event API**
- Please see Event Server. 

**Event Server**
- Event Server is designed to collect data into PredictionIO in an event-based style. Once the Event Server is launched, your application can send data to it through its Event API with HTTP requests or with the EventClient of PredictionIO's SDKs.

**EventClient**
- Please see Event Server. 

**Live Evaluation**

**Offline Evaluation**

**Test Data** 

**Training Data**

**Training**
