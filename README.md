Tutorials
=========

- [Recommendation Engine with RDD-based Model using MLlib's ALS](engines/src/main/scala/recommendations/README.md)
- [Stock Prediction Engine with customizable algorithms](engines/src/main/stock/README.md)

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
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/scala/regression/local/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/local/params --metricsClass io.prediction.controller.MeanSquareError
```


### Parallel

```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/scala/regression/parallel/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression.parallel --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/parallel/params --metricsClass io.prediction.controller.MeanSquareError
```


Pure Java Workflow
------------------

```
bin/pio-class io.prediction.tools.RegisterEngine engines/src/main/java/regression/examples/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.engines.java.regression.MeanSquareMetrics
```


### Using Scala Metrics

Replace the last line in the section above with the following:

```
bin/pio-class io.prediction.tools.RunWorkflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.java.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/java/regression/examples --metricsClass io.prediction.controller.MeanSquareError
```


Deploying a Server
------------------

Following from instructions above, you should have obtained a run ID after
your workflow finished.

```
bin/pio-class io.prediction.tools.RunServer --runId RUN_ID_HERE
```

This will create a server that by default binds to http://localhost:8000. You
can visit that page in your web browser to check its status.

To perform real-time predictions, try the following:

```
curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000
curl -H "Content-Type: application/json" -d '[-0.8600615539670898, -1.0084357652346345, -1.3088407119560064, -1.9340485539299312, -0.6246990990796732, -2.325746651211032, -0.28429904752434976, -0.1272785164794058, -1.3787859877532718, -0.24374419289538318]' http://localhost:8000
```
