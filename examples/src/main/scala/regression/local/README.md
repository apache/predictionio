Linear Regression Engine
========================

This document describes a Scala-based single-machine linear regression engine.


Prerequisite
------------

Make sure you have built PredictionIO and setup storage described
[here](/README.md).


High Level Description
----------------------

This engine demonstrates how one can simply wrap around the
[Nak](https://github.com/scalanlp/nak) library to train a linear regression
model and serve real-time predictions.

All code definition can be found [here](Run.scala).


### Data Source

Training data is located at `/examples/data/lr_data.txt`. The first column are
values of the dependent variable, and the rest are values of explanatory
variables. In this example, they are represented by the `TrainingData` case
class as a vector of double (all rows of the first column), and a vector of
vector of double (all rows of the remaining columns) respectively.


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
$ cd $PIO_HOME/examples
$ ../bin/register-engine src/main/scala/regression/local/manifest.json
```
where `$PIO_HOME` is the root directory of the PredictionIO code tree.

To start training, use the following command.
```
$ cd $PIO_HOME/examples
$ ../bin/run-train \
  --engineId io.prediction.examples.regression \
  --engineVersion 0.8.0-SNAPSHOT \
  --jsonBasePath src/main/scala/regression/local/params
```
This will train a model and save it in PredictionIO's metadata storage. Notice
that when the run is completed, it will display a run ID, like below.
```
2014-08-05 14:01:28,312 INFO  SparkContext - Job finished: collect at DebugWorkflow.scala:569, took 0.043905 s
2014-08-05 14:01:28,313 INFO  APIDebugWorkflow$ - Metrics is null. Stop here
2014-08-05 14:01:28,482 INFO  APIDebugWorkflow$ - Run information saved with ID: qGGlujG0SMOkvCaA-YZmEw
```
Take a note of the ID
for later use.


Running Evaluation Metrics
--------------------------

To run evaluation metrics, simply add an argument to the `run-workflow` command.
```
$ cd $PIO_HOME/examples
$ ../bin/run-eval --engineId io.prediction.examples.regression \
  --engineVersion 0.8.0-SNAPSHOT \
  --jsonBasePath src/main/scala/regression/local/params \
  --metricsClass io.prediction.controller.MeanSquareError
```
Notice that we have appended `--metricsClass
io.prediction.controller.MeanSquareError` to the end of the command. This
instructs the workflow runner to run the specified metrics after training is
done. When you look at the console output again, you should be able to see a
mean square error computed, like the following.
```
2014-08-05 14:02:04,848 INFO  APIDebugWorkflow$ - Set: The One Size: 1000 MSE: 0.092519
2014-08-05 14:02:04,848 INFO  APIDebugWorkflow$ - APIDebugWorkflow.run completed.
2014-08-05 14:02:04,940 INFO  APIDebugWorkflow$ - Run information saved with ID: CM4y41D8TT-Ovh1l9PGRrw
```


Deploying a Real-time Prediction Server
---------------------------------------

Following from instructions above, you should have obtained a run ID after
your workflow finished. Use the following command to start a server.
```
$ cd $PIO_HOME/examples
$ ../bin/run-server --runId RUN_ID_HERE
```
This will create a server that by default binds to http://localhost:8000. You
can visit that page in your web browser to check its status.

To perform real-time predictions, try the following.
```
$ curl -H "Content-Type: application/json" -d '[2.1419053154730548, 1.919407948982788, 0.0501333631091041, -0.10699028639933772, 1.2809776380727795, 1.6846227956326554, 0.18277859260127316, -0.39664340267804343, 0.8090554869291249, 2.48621339239065]' http://localhost:8000
$ curl -H "Content-Type: application/json" -d '[-0.8600615539670898, -1.0084357652346345, -1.3088407119560064, -1.9340485539299312, -0.6246990990796732, -2.325746651211032, -0.28429904752434976, -0.1272785164794058, -1.3787859877532718, -0.24374419289538318]' http://localhost:8000
```
Congratulations! You have just trained a linear regression model and is able to
perform real time prediction.


Bonus: Production Prediction Server Deployment
----------------------------------------------

The prediction server you have launched in the previous section hosts an
immutable model in memory, i.e. you cannot update the model without restarting
the server.

Being immutable and stateless are important properties for horizontal scaling.
The server is designed in a way such that you can run other automation tools or
monitors to manage its lifecycle.

In this example, we will use [Supervisor](http://supervisord.org/) to manage our
prediction server.

**Make sure you have killed the server if you have launched one in the previous
section before proceeding to the following steps.**

1.  Start by installing Supervisor following instructions on its [web
    site](http://supervisord.org/).

2.  Create a Supervisor configuration at `$PIO_HOME/examples/supervisord.conf`
    with the following content.
    ```
    [unix_http_server]
    file=/tmp/supervisor.sock

    [inet_http_server]
    port=127.0.0.1:9001

    [supervisord]
    logfile=/tmp/supervisord.log
    logfile_maxbytes=50MB
    logfile_backups=10
    loglevel=info
    pidfile=/tmp/supervisord.pid
    nodaemon=false
    minfds=1024
    minprocs=200

    [rpcinterface:supervisor]
    supervisor.rpcinterface_factory = supervisor.rpcinterface:make_main_rpcinterface

    [supervisorctl]
    serverurl=unix:///tmp/supervisor.sock

    [program:pio]
    command=../bin/run-server --engineId io.prediction.examples.regression --engineVersion 0.8.0-SNAPSHOT
    autostart=false
    ```

3.  Launch Supervisor at `$PIO_HOME/examples`.
    ```
    $ cd $PIO_HOME/examples
    $ supervisord
    ```

4.  Using your web browser, go to http://localhost:9001. You should see a
    Supervisor status screen, showing that the `pio` process is stopped.

5.  Run training or evaluation. These scripts have been written to detect the
    existence of Supervisor and will automatically (re)start our prediction server.
    ```
    $ cd $PIO_HOME/examples
    $ ../bin/run-train \
      --engineId io.prediction.examples.regression \
      --engineVersion 0.8.0-SNAPSHOT \
      --jsonBasePath src/main/scala/regression/local/params
    ```

    or

    ```
    $ cd $PIO_HOME/examples
    $ ../bin/run-eval --engineId io.prediction.examples.regression \
      --engineVersion 0.8.0-SNAPSHOT \
      --jsonBasePath src/main/scala/regression/local/params \
      --metricsClass io.prediction.controller.MeanSquareError
    ```

6.  Refresh the Supervisor status screen. You should now see the server as
    running. If you go to http://localhost:8000, you should see the prediction
    server status page.
7.  Repeat steps 5 to 6 to see the prediction server restarted automatically
    after every training/evaluation.

Congratulations! You have just deployed a production-ready setup that can
restart itself automatically after every training! Simply add the training or
evaluation command to your `crontab`, and your setup will be able to re-deploy
itself automatically in a regular interval.
