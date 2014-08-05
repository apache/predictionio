package io.prediction.engines.stock2

import io.prediction.controller.LAlgorithm

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import io.prediction.controller.EmptyParams
import org.saddle._

import scala.reflect._
import scala.reflect.runtime.universe._

abstract class StockStrategy[M: ClassTag]
  extends LAlgorithm[
      EmptyParams, 
      TrainingData, 
      (TrainingData, M), 
      QueryDate, 
      AnyRef] {

  def train(trainingData: TrainingData): (TrainingData, M) = {
    (trainingData, createModel(trainingData.view))
  }

  def createModel(dataView: DataView): M


  def predict(dataModel: (TrainingData, M), queryDate: QueryDate): AnyRef = {
    val (trainingData, model) = dataModel

    val rawData = trainingData.rawDataB.value

    val dataView: DataView = 
      rawData.view(queryDate.idx, trainingData.maxWindowSize)

    /*
    val dataView: DataView = DataView(
      rawData, 
      queryDate.idx, 
      trainingData.maxWindowSize)
    */

    //val dataView = rawData.view

    val active = dataView.activeFrame().rowAt(0)

    //println(dataView)
    //println(active)

    val filtered = active.filter(identity)
    //println(filtered)
    
    
    //println(active.index)
    //println(filtered.index)

    val query = Query(
      idx = queryDate.idx, 
      dataView = dataView,
      tickers = Array[String](),
      mktTicker = "")

    onClose(model, query)
    None
  }

  def onClose(model: M, query: Query): Orders
}

class Strategy extends StockStrategy[AnyRef] {
  def createModel(dataView: DataView): AnyRef = {
    //val priceFrame = dataView.priceFrame(10)
    println(dataView.today)
    //println(dataView)
    println(dataView.priceFrame(10))
    println(dataView.retFrame(10))
    None
  }

  //def onClose(model: Anyref, query: Query, dataView: DataView): Orders = {
  def onClose(model: AnyRef, query: Query): Orders = {
    //println("onClose: " + query.dataView.today)
    Orders(0, Map[String, Double]())
  }
  
}

