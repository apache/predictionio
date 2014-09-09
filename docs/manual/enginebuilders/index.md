---
layout: docs
title: Engine Builders' Guide
---

# Engine Builders' Guide

## DASE Architecture Overview

A PredictionIO engine workflow looks like the following figure:

```
            DataSource.read
             (TrainingData)
                   v
           Preparator.prepare
             (PreparedData)
                   v
     +-------------+-------------+
     v             v             v
Algo1.train   Algo2.train   Algo3.train
  (Model1)      (Model2)      (Model3)
     v             v             v
Algo1.predict Algo2.predict Algo3.predict <- (Query)
(Prediction1) (Prediction2) (Prediction3)
     v             v             v
     +-------------+-------------+
                   v
              Serving.serve
              (Prediction)
```


### [D] Data

Preparing the Data

### [A] Algorithm

Adding an Algorithm

### [S] Serving

Serving the prediction

### [E] Evaluation

Adding an Evaluation Metrics


