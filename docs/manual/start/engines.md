---
layout: docs
title:  Engine and Engine Instance 
---
<code>Rewrite/Update</code>

# A Closer Look at the Engine

An Engine is a template for a type of Machine Learning task. It follows the DASE architecture, containing the following components:

## [D] Data Source and Data Preparator

A Data Source reads data from an input source and transforms it into a desired format. A Data Preparator preprocesses the data and forwards it to the algorithm for model training.

## [A] Algorithm

A Machine Learning Algorithm, and the settings of its parameters, determines how a predictive model is constructed.

## [S] Serving

The Serving component takes prediction *queries* and returns prediction results. If the engine has multiple algorithms, Serving will combine the results into
one. Additionally, business-specific logic can be added in Serving to further
customize the final returned results.

## [E] Evaluation Metrics

An Evaluation Metric quantifies prediction accuracy with a numerical score. It can be used for comparing algorithms or algorithm parameter settings.

> PredictionIO helps you modularize these components so you can build, for
example, several Serving components for an Engine. You will be able to choose
which one to be deployed when you create an Engine Instance.


![Engine Overview]({{ site.baseurl }}/images/engineinstance-overview.png)

# Engine Instance Deployment

An Engine Instance is a deployable implementation of an Engine with defined
parameter settings. It specifies:

* One Data Source

* One Data Preparator

* One or more Algorithm(s)

* One Serving

> If more than one algorithm is specified, each of their model prediction
results will be passed to Serving for ensembling.

Each Engine Instance processes data and constructs predictive models
independently. Therefore, every engine instance serves its own set of prediction
results. For example, you may deploy two engine instances for your mobile
application: one for recommending news to users and another one for suggesting
new friends to users.


# Engine Instance Evaluation

To evaluate the prediction accuracy of an Engine Instance, all you need to do is
to specify an Evaluation Metric when you run an evaluation on an Engine
Instance, i.e.:

* An Engine Instance (One Data Source, One Data Preparator, One or more
  Algorithm(s), One Serving)

* One Evaluation Metric


# Engine Templates

PredictionIO currently offers two engine templates for **Apache Spark MLlib**:

* Collaborative Filtering Engine Template - with MLlib ALS (/templates/scala-parallel-recommendation)
* Classification Engine Template - with MLlib Naive Bayes  (/templates/scala-parallel-classification)

This tutorial shows you how to use the Collaborative Filtering Engine Template to build your own recommendation engine for production use.
The usage of other engine templates are very similar.

> What is an Engine Template?
>
> It is a basic skeleton of an engine. You can customize it easily to fit your specific needs.

PredictionIO offers the following features on top of Apache Spark  MLlib project:

* Deploy Spark MLlib model as a service on a production environment
* Support JSON query to retrieve prediction online
* Offer separation-of-concern software pattern based on the DASE architecture
* Support model update with new data
