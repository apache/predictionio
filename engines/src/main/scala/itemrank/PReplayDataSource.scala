package io.prediction.engines.itemrank

import io.prediction.controller._
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.joda.time.Instant
import org.json4s._
import org.json4s.native.JsonMethods._
import com.github.nscala_time.time.Imports._

class PReplayDataSource(val dsp: ReplayDataSourceParams)
  extends PDataSource[
      DataSourceParams,
      ReplaySliceParams,
      RDD[TrainingData],
      Query,
      Actual] {

  override
  def readBase(sc: SparkContext)
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
    require(inputData.count == 1, "Must reside in one partition")

    val preprocessed
    //: RDD[(Array[User], Array[Item], Map[LocalDate, Array[U2I]])] =
    : RDD[ReplayDataSource.PreprocessedData] = inputData.map(ds.preprocess)
    require(preprocessed.count == 1, "Must reside in one partition")

    // This is a terrible DataSource implementation, as it depends on
    // LDataSource. Indeed, it should a separate logic. First generate the
    // DataParams, then, based on the data-param, generate TrainingData and
    // QueryActualArray in parallel.
    val generated
    : RDD[((ReplaySliceParams, TrainingData, Array[(Query, Actual)]), Long)] = 
      preprocessed.flatMap(ds.generate).zipWithIndex
    generated.cache

    val dataParams: Seq[(ReplaySliceParams, Long)] = generated
      .map{ e => (e._1._1, e._2) }
      .collect

    dataParams.map { case(dp, index) => {
      val trainingData: RDD[TrainingData] = generated
        .filter(_._2 == index)
        .map(_._1._2)
      trainingData.cache

      val qaRdd: RDD[(Query, Actual)] = generated
        .filter(_._2 == index)
        .flatMap(_._1._3)
      // persist?
      qaRdd.cache

      (dp, trainingData, qaRdd)
    }}
    .toSeq
  }

  // of course I know what I am doing..
  def read(sc: SparkContext)
  : Seq[(ReplaySliceParams, RDD[TrainingData], RDD[(Query, Actual)])] = 
    Seq[(ReplaySliceParams, RDD[TrainingData], RDD[(Query, Actual)])]()

}


