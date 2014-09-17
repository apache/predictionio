---
layout: docs
title: Overview
---

# Overview

![System Overview](/images/system-overview.png)

**Data Collection with Event Server**

After PredictionIO is installed, your application can send data to PredictionIO's *Event Server* through its *Event API*.

**App**

You may use PredictionIO for more than one application.  *App ID* is used to identify applications uniquely. 

**Engine and Engine Instance**

PredictionIO comes with a few built-in *Engines* for different types of Machine Learning tasks, e.g. Personalized Item Recommendation, Item Similarity and Item Ranking. You may also build your own engines for specific type of prediction problems.

You can create one or more *Engine Instance(s)* from an Engine. An engine instance trains predictive model according to its own parameter settings.
After an engine instance is deployed by PredictionIO, your application can send query to it through its *Engine API* to retrieve prediction results. 


## Engine and Engine Instance: A Closer Look

An Engine represents a type of Machine Learning task. It follows the DASE architecture which contains the following components: 

### [D] Data Source and Data Preparator

Data Source reads data from source and transforms it to the desired format. Data Preparator preprocesses the data and forward it to the algorithm for model training. 

### [A] Algorithm

A Machine Learning Algorithm, and the settings of its parameters, determines how a predictive model is constructed.  

### [S] Serving

The Serving component takes prediction *queries* and returns prediction results. If the engine has multiple algorithms, Serving will combine the results into one. Additionally, you can add your own business logic in Serving and further customize the final returned results. 

### [E] Evaluation Metrics

An Evaluation Metrics quantifies prediction accuracy with a score. It can be used for comparing algorithms or algorithm parameter settings.

> PredictionIO helps you modularize these components so you can build, for example, several Serving components for an Engine. You will be able to choose which one to be deployed when you create an Engine Instance.


![Engine Overview](/images/engineinstance-overview.png)

## Engine Instance Deployment

An Engine Instance is a deployable implementation of an Engine with defined parameter settings.
It specifies:

* One Data Source

* One Data Preparator

* One or more Algorithm(s)

* One Serving

> If more than one algorithm is specified, each of their model prediction results will be passed to Serving for ensembling.

Each Engine Instance processes data and constructs predictive models independently. Therefore, every engine instance serves its own set of prediction results. For example, you may deploy two engine instances for your mobile application: one for recommending news to users and another one for suggesting new friends to users. 
 

## Engine Instance Evaluation

To evaluate the prediction accuracy of an Engine Instance, all you need to do is to specify an Evaluation Metrics when you run an evaluation on an Engine Instance, i.e.:

* An Engine Instance (One Data Source, One Data Preparator, One or more Algorithms, One Serving

* One Evaluation Metrics
