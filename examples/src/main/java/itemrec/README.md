# Mahout ItemRec Engine

* data/ - engine data
* algos/ - algo

## Compile and Run

Run GenericItemBased:

```
$ cd $PIO_HOME/examples
$ ../bin/pio-run io.prediction.examples.java.itemrec.Runner
```


By default, the sample data examples/ratings.csv is used. You may provide othere data source file.

## Use ml-100k Data Set

Download data set:

    $ curl http://files.grouplens.org/papers/ml-100k.zip -o ml-100k.zip
    $ unzip ml-100k.zip

**Note: because the algorithm will look at all other files in the same directory, the data source file directory must contain only valid data files.**

Since the ml-100k directory contain other files, copy the u.data for the reasons above:

    $ cp ml-100k/u.data  <your data source file directory>/


Run GenericItemBased with **\<your data source file directory\>/u.data**:

```
$ ../bin/pio-run io.prediction.examples.java.itemrec.Runner \
<your data source file directory>/u.data \
genericitembased
```

Run SVDPlusPlus with **\<your data source file directory\>/u.data**:

```
$ ../bin/pio-run io.prediction.examples.java.itemrec.Runner \
<your data source file directory>/u.data \
svdplusplus
```
