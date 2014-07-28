Linear Regression Engine
========================

This document describes a Scala-based single-machine linear regression engine.


Prerequisite
------------

Make sure you have built PredictionIO and setup storage described
[here](/#storage-setup).


High Level Description
----------------------

This engine demonstrates how one can simply wrap around the
[Nak](https://github.com/scalanlp/nak) library to train a linear regression
model and serve real-time predictions.

All code definition can be found [here](Run.scala).


### Data Source

Training data is located at `/data/lr_data.txt`. The first column are values of
the dependent variable, and the rest are values of explanatory variables. In
this example, they are represented by the `TrainingData` case class as a vector
of double (all rows of the first column), and a vector of vector of double (all
rows of the remaining columns) respectively.


### Preparator

The preparator in this example accepts two parameters: `n` and `k`. Each row of
data is indexed by `index` starting from 0. When `n > 0`, rows matching `index
mod n = k` will be dropped.


### Algorithm

This example engine contains one single algorithm that wraps around the Nak
library's linear regression routine. The `train()` method simply massage the
`TrainingData` into a form that can be used by Nak.


### Serving

This example engine uses `FirstServing`, which serves only predictions from the
first algorithm. Since there is only one algorithm in this engine, predictions
from the linear regression algorithm will be served.


Training a Model
----------------

This example provides a set of ready-to-use parameters for each component
mentioned in the previous section. They are located inside the `params`
subdirectory.

Before training, you must let PredictionIO know about the engine. Run the
following command to register the engine.
```
cd $PIO_HOME
bin/register-engine engines/src/main/scala/regression/local/manifest.json core/target/scala-2.10/core_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines_2.10-0.8.0-SNAPSHOT.jar engines/target/scala-2.10/engines-assembly-0.8.0-SNAPSHOT-deps.jar
```
where `$PIO_HOME` is the root directory of the PredictionIO code tree.

To start training, use the following command.
```
cd $PIO_HOME
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/local/params
```
This will train a model and save it in PredictionIO's metadata storage. Notice
that when the run is completed, it will display a run ID, like below.
```
14/07/28 14:57:27 INFO SparkContext: Job finished: collect at DebugWorkflow.scala:553, took 0.038936 s
14/07/28 14:57:27 INFO APIDebugWorkflow$: Metrics is null. Stop here
14/07/28 14:57:28 INFO APIDebugWorkflow$: Run information saved with ID: rO3kuBnmTi6zYBej5x1MWg
```
Take a note of the ID
for later use.


Running Evaluation Metrics
--------------------------

To run evaluation metrics, simply add an argument to the `run-workflow` command.
```
cd $PIO_HOME
bin/run-workflow --sparkHome $SPARK_HOME --engineId io.prediction.engines.regression --engineVersion 0.8.0-SNAPSHOT --jsonBasePath engines/src/main/scala/regression/local/params --metricsClass io.prediction.controller.MeanSquareError
```
Notice that we have appended `--metricsClass
io.prediction.controller.MeanSquareError` to the end of the command. This
instructs the workflow runner to run the specified metrics after training is
done. When you look at the console output again, you should be able to see a
mean square error computed, like the following.
```
14/07/28 14:58:16 INFO APIDebugWorkflow$: Set: The One Size: 1000 MSE: 0.092519
14/07/28 14:58:16 INFO APIDebugWorkflow$: APIDebugWorkflow.run completed.
14/07/28 14:58:16 INFO APIDebugWorkflow$: Run information saved with ID: HT_35CSbTEa91bx04nWwuQ
```


Deploying a Real-time Prediction Server
---------------------------------------

Following from instructions above, you should have obtained a run ID after
your workflow finished. Use the following command to start a server.
```
cd $PIO_HOME
bin/run-server --runId RUN_ID_HERE
```
This will create a server that by default binds to http://localhost:8000. You
can visit that page in your web browser to check its status.

To perform real-time predictions, try the following.
```
curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000
curl -H "Content-Type: application/json" -d '[-0.8600615539670898, -1.0084357652346345, -1.3088407119560064, -1.9340485539299312, -0.6246990990796732, -2.325746651211032, -0.28429904752434976, -0.1272785164794058, -1.3787859877532718, -0.24374419289538318]' http://localhost:8000
```
Congratulations! You have just trained a linear regression model and is able to
perform real time prediction.
