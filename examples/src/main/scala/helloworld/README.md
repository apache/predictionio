# My First "Hello World" Engine

## 1. DataSource

## 2. Algorithm

## 3. Deploy

Prepare training data:
```
$ cd $PIO_HOME/examples
$ cp data/helloworld/data1.csv data/helloworld/data.csv
```

Register engine:

```
$ ../bin/register-engine src/main/scala/helloworld/manifest.json
```

Train:

```
$ ../bin/run-train \
  --engineId io.prediction.engines.helloworld \
  --engineVersion 0.8.0-SNAPSHOT
```

Example output
```
2014-08-05 17:06:02,638 INFO  APIDebugWorkflow$ - Metrics is null. Stop here
2014-08-05 17:06:02,769 INFO  APIDebugWorkflow$ - Run information saved with ID: 201408050005
```

Note the retuned ID at the end.
Use the returned ID to Start server:

```
$ ../bin/run-server --runId <replaced by the ID>
```

Retrieve prediction:

```
$ curl -H "Content-Type: application/json" -d '{ "day": "Mon" }' http://localhost:8000
```

Output:

```
{"temperature":75.5}
```

Retrieve prediction:

```
$ curl -H "Content-Type: application/json" -d '{ "day": "Tue" }' http://localhost:8000
```

Output:
```
{"temperature":80.5}
```

## 4. Re-training

TBD

New data comes in:
```
$ cd $PIO_HOME/examples
$ cp data/helloworld/data2.csv data/helloworld/data.csv
```

Retrieve prediction:
