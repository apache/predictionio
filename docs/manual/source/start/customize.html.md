---
title: Customizing an Engine
---

When you download an engine template, it comes with the source code. All engine templates follow the same DASE architecture and they are designed to be customizable.

You may want to customize an engine for many reasons, for example: 

* Use another algorithm, or multiple of them
* Read data from a different, or existing, data store
* Read different types of training data
* Transform data with another approach
* Add new evaluation measures
* Add custom business logics


To learn more about DASE, please read "[Learning DASE](/customize/)".

After you have finished modifying the code, you can re-build and deploy the engine again with:

```
$ pio build; pio train; pio deploy
```