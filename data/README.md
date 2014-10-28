## Data Collection API

Please refer to the documentation site. (TODO: add link)

## For Development:

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
$ data/test.sh
```
