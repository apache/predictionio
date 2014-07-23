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

### High Level Description
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

### Key Data Structures

#### Training Data
[Training data](Data.scala) has 4 main fields. `tickers` is the list of tickers that is used for training. `mktTicker` is the ticker of market, for calculate beta related metrics. `timeIndex` is a list of dates representing the time window used for training. `price` is a map from ticker to array of stock price, the array is of the same length as `timeIndex`. Notice that only tickers that are active through out the whole time window is included, i.e. for example, if the `TrainingData`'s time window is from 2012-01-01 to 2013-01-01, Facebook (FB) will not be included in the training data.

```scala
class TrainingData(
  val tickers: Seq[String],
  val mktTicker: String,
  val timeIndex: Array[DateTime],
  val price: Array[(String, Array[Double])])
```

#### Query
[Query](Data.scala) is the input for prediction. Most of the fields resembles `TrainingData`, with an additional field `tomorrow` indicating the date of the prediction output. Algorithm builders take this as input, together will the trained model created with `TrainingData`, and make prediction.

```scala
class Query(
  val mktTicker: String,
  val tickerList: Seq[String],
  val timeIndex: Array[DateTime],
  val price: Array[(String, Array[Double])],
  val tomorrow: DateTime)
```

#### Target
[Target](Data.scala) is the output for prediction. It is essentially a map from ticker to predicted return. Notice that the prediction need not to match `Query`, if an algorithm cannot make prediction some symbols, it can just leave them out.
