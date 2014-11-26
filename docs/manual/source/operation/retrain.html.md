---
title: Model Re-training
---

# Model Re-training

You can update the predictive model with new data by making the *train* and *deploy* commands again:

1.  Assuming you already have a deployed engine running, go to http://localhost:8000 to check its status. Take note of the
    **Instance ID** at the top.

2.  Run training and deploy again. There is no need to manually terminate the previous deploy instance.

    ```
    $ pio train
    $ pio deploy
    ```

3.  Refresh the page at http://localhost:8000, you should see the status page with a new **Instance ID** at the top.


For example, if you want to re-train the model every day, you may add this to your *crontab*:

```
0 0 * * *   $PIO_HOME/bin/pio train; $PIO_HOME/bin/pio deploy
```
where *$PIO_HOME* is the installation path of PredictionIO.