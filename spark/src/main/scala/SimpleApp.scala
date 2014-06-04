import io.prediction.engines.stock.LocalFileStockEvaluator
import io.prediction.engines.stock.StockEvaluator
import io.prediction.engines.stock.EvaluationDataParams
import io.prediction.engines.stock.StockEngine

import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf


object SimpleApp {
  def simple() {
    val logFile = "/Users/yipjustin/data/spark/README.md" 
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val logData = sc.textFile(logFile, 2).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))

  }

  def main(args: Array[String]) {
    val s = StockEvaluator()
    val e = StockEngine()

    val randomAlgo = e.algorithmClassMap("random").newInstance

    //val logFile = "YOUR_SPARK_HOME/README.md" // Should be some file on your system


    //val logFile = "/Users/yipjustin/data/spark/README.md" 
    val conf = new SparkConf().setAppName("PredictionIO")
    val sc = new SparkContext(conf)

    /*
    val logData = sc.textFile(logFile, 2).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))
    */
    //val s = LocalFileStockEvaluator()
    //val s = StockEvaluator()


    val tickerList = Seq("GOOG", "AAPL", "FB", "GOOGL", "MSFT")
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      //untilIdx = 630,
      untilIdx = 1200,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)
    println(evalDataParams) 

    val dataPrep = s.dataPreparatorClass.newInstance

    val localParamsSet = dataPrep.getParamsSetBase(evalDataParams)

    val sparkParamsSet = sc.parallelize(localParamsSet)

    val sparkTrainingSet = sparkParamsSet.map(_._1).map(dataPrep.prepareTrainingBase)

    println("spark training set")
    println(sparkTrainingSet.first)

    val sparkModel = sparkTrainingSet.map(randomAlgo.trainBase)

    println("spark model")

    sparkModel.foreach(println)

    //println(sparkModel.first)
    val localModel = sparkModel.collect
    //val localModel = sparkModel.take(3)

    localModel.foreach(println)
    //sparkModel.persist

    //sparkModel.saveAsObjectFile("/tmp/pio/obj")
   
  }
}
