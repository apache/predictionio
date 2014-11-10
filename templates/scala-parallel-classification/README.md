

Get sample data from MLlib repo and store inside data/ directory

```
$ curl https://raw.githubusercontent.com/apache/spark/master/data/mllib/sample_naive_bayes_data.txt --create-dirs -o data/sample_naive_bayes_data.txt
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
