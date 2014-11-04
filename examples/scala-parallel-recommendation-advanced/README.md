
Insert sample random ratings data into Mongo 'test' db
```
$ mongo data/insert_sample_ratings_mongo.js
```

engine-with-mongo.json is example engine which reads Mongo as DataSource.
```
$ $PIO_HOME/home/bin/pio build --asm
$ $PIO_HOME/home/bin/pio train --variant engine-with-mongo.json
$ $PIO_HOME/home/bin/pio deploy --variant engine-with-mongo.json
```
* Use --asm for pio build for the 1st time to include dependency.
