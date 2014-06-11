import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf


import io.prediction.engines.stock.LocalFileStockEvaluator
import io.prediction.engines.stock.StockEvaluator
import io.prediction.engines.stock.EvaluationDataParams
import io.prediction.engines.stock.RandomAlgoParams
import io.prediction.engines.stock.StockEngine
import io.prediction.engines.stock.Feature

import io.prediction.engines.itemrank._

import io.prediction.core.BaseEvaluator
import io.prediction.core.BaseEngine


import io.prediction.core.AbstractDataPreparator

import com.github.nscala_time.time.Imports.DateTime

import org.apache.spark.serializer.KryoRegistrator
import org.apache.spark.serializer.KryoSerializer

import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.FileInputStream
import java.io.ObjectInputStream

import io.prediction.workflow.SparkWorkflow

//import org.saddle.Series
import org.saddle._

import com.twitter.chill.MeatLocker

//import com.twitter.summingbird.online.Externalizer
import com.twitter.chill.Externalizer
import com.esotericsoftware.kryo.Kryo

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

class R extends KryoRegistrator {
  override def registerClasses(kryo: Kryo) {
    println("XXXXXXXXXX")
    //kryo.register(classOf[Series[Int, Int]])
    //kryo.register(classOf[org.saddle.Series[_, _]])
    kryo.register(classOf[X])
  }
}

/*
class X(val x : Int) {}
class Y(val data: MeatLocker[Frame[Int, String, Int]]) extends Serializable {}
*/

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
    val conf = new SparkConf().setAppName("PredictionIO")
    //conf.set("spark.serializer", classOf[KryoSerializer].getName)
    /*
    conf.set("spark.serializer", 
      "org.apache.spark.serializer.KryoSerializer")
    conf.set("spark.kryo.registrator", classOf[R].getName)
    */

    val sc = new SparkContext(conf)
    //sc.addJar("/home/yipjustin/.ivy2/cache/org.scala-saddle/saddle-core_2.10/jars/saddle-core_2.10-1.3.2.jar")

    val d = sc.parallelize(Array(1,2,3))                                          

    //val e = d.map(i => Series(i)).map(e => Externalizer[Series[Int, Int]](e))  
    val e = d.map(i => Series(i)).map(s => {
      val f = Frame("a" -> s)
      new Y(data = MeatLocker(f))
    })

    e.collect.foreach(e => println(e.data.get))

    val f = e.zipWithIndex.map(_.swap)
    
    val g = d.zipWithIndex.map(_.swap)

    val h = f.join(g)

    h.collect.foreach{ case(k, v) => {
      println(k + " : " + v._1 + " " + v._2)
    }}
    
    /*
    val s = StockEvaluator()
    val dataPrep = s.dataPreparatorClass.newInstance

    val fn = WS.saveData(42, dataPrep)

    val dp = WS.loadData[AbstractDataPreparator](fn) 
    */
    //val f: Series[Int, Int] = Series(1,2,3,4,5,6)
    
  }

  def stock() {
    val s = StockEvaluator()
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
    
    //SparkWorkflow.run("Fizz", 
    //  evalDataParams, 
    //  null, /* validation params */
    //  null,  /* algo params */
    //  e,s)
  }

  def itemrank() {
    val s = ItemRankEvaluator()
    val e = ItemRankEngine()
    val evalParams = new EvalParams(
      appid = 1,
      itypes = None,
      actions = Map(
        "view" -> Some(3),
        "like" -> Some(5),
        "conversion" -> Some(4),
        "rate" -> None
      ),
      conflict = "latest",
      //recommendationTime = 123456,
      seenActions = Some(Set("conversion")),
      //(int years, int months, int weeks, int days, int hours,
      // int minutes, int seconds, int millis)
      hours = 24,//new Period(0, 0, 0, 1, 0, 0, 0, 0),
      trainStart = new DateTime("2014-04-01T00:00:00.000"),
      testStart = new DateTime("2014-04-20T00:00:00.000"),
      testUntil = new DateTime("2014-04-30T00:00:00.000"),
      goal = Set("conversion", "view")
    ) 
    val knnAlgoParams = new KNNAlgoParams(similarity="consine")

    //SparkWorkflow.run("Fizz", 
    //  //evalDataParams, 
    //  evalParams, 
    //  evalParams, /* validation params */
    //  knnAlgoParams,
    //  e,s)
    return
  }


  def main(args: Array[String]) {
    //itemrank
    stock
    //test
    //return


    
    
   
  }
}
