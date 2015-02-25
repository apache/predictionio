---
title: Deploying an Engine
---

An engine must be **built** (i.e. `pio build`) and **trained** (i.e. `pio train`)  before it can be deployed as a web service.

## Deploying an Engine the First Time

After you have already created an engine by [downloading an Engine Template](/start/download/),  you can deploy an engine with these steps:

1. Specify the app this engine will run for by setting the *App ID* value in *engine.json*
2. Run `pio build` to update the engine
2. Run `pio train` to train a predictive model with training data
3. Run `pio deploy` to deploy the engine as a service

A deployed engine listens to port 8000 by default. Your application can [send query to retrieve prediction](/appintegration/) in real-time through the REST interface.

## Update Model with New Data

You probably want to update the trained predictive model with newly collected data regularly.
To do so, run the `pio train` and `pio deploy` commands again:

```
$ pio train
$ pio deploy
```

For example, if you want to re-train the model every day, you may add this to your *crontab*:

```
0 0 * * *   $PIO_HOME/bin/pio train; $PIO_HOME/bin/pio deploy
```
where *$PIO_HOME* is the installation path of PredictionIO.


## Specify a Different Engine Port

By default, `pio deploy` deploys an engine on **port 8000**.

You can specify another port with an *--port* argument. For example, to deploy on port 8123

```
pio deploy --port 8123
```

You can also specify the binding IP with *--ip*, which is set to *localhost* if not specified. For example:

```
pio deploy --port 8123 --ip 1.2.3.4
```


