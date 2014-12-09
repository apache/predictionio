---
title: Specify a Different Engine Port
---

By default, **pio deploy** deploys an engine on port 8000.

You can specify another port with an *--port* argument. For example, to deploy on port 8123

```
pio deploy --port 8123
```

You can also specify the binding IP with *--ip*, which is set to *localhost* if not specified. For example:

```
pio deploy --port 8123 --ip 1.2.3.4
```
