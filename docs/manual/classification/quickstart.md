---
layout: docs
title: Classification Quick Start
---

# Quick Start - Classification Engine Template

An engine template is a basic skeleton of an engine. PredictionIO's Classification Engine Template (/templates/scala-parallel-classification) has integrated **Apache Spark MLlib**'s Naive Bayes algorithm by default.
 You can customize it easily to fit your specific needs.

We are going to show you how to create your own classification engine for production use based on this template.


## Install PredictionIO

First you need to [install PredictionIO {{site.pio_version}}]({{site.baseurl}}/install/)

## Create a new Engine from an Engine Template

Let's say you have installed PredictionIO at */home/yourname/predictionio/*.
For convenience, add PredictionIO's binary command path to your PATH, i.e. /home/yourname/predictionio/bin:

```
$ PATH=$PATH:/home/yourname/predictionio/bin; export PATH
```

Now you create a new engine called *MyEngine* by cloning the MLlib Classification engine template:

```
$ cp -r /home/yourname/predictionio/templates/scala-parallel-classification MyEngine
$ cd MyEngine
```
* Assuming /home/yourname/predictionio is the installation directory of PredictionIO.*

By default, the engine reads training data from a text file located at data/sample_naive_bayes_data.txt. Use the sample data file from MLlib repo for now:

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_naive_bayes_data.txt --create-dirs -o data/sample_naive_bayes_data.txt
```

> We will update the engine template so that it will read from the Event Server by default soon.

## Deploy the Engine as a Service

To build *MyEngine* and deploy it as a service:

```
$ pio build
$ pio train
$ pio deploy
```

This will deploy an engine that binds to http://localhost:8000. You can visit that page in your web browser to check its status.

Now, You can try to retrieve predicted results.
For example, to predict the label of a product with feature 2, 0 and 0, you send this JSON { "features": [2, 0, 0] } to the deployed engine and it will return a JSON of the predicted label.

```
$ curl -H "Content-Type: application/json" -d '{ "features": [2, 0, 0] }' http://localhost:8000/queries.json

{"label":0.0}

```

And to predict the label of a product with feature 4, 3 and 8, you send this JSON { "features": [4, 3, 8] } to the deployed engine and it will return a JSON of the predicted label.

```
$ curl -H "Content-Type: application/json" -d '{ "features": [4, 3, 8] }' http://localhost:8000/queries.json

{"label":2.0}
```

Your MyEngine is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.

#### [Next: DASE Components Explained](../classification/dase.html)
