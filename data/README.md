## Data Collection API

Please refer to the documentation site. (TODO: add link)

## For Development:

### Run sample.ItemRankDataSource

Start Data API server
```
$ bin/pio dataapi
```

Imoprt test data
```
$ python sdk/python-sdk/itemrec_example.py --appid <appid>
```

Run Sample ItemRankDataSource:
```
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.sample.ItemRankDataSource <appid>"
```
