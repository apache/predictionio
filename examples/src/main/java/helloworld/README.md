# My First "Hello World" Engine


Prepare training data:
```
$ cd $PIO_HOME/examples
$ cp data/helloworld/data1.csv data/helloworld/data.csv
```

Register engine:

```
$ ../bin/register-engine src/main/java/helloworld/engine.json
```

Train:

```
$ ../bin/run-train \
  --engineId org.sample.java.helloworld \
  --engineVersion 0.1
```

Example output:

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
