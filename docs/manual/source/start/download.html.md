---
title: Downloading an Engine Template
---

The first step to create a new engine is to browse [PredictionIO template gallery](http://templates.prediction.io/).
Choose an engine template that matches your use case the best. You can further customize the engine later if you like.

"Vanilla" is the generic template which may be customized to do any kind of machine learning task.

To download a template, run:

```
pio template get <template-repo-path> <new-engine-directory>
```

For example, to create "MyRecommendation" with the basic Recommendation template, run:

```
pio template get PredictionIO/template-scala-parallel-recommendation MyRecommendation
```



