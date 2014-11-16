

Get sample data from MLlib repo and store inside data/ directory

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_naive_bayes_data.txt --create-dirs -o data/sample_naive_bayes_data.txt
```

Use the provided sample script to import sample data to Event Server

```
$ python data/import_eventserver.py --access_key <your_access_key>
```

Or use the following curl command to import events one by one:

```
curl -i -X POST http://localhost:7070/events.json?accessKey=<your_access_key> \
-H "Content-Type: application/json" \
-d '{
  "event" : "$set",
  "entityType" : "user"
  "entityId" : "0",
  "properties" : {
    "attr0" : 0,
    "attr1" : 1,
    "attr2" : 0,
    "plan" : 1
  }
  "eventTime" : "2004-12-13T21:39:45.618-07:00"
}'

```

```
$ $PIO_HOME/bin/pio build
$ $PIO_HOME/bin/pio train
$ $PIO_HOME/bin/pio deploy
```

```
$ curl -H "Content-Type: application/json" -d '{ "features": [2, 0, 0] }' http://localhost:8000/queries.json

{"label":0.0}
```

```
$ curl -H "Content-Type: application/json" -d '{ "features": [4, 3, 8] }' http://localhost:8000/queries.json

{"label":2.0}
```
