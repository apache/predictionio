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

Jupyter With PredictionIO
=========================

## Overview

Using Jupyter based docker, you can use Jupyter Notebook with PredictionIO environment.
It helps you with your exploratory data analysis (EDA).

## Run Jupyter Notebook

First of all, start Jupyter container with PredictionIO environment:

```
docker-compose -f docker-compose.jupyter.yml \
  -f pgsql/docker-compose.base.yml \
  -f pgsql/docker-compose.meta.yml \
  -f pgsql/docker-compose.event.yml \
  -f pgsql/docker-compose.model.yml \
  up
```

Open `http://127.0.0.1:8888/` and then open a new terminal in Jupyter from `New` pulldown button.

## Getting Started With Scala Based Template

### Download Template

Clone a template using Git:

```
cd templates/
git clone https://github.com/apache/predictionio-template-recommender.git
cd predictionio-template-recommender/
```

Replace a name with `MyApp1`.

```
sed -i "s/INVALID_APP_NAME/MyApp1/" engine.json
```

### Register New Application

Using pio command, register a new application as `MyApp1`.

```
pio app new MyApp1
```

This command prints an access key as below.

```
[INFO] [Pio$] Access Key: bbe8xRHN1j3Sa8WeAT8TSxt5op3lUqhvXmKY1gLRjg70K-DUhHIJJ0-UzgKumxGm
```

Set it to an environment variable `ACCESS_KEY`.

```
ACCESS_KEY=bbe8xRHN1j3Sa8WeAT8TSxt5op3lUqhvXmKY1gLRjg70K-DUhHIJJ0-UzgKumxGm
```

### Import Training Data

Download trainging data and import them to PredictionIO Event server.

```
curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_movielens_data.txt --create-dirs -o data/sample_movielens_data.txt
python data/import_eventserver.py --access_key $ACCESS_KEY
```

### Build Template

Build your template by the following command:

```
pio build --verbose
```

### Create Model

To create a model, run:

```
pio train
```

## Getting Started With Python Based Template

### Download Template

Clone a template using Git:

```
cd templates/
git clone https://github.com/jpioug/predictionio-template-iris.git
predictionio-template-iris/
```

### Register New Application

Using pio command, register a new application as `IrisApp`.

```
pio app new --access-key IRIS_TOKEN IrisApp
```

### Import Training Data

Download trainging data and import them to PredictionIO Event server.

```
python data/import_eventserver.py
```

### Build Template

Build your template by the following command:

```
pio build --verbose
```

### EDA

To do data analysis, open `templates/predictionio-template-iris/eda.ipynb` on Jupyter.

### Create Model

You need to clear the following environment variables in the terminal before executing `pio train`.

```
unset PYSPARK_PYTHON
unset PYSPARK_DRIVER_PYTHON
unset PYSPARK_DRIVER_PYTHON_OPTS
```

To create a model, run:

```
pio train --main-py-file train.py
```


