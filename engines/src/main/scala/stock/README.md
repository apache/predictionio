## How to implement a stock prediction algorithm

### Fetch data from external source
You only need to do it once. (Unless you want to update your data.)

We provide a script which extracts historical data of SP500 stocks from Yahoo and store the data
in your local data storage. Make sure you have setup storage according to
[storage setup instructions](/README.md). Feel free to substitute with your
favorite stock data source.

Specify environment. (Temporary.)
```
$ source conf/pio-env.sh
```

Run the fetch script.
```
$ sbt/sbt "engines/runMain io.prediction.engines.stock.FetchMain"
```
As SP500 constituents change all the time, the hardcoded list may not reflect
the current state and the script may fail to extract delisted tickers. Whilst
the stock engine is designed to accomodate missing / incomplete data, you may as
well update the ticker list in [stock engine
settings](/engines/src/main/scala/stock/Settings.scala).

### High level description
A stock prediction algorithms employs a *rolling window* estimation method. For a given window with `testingWindowSize`, the algorithm trains a model with data in the window, and then the model is evaluated with another set of data.

For example, it builds a model with data from Aug 31, 2012 to Aug 31, 2013, and evaluate the model with data from Sept 1, 2013 to Oct 1, 2013. And then, the training is rolled one month forward: model training with data from Sept 30, 2012 to Sept 30, 2013, and evaluation with data from Oct 1, 2013 to Nov 1, 2013, and so on.

Training Data | Testing Data
--------------|-------------
2012-08-31 -> 2013-08-31 | 2013-09-01 -> 2013-10-01
2012-09-30 -> 2013-09-30 | 2013-10-01 -> 2013-11-01
2012-10-31 -> 2013-10-31 | 2013-11-01 -> 2013-12-01
2012-11-30 -> 2013-11-30 | 2013-12-01 -> 2014-01-01
2012-12-31 -> 2013-12-31 | 2014-01-01 -> 2014-02-01
... | ...

For an algorithm developer, the task is to create a model with the training data, and then make prediction based on testing data.


### Training Data
```scala
class TrainingData(
  val tickers: Seq[String],
  val mktTicker: String,
  val timeIndex: Array[DateTime],
  val price: Array[(String, Array[Double])])
```
