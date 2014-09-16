
## Quick Start

```
$ python -m examples.itemrank_quickstart_import
```


## MovieLens-100k

1. Get ml-100k data set
```
$ wget http://files.grouplens.org/datasets/movielens/ml-100k.zip
$ unzip ml-100k.zip
```

2. Modify app_id in examples/demo-movielens/app_config.py

3. Import data:
```
$ python -m examples.demo-movielens.batch_import
```
