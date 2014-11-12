---
layout: docs
title: Recommendation Quick Start
---

# Quick Start - Recommendation Engine Template

An engine template is a basic skeleton of an engine. PredictionIO's Recommendation Engine Template (/templates/scala-parallel-recommendation) has integrated **Apache Spark MLlib**'s Collaborative Filtering algorithm by default.
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

Now you create a new engine called *MyEngine* by cloning the MLlib Collaborative Filtering engine template:

```
$ cp -r /home/yourname/predictionio/templates/scala-parallel-recommendation MyEngine
$ cd MyEngine
```
* Assuming /home/yourname/predictionio is the installation directory of PredictionIO.*

By default, the engine reads training data from a text file located at data/sample_movielens_data.txt. Use the sample movie data from MLlib repo for now:

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_movielens_data.txt --create-dirs -o data/sample_movielens_data.txt
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


![Engine Status]({{ site.baseurl }}/images/engine-server.png)

|

Now, You can try to retrieve predicted results.
To recommend 4 movies to user whose id is 1, you send this JSON { "user": 1, "num": 4 } to the deployed engine and it will return a JSON of the recommended movies.

```
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' http://localhost:8000/queries.json

{"productScores":[{"product":22,"score":4.072304374729956},{"product":62,"score":4.058482414005789},{"product":75,"score":4.046063009943821},{"product":68,"score":3.8153661512945325}]}
```

Your MyEngine is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.


Your MyEngine is now running. Next, we are going to take a look at the engine architecture and explain how you can customize it completely.
