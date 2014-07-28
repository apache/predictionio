Tutorials
=========

The purpose of the tutorials 1-4 is to help you to get familiar with each components of the PredictionIO framework.

- [Tutorial 1 - Develop and Integrate Algorithm with PredictionIO
](engines/src/main/java/recommendations/tutorial1-develop.md)
- [Tutorial 2 - Test Engine Components
](engines/src/main/java/recommendations/tutorial2-runner.md)
- [Tutorial 3 - Evaluation](engines/src/main/java/recommendations/tutorial3-evaluation.md)
- [Tutorial 4 - Multiple Algorithms Engine](engines/src/main/java/recommendations/tutorial4-multialgo.md)

More interesting tutorials:
- [Stock Prediction Engine with customizable algorithms](engines/src/main/scala/stock/README.md)
- [Distributed Recommendation Engine with RDD-based Model using MLlib's ALS](engines/src/main/scala/recommendations/README.md)

API Documentation
=================

1.  Run this command.
    ```
    sbt/sbt unidoc
    ```

2.  Point your web browser at `target/scala-2.10/unidoc/index.html` for
    ScalaDoc, or `target/javaunidoc/index.html` for Javadoc.


Storage Setup
=============

PredictionIO relies on a data store to store its meta data. At the moment,
PredictionIO's storage layer supports both
[Elasticsearch](http://www.elasticsearch.org/) and
[MongoDB](http://www.mongodb.org/). Make sure you have one of these running and
functioning properly on your computer.

1. Copy ``conf/pio-env.sh.template`` to ``conf/pio-env.sh``.

2. If you use Elasticsearch (default), change the following to fit your setup.
   ```
   PIO_STORAGE_SOURCES_ELASTICSEARCH_TYPE=elasticsearch
   PIO_STORAGE_SOURCES_ELASTICSEARCH_HOSTS=localhost
   PIO_STORAGE_SOURCES_ELASTICSEARCH_PORTS=9300
   ```
   If you use MongoDB, add and modify the following to fit your setup.
   ```
   PIO_STORAGE_SOURCES_MONGODB_TYPE=mongodb
   PIO_STORAGE_SOURCES_MONGODB_HOSTS=localhost
   PIO_STORAGE_SOURCES_MONGODB_PORTS=27017
   ```

3. The following points the storage repositories to their respective backend
   data sources. By default, they point to Elasticsearch.
   ```
   PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=ELASTICSEARCH
   PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=ELASTICSEARCH
   ```
   If you use MongoDB, change them to something like this.
   ```
   PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=MONGODB
   PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=MONGODB
   ```

4. Save ``conf/pio-env.sh`` and you are done!


Running Evaluation and Server
=============================

The following is a regression example:

- Make sure Elasticsearch is running at localhost:9300, or MongoDB is running at
  localhost:27017.
- Make sure you have downloaded and extracted an Apache Spark binary
  distribution, or have built it from source. You will need to point your
  environmental SPARK_HOME to the root directory of your Apache Spark
  installation.
- You only need to run RegisterEngine once unless you updated your engine's
  manifest.

First, copy ``conf/pio-env.sh.template`` to ``conf/pio-env.sh``. If you use
MongoDB, add these:

```
PIO_STORAGE_SOURCES_MONGODB_TYPE=mongodb
PIO_STORAGE_SOURCES_MONGODB_HOSTS=localhost
PIO_STORAGE_SOURCES_MONGODB_PORTS=27017
```

and change these lines

```
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=ELASTICSEARCH
PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=ELASTICSEARCH
```

to

```
PIO_STORAGE_REPOSITORIES_METADATA_SOURCE=MONGODB
PIO_STORAGE_REPOSITORIES_APPDATA_SOURCE=MONGODB
```

Once your data store is properly configured, build binaries:

```
sbt/sbt package
sbt/sbt engines/assemblyPackageDependency
sbt/sbt tools/assembly
```


Pure Scala Workflow
-------------------


### Local

```
bin/register-engine engines/src/main/scala/regression/local/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/local/params --metricsClass io.prediction.controller.MeanSquareError
```


### Parallel

```
bin/register-engine engines/src/main/scala/regression/parallel/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression.parallel --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/parallel/params --metricsClass io.prediction.controller.MeanSquareError
```


Pure Java Workflow
------------------

```
bin/register-engine engines/src/main/java/regression/examples/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.engines.java.regression.MeanSquareMetrics
```


### Using Scala Metrics

Replace the last line in the section above with the following:

```
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.controller.MeanSquareError
```


Deploying a Server
------------------

Following from instructions above, you should have obtained a run ID after
your workflow finished.

```
bin/run-server --runId RUN_ID_HERE
```

This will create a server that by default binds to http://localhost:8000. You
can visit that page in your web browser to check its status.

To perform real-time predictions, try the following:

```
curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000
curl -H "Content-Type: application/json" -d '[-0.8600615539670898, -1.0084357652346345, -1.3088407119560064, -1.9340485539299312, -0.6246990990796732, -2.325746651211032, -0.28429904752434976, -0.1272785164794058, -1.3787859877532718, -0.24374419289538318]' http://localhost:8000
```
