package io.prediction.engines.itemrank

import io.prediction.controller._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.joda.time.Instant
import org.json4s._
import org.json4s.native.JsonMethods._
import com.github.nscala_time.time.Imports._

class PReplayDataSource(val dsp: ReplayDataSource.Params)
  extends PDataSource[
      ReplayDataSource.Params,
      ReplaySliceParams,
      RDD[TrainingData],
      Query,
      Actual] {

  def read(sc: SparkContext)
  : Seq[(ReplaySliceParams, RDD[TrainingData], RDD[(Query, Actual)])] = {
    implicit val formats = DefaultFormats
    
    val ds = new ReplayDataSource(dsp)

    val u2iList: RDD[Array[U2I]] = sc
      .textFile(path = dsp.u2iPath)
      .map { s => 
        implicit val formats = DefaultFormats 
        parse(s).extract[U2I] 
      }
      .coalesce(numPartitions = 1)
      .glom
    require(u2iList.partitions.size == 1, "Must resides in one partition")

    val userList: RDD[Array[User]] = sc
      .textFile(path = dsp.userPath)
      .map { s => 
        implicit val formats = DefaultFormats 
        parse(s).extract[User] 
      }
      .coalesce(numPartitions = 1)
      .glom
    require(userList.partitions.size == 1, "Must resides in one partition")

    val itemList: RDD[Array[Item]] = sc
      .textFile(path = dsp.itemPath)
      .map { s => 
        implicit val formats = DefaultFormats 
        parse(s).extract[Item] 
      }
      .coalesce(numPartitions = 1)
      .glom
    require(itemList.partitions.size == 1, "Must resides in one partition")

    val inputData: RDD[(Array[User], Array[Item], Array[U2I])] =
      userList.zip(itemList).zip(u2iList).map { e => (e._1._1, e._1._2, e._2) }

    val preprocessed
    : RDD[ReplayDataSource.PreprocessedData] = inputData.map(ds.preprocess)
    preprocessed.cache
    require(preprocessed.count == 1, "Must reside in one partition")


    val dataParams: Seq[ReplaySliceParams] = ds.generateParams

    dataParams.map { dp =>
      val sliceData = preprocessed.map(p => ds.generateOne((p, dp)))
      
      val trainingData: RDD[TrainingData] = sliceData.map(_._2)
      val qaRdd: RDD[(Query, Actual)] = sliceData.flatMap(_._3)
      trainingData.cache
      qaRdd.cache
      (dp, trainingData, qaRdd)
    }
    .toSeq
  }
}


