<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

Apache PredictionIO Docker
==========================

## Overview

PredictionIO Docker provides Docker image for use in development and production environment.


## Usage

### Run PredictionIO with Selectable docker-compose Files

You can choose storages for event/meta/model to select docker-compose.yml.

```
docker-compose -f docker-compose.yml -f ... up
```

Supported storages are as below:

| Type  | Storage                          |
|:-----:|:---------------------------------|
| Event | Postgresql, MySQL, Elasticsearch |
| Meta  | Postgresql, MySQL, Elasticsearch |
| Model | Postgresql, MySQL, LocalFS       |

If you run PredictionIO with Postgresql, run as below:

```
docker-compose -f docker-compose.yml \
  -f pgsql/docker-compose.base.yml \
  -f pgsql/docker-compose.meta.yml \
  -f pgsql/docker-compose.event.yml \
  -f pgsql/docker-compose.model.yml \
  up
```

To use localfs as model storage, change as below:

```
docker-compose -f docker-compose.yml \
  -f pgsql/docker-compose.base.yml \
  -f pgsql/docker-compose.meta.yml \
  -f pgsql/docker-compose.event.yml \
  -f localfs/docker-compose.model.yml \
  up
```

## Tutorial

In this demo, we will show you how to build a recommendation template.

### Run PredictionIO environment

The following command starts PredictionIO with an event server.
PredictionIO docker image mounts ./templates directory to /templates.

```
$ docker-compose -f docker-compose.yml \
    -f pgsql/docker-compose.base.yml \
    -f pgsql/docker-compose.meta.yml \
    -f pgsql/docker-compose.event.yml \
    -f pgsql/docker-compose.model.yml \
    up
```

We provide `pio-docker` command as an utility for `pio` command.
`pio-docker` invokes `pio` command in PredictionIO container.

```
$ export PATH=`pwd`/bin:$PATH
$ pio-docker status
...
[INFO] [Management$] Your system is all ready to go.
```

### Download Recommendation Template

This demo uses [predictionio-template-recommender](https://github.com/apache/predictionio-template-recommender).

```
$ cd templates
$ git clone https://github.com/apache/predictionio-template-recommender.git MyRecommendation
$ cd MyRecommendation
```

### Register Application

You need to register this application to PredictionIO:

```
$ pio-docker app new MyApp1
[INFO] [App$] Initialized Event Store for this app ID: 1.
[INFO] [Pio$] Created a new app:
[INFO] [Pio$]       Name: MyApp1
[INFO] [Pio$]         ID: 1
[INFO] [Pio$] Access Key: i-zc4EleEM577EJhx3CzQhZZ0NnjBKKdSbp3MiR5JDb2zdTKKzH9nF6KLqjlMnvl
```

Since an access key is required in subsequent steps, set it to ACCESS_KEY.

```
$ ACCESS_KEY=i-zc4EleEM577EJhx3CzQhZZ0NnjBKKdSbp3MiR5JDb2zdTKKzH9nF6KLqjlMnvl
```

`engine.json` contains an application name, so replace `INVALID_APP_NAME` with `MyApp1`.

```
...
"datasource": {
  "params" : {
    "appName": "MyApp1"
  }
},
...
```

### Import Data

To import training data to Event server for PredictionIO, this template provides an import tool.
The tool depends on PredictionIO Python SDK and install as below:

```
$ pip install predictionio
```
and then import data:
```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_movielens_data.txt --create-dirs -o data/sample_movielens_data.txt
$ python data/import_eventserver.py --access_key $ACCESS_KEY
```

### Build Template

This is Scala based template.
So, you need to build this template by `pio` command.

```
$ pio-docker build --verbose
```

### Train and Create Model

To train a recommendation model, run `train` sub-command:

```
$ pio-docker train
```

### Deploy Model

If a recommendation model is created successfully, deploy it to Prediction server for PredictionIO.

```
$ pio-docker deploy

```
You can check predictions as below:
```
$ curl -H "Content-Type: application/json" \
-d '{ "user": "1", "num": 4 }' http://localhost:8000/queries.json
```

## Advanced Topics

### Run with Elasticsearch

For Elasticsearch, Meta and Event storage are available.
To start PredictionIO with Elasticsearch,

```
docker-compose -f docker-compose.yml \
  -f elasticsearch/docker-compose.base.yml \
  -f elasticsearch/docker-compose.meta.yml \
  -f elasticsearch/docker-compose.event.yml \
  -f localfs/docker-compose.model.yml \
  up
```

### Run with Spark Cluster

Adding `docker-compose.spark.yml`, you can use Spark cluster on `pio train`.

```
docker-compose -f docker-compose.yml \
  -f docker-compose.spark.yml \
  -f elasticsearch/docker-compose.base.yml \
  -f elasticsearch/docker-compose.meta.yml \
  -f elasticsearch/docker-compose.event.yml \
  -f localfs/docker-compose.model.yml \
  up
```

To submit a training task to Spark Cluster, run `pio-deploy train` with `--master` option:

```
pio-docker train -- --master spark://spark-master:7077
```

See `docker-compose.spark.yml` if changing settings for Spark Cluster.

### Run Engine Server

To deploy your engine and start an engine server, run Docker with `docker-compose.deploy.yml`.

```
docker-compose -f docker-compose.yml \
  -f pgsql/docker-compose.base.yml \
  -f pgsql/docker-compose.meta.yml \
  -f pgsql/docker-compose.event.yml \
  -f pgsql/docker-compose.model.yml \
  -f docker-compose.deploy.yml \
  up
```

See `deploy/run.sh` and `docker-compose.deploy.yml` if changing a deployment.

### Run with Jupyter

You can launch PredictionIO with Jupyter.

```
docker-compose -f docker-compose.jupyter.yml \
  -f pgsql/docker-compose.base.yml \
  -f pgsql/docker-compose.meta.yml \
  -f pgsql/docker-compose.event.yml \
  -f pgsql/docker-compose.model.yml \
  up
```

For more information, see [JUPYTER.md](./JUPYTER.md).

## Development

### Build Base Docker Image

```
docker build -t predictionio/pio pio
```

### Build Jupyter Docker Image

```
docker build -t predictionio/pio-jupyter jupyter
```

### Push Docker Image

```
docker push predictionio/pio:latest
docker tag predictionio/pio:latest predictionio/pio:$PIO_VERSION
docker push predictionio/pio:$PIO_VERSION
```
