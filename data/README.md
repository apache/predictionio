## Data Collection API

Please refer to the documentation site. (TODO: add link)

## For Development Use only:

### Start Data API without bin/pio

```
$ sbt/sbt "data/compile"
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.api.Run"
```

Very simple test

```
$ data/test.sh <appId>
```

Experimental upgrade tool (Upgrade HBase schema from 0.8.0/0.8.1 to 0.8.2)
Create an app to store the data
```
$ bin/pio app new <my app>
```

Replace <to app ID> by the returned app ID:
(<from app ID> is the original app ID used in 0.8.0/0.8.2.)

```
$ set -a
$ source conf/pio-env.sh
$ set +a
$ sbt/sbt "data/run-main io.prediction.data.storage.hbase.upgrade.Upgrade <from app ID>" "<to app ID>"

```
