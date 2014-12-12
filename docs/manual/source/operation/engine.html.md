---
title: Running an Engine
---

This section documents helpful tips for running the engine.

##Specify a Different Engine Port

By default, `pio deploy` deploys an engine on **port 8000**.

You can specify another port with an *--port* argument. For example, to deploy on port 8123

```
pio deploy --port 8123
```

You can also specify the binding IP with *--ip*, which is set to *localhost* if not specified. For example:

```
pio deploy --port 8123 --ip 1.2.3.4
```


##Model Re-training

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