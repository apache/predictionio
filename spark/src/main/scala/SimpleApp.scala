import io.prediction.engines.stock.LocalFileStockEvaluator
import io.prediction.engines.stock.StockEvaluator
import io.prediction.engines.stock.EvaluationDataParams
import io.prediction.engines.stock.RandomAlgoParams
import io.prediction.engines.stock.StockEngine

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine


import io.prediction.core.AbstractDataPreparator

import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream


object WS {
  val tmpDir = "/tmp/pio/"

  def filename(id: Int): String = s"${tmpDir}${id}.data"

  def loadData[A](filePath: String): A = {
    val ois = new ObjectInputStream(new FileInputStream(filePath))
    val obj = ois.readObject
    //obj.asInstanceOf[MeatLocker[BasePersistentData]].get
    obj.asInstanceOf[A]
  }

  def saveData[A](id: Int, data: A): String = {
  //def saveData(id: Int, data: Object): String = {
    //val boxedData = MeatLocker(data)
    val filePath = filename(id)
    val oos = new ObjectOutputStream(new FileOutputStream(filePath))
    oos.writeObject(data)
    filePath
  }
}

object SimpleApp {
  def simple() {
    val logFile = "/home/yipjustin/client/spark/spark-1.0.0-bin-hadoop2/README.md" 
    val conf = new SparkConf().setAppName("Simple Application")
    val sc = new SparkContext(conf)
    val logData = sc.textFile(logFile, 2).cache()
    val numAs = logData.filter(line => line.contains("a")).count()
    val numBs = logData.filter(line => line.contains("b")).count()
    println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))

  }

  def test() {
    val s = StockEvaluator()
    val dataPrep = s.dataPreparatorClass.newInstance

    val fn = WS.saveData(42, dataPrep)

    val dp = WS.loadData[AbstractDataPreparator](fn) 
    
  }



  def main(args: Array[String]) {
    //test
    //return

    val s = StockEvaluator()
    //val e = StockEngine.get
    val e = StockEngine()
    
    val tickerList = Seq("GOOG", "AAPL", "FB", "GOOGL", "MSFT")
    val evalDataParams = new EvaluationDataParams(
      baseDate = new DateTime(2006, 1, 1, 0, 0),
      fromIdx = 600,
      untilIdx = 630,
      //untilIdx = 1200,
      trainingWindowSize = 600,
      evaluationInterval = 20,
      marketTicker = "SPY",
      tickerList = tickerList)
    println(evalDataParams) 

    val randomAlgoParams = new RandomAlgoParams(seed = 1, scale = 0.01)

    SparkWorkflow.run("Fizz", 
      evalDataParams, 
      //null,  /* algo params */
      randomAlgoParams,
      s, 
      e)
    return
    
    

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




    val dataPrep = s.dataPreparatorClass.newInstance

    val localParamsSet = dataPrep.getParamsSetBase(evalDataParams)

    val sparkParamsSet = sc.parallelize(localParamsSet)

    sparkParamsSet.foreach(println)

    val sparkTrainingSet = sparkParamsSet.map(_._1).map(dataPrep.prepareTrainingBase)

    println("spark training set")
    println(sparkTrainingSet.first)
    sparkTrainingSet.foreach(println)


    //randomAlgo.initBase(randomAlgoParams)
    //val sparkModel = sparkTrainingSet.map(randomAlgo.trainBase)

    //println("spark model")

    //sparkModel.foreach(println)

    //println(sparkModel.first)
    //val localModel = sparkModel.collect
    //val localModel = sparkModel.take(3)

    //localModel.foreach(println)
    //sparkModel.persist

    //sparkModel.saveAsObjectFile("/tmp/pio/obj")
   
  }
}
