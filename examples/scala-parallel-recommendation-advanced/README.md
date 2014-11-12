## Advanced examples of scala-parallel-recommenation


Insert sample random ratings data into Mongo 'test' db
```
$ mongo data/insert_sample_ratings_mongo.js
```

engine-with-file.json is example engine which reads File as DataSource.
engine-with-mongo.json is example engine which reads Mongo as DataSource.

```
$ $PIO_HOME/bin/pio build --asm
$ $PIO_HOME/bin/pio train --variant engine-with-mongo.json
$ $PIO_HOME/bin/pio deploy --variant engine-with-mongo.json
```
* Use --asm for pio build for the 1st time to include dependency.

```
$ curl -H "Content-Type: application/json" -d '{ "user": 1, "num": 4 }' http://localhost:8000/queries.json
```
