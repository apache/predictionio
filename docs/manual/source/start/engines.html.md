---
title: Engine - A Closer Look
---

An Engine is a type of Machine Learning task. It follows the DASE architecture,
containing the following components:

### [D] Data Source and Data Preparator

Data Source reads data from an input source and transforms it into a desired
format. Data Preparator preprocesses the data and forwards it to the algorithm
for model training.

### [A] Algorithm

The Algorithm component includes the Machine Learning algorithm, and the
settings of its parameters, determines how a predictive model is constructed.

### [S] Serving

The Serving component takes prediction *queries* and returns prediction results.
If the engine has multiple algorithms, Serving will combine the results into
one. Additionally, business-specific logic can be added in Serving to further
customize the final returned results.

### [E] Evaluation Metrics

An Evaluation Metric quantifies prediction accuracy with a numerical score. It
can be used for comparing algorithms or algorithm parameter settings.

> PredictionIO helps you modularize these components so you can build, for
example, several Serving components for an Engine. You will be able to choose
which one to be deployed when you create an Engine.


![Engine Overview](/images/engineinstance-overview.png)


## Engine Templates

While PredictionIO makes it easy to create your own engine completely from
scratch, it also comes with engine templates that are almost-complete engine
implementations. You can customize it easily to fit your specific needs.
PredictionIO currently offers two engine templates for **Apache Spark MLlib**:

* [Recommendation Engine Template - with MLlib ALS]
  (/templates/recommendation/quickstart)
* [Classification Engine Template - with MLlib Naive Bayes]
  (/templates/classification/quickstart)


## Engine Deployment

An engine puts all DASE components into a deployable state by specifying:

* One Data Source

* One Data Preparator

* One or more Algorithm(s)

* One Serving

INFO: If more than one algorithm is specified, each of their model prediction
results will be passed to Serving for ensembling.

Each Engine processes data and constructs predictive models independently.
Therefore, every engine serves its own set of prediction results. For example,
you may deploy two engines for your mobile application: one for recommending
news to users and another one for suggesting new friends to users.


## Engine Evaluation

To evaluate the prediction accuracy of an Engine, all you need to do is to
specify an Evaluation Metric when you run an evaluation on an Engine, i.e.:

* An Engine (One Data Source, One Data Preparator, One or more Algorithm(s), One
  Serving)

* One Evaluation Metric

Having a good understanding of Engine and Engine Template, you can now follow
the Quick Start guide and develop a custom engine.

#### [Next: Recommendation Engine Quick Start](/templates/recommendation/quickstart)
