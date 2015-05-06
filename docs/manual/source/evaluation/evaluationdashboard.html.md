---
title: Evaluation Dashboard
---

PredictionIO provides a web dashboard which allows you to see previous
evaluation and a drill down page about each evaluation. It is particularly
useful when we ran multiple [hyperparameter tunings](/evaluation/paramtuning/)
as we may easily lose track of all the engine variants evaluated.

We can start the dashboard with the following command:

```
$ pio dashboard
```

The dashboard lists out all completed evaluations in a reversed chronological
order. A high level description of each evaluation can be seen directly from the
dashboard. We can also click on the *HTML* button to see the evaluation drill
down page.
