---
title: Model Re-training
---

You probably want to update the trained predictive model with newly collected data regularly.
To do so, run the *train* and *deploy* commands again:

```
$ pio train
$ pio deploy
```

For example, if you want to re-train the model every day, you may add this to your *crontab*:

```
0 0 * * *   $PIO_HOME/bin/pio train; $PIO_HOME/bin/pio deploy
```
where *$PIO_HOME* is the installation path of PredictionIO.