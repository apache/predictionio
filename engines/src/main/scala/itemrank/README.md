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
$ pushd sdk/python-sdk/
$ python -m itemrank_import.py --app_id <appid>
$ popd
```
Update the appId in params/datasource.json with <appid>

```
$ pushd engines/src/main/scala/itemrank/examples
$ ../../../../../../bin/pio register
$ ../../../../../../bin/pio train -ap mahoutItemBasedAlgo.json
$ ../../../../../../bin/pio deploy
$ popd
```

By default, the server is deployed at http://localhost:8000/.

Train with other algorithms:
```
$ ../../../../../../bin/pio train -ap randAlgo.json
$ ../../../../../../bin/pio train -ap featurebasedAlgo.json
$ ../../../../../../bin/pio train -ap ncMahoutItemBasedAlgo.json
```


Retrieve prediction with command line:

```
$ curl -i -X POST http://localhost:8000/ \
-d '{ "uid" : "u2", "iids" : ["i0", "i1", "i2", "i3"] }'
```

Retrieve prediction with python sdk:
```
$ pushd sdk/python-sdk/
$ python -m itemrank_query --url http://localhost:8000
{u'items': [{u'_2': 5.698571227664496, u'_1': u'i0'}, {u'_2':
3.2369502553192753, u'_1': u'i2'}, {u'_2': 2.4277126914894565, u'_1': u'i1'},
{u'_2': 1.6184751276596376, u'_1': u'i3'}], u'isOriginal': False}
...
$ popd
```

Run Runner (obsolete)
==============
```
$ pushd engines
$ ../bin/pio-run  io.prediction.engines.itemrank.DetailedRunner
$ popd engines

$ ../bin/pio-run  io.prediction.engines.itemrank.Runner
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
