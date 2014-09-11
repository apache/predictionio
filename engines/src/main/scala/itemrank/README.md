ItemRank engine
===============

## Use case

Personalize the order of a list of items for each user.

## Prediction Input

- user id
- list of items to be ranked

## Prediction Output

- ranked items with score

## Events requirement

- user entity
- item entity, with following properties:
	- itypes: array of String
	- starttime: ISO 8601
	- endtime: ISO 8601
	- inactive: boolean
- user-to-item action event, optionally with following properties:
 	- rating: integer rating

## Example Run

Import sample events
```
$ python sdk/python-sdk/itemrec_example.py --appid <appid>
```

```
$ cd engines/src/main/scala/itemrank/examples
```

Update the appId in params/datasource.json with <appid>

```
$ ../../../../../../bin/pio register
$ ../../../../../../bin/pio train -ap mahoutalgo.json
$ ../../../../../../bin/pio deploy
```

Retrieve prediction:

```
$ curl -i -X POST http://localhost:8000/ \
-d '{ "uid" : "u2", "items" : ["i0", "i1", "i2", "i3"] }'
```

Run Evaluation
==============
```
../bin/pio-run  io.prediction.engines.itemrank.DetailedRunner

../bin/pio-run  io.prediction.engines.itemrank.Runner
```

Import Sample Data (obsolete)
==================
Run under examples.
```
examples$ set -a
examples$ source ../conf/pio-env.sh
examples$ set +a

examples$ ../sbt/sbt "runMain io.prediction.engines.itemrank.CreateSampleData"
```
