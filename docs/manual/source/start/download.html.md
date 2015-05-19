---
title: Downloading an Engine Template
---

The first step to create a new engine is to browse [PredictionIO template gallery](http://templates.prediction.io/).
Choose an engine template that matches your use case the best. You can further customize the engine later if you like.

To download a template, run:

```
$ pio template get <template-repo-path> <new-engine-directory>
```

For example, to download the tempalte "template-scala-parallel-recommendation" to your directory "MyRecommendation", run:

```
$ pio template get PredictionIO/template-scala-parallel-recommendation MyRecommendation
```

NOTE: `pio` is a command available in the `bin/` of the installed PredictionIO directory. You may add the installed Prediction's bin/ directory path to you environment PATH.

Please browse the [template gallery]((http://templates.prediction.io/)) to choose an engine template.
