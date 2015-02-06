package n.io.prediction.controller

import org.scalatest.FunSuite
import org.scalatest.Inside
import org.scalatest.Matchers._
import org.scalatest.Inspectors._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import n.io.prediction.controller._
import n.io.prediction.core._
import grizzled.slf4j.{ Logger, Logging }

import java.lang.Thread

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite

import io.prediction.controller.{ Params => PIOParams }
import io.prediction.controller.EngineParams

object Engine0 {
  @transient lazy val logger = Logger[this.type] 

  case class TrainingData(id: Int)
  case class EvalInfo(id: Int)
  case class ProcessedData(id: Int, td: TrainingData)

  case class Query(id: Int, ex: Int = 0, qx: Int = 0)
  case class Actual(id: Int, ex: Int = 0, qx: Int = 0)
  case class Prediction(
    id: Int, q: Query, models: Option[Any] = None, 
    ps: Seq[Prediction] = Seq[Prediction]())

  class PDataSource0(id: Int = 0) 
  extends PDataSource[TrainingData, EvalInfo, Query, Actual] {
    def readTrain(sc: SparkContext): TrainingData = {
      TrainingData(id)
    }
  }
  
  class PDataSource1(id: Int = 0, en: Int = 0, qn: Int = 0)
  extends PDataSource[TrainingData, EvalInfo, Query, Actual] {
    def readTrain(sc: SparkContext): TrainingData = TrainingData(id)
    
    override
    def readEval(sc: SparkContext)
    : Seq[(TrainingData, EvalInfo, RDD[(Query, Actual)])] = {
      (0 until en).map { ex => {
        val qaSeq: Seq[(Query, Actual)] = (0 until qn).map { qx => {
          (Query(id, ex=ex, qx=qx), Actual(id, ex, qx))
        }}
        (TrainingData(id), EvalInfo(id), sc.parallelize(qaSeq))
      }}
    }
  }

  object PDataSource2 {
    case class Params(id: Int, en: Int = 0, qn: Int = 0) extends PIOParams
  }
  
  class PDataSource2(params: PDataSource2.Params)
  extends PDataSource[TrainingData, EvalInfo, Query, Actual] {
    val id = params.id
    def readTrain(sc: SparkContext): TrainingData = TrainingData(id)
    
    override
    def readEval(sc: SparkContext)
    : Seq[(TrainingData, EvalInfo, RDD[(Query, Actual)])] = {
      (0 until params.en).map { ex => {
        val qaSeq: Seq[(Query, Actual)] = (0 until params.qn).map { qx => {
          (Query(id, ex=ex, qx=qx), Actual(id, ex, qx))
        }}
        (TrainingData(id), EvalInfo(id), sc.parallelize(qaSeq))
      }}
    }
  }
  
  class LDataSource0(id: Int, en: Int = 0, qn: Int = 0) 
    extends LDataSource[TrainingData, EvalInfo, Query, Actual] {
    def readTrain(): TrainingData = TrainingData(id)
    
    def readEval()
    : Seq[(TrainingData, EvalInfo, Seq[(Query, Actual)])] = {
      (0 until en).map { ex => {
        val qaSeq: Seq[(Query, Actual)] = (0 until qn).map { qx => {
          (Query(id, ex=ex, qx=qx), Actual(id, ex, qx))
        }}
        (TrainingData(id), EvalInfo(id), qaSeq)
      }}
    }
  }
  
  class PPreparator0(id: Int = 0)
  extends PPreparator[TrainingData, ProcessedData] {
    def prepare(sc: SparkContext, td: TrainingData): ProcessedData = {
      ProcessedData(id, td)
    }
  }

  object PPreparator1 {
    case class Params(id: Int  = 0) extends PIOParams
  }

  class PPreparator1(params: PPreparator1.Params)
  extends PPreparator[TrainingData, ProcessedData] {
    def prepare(sc: SparkContext, td: TrainingData): ProcessedData = {
      ProcessedData(params.id, td)
    }
  }

  class LPreparator0(id: Int = 0) 
  extends LPreparator[TrainingData, ProcessedData] {
    def prepare(td: TrainingData): ProcessedData = {
      ProcessedData(id, td)
    }
  }

  object PAlgo0 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class PAlgo0(id: Int = 0)
  extends PAlgorithm[ProcessedData, PAlgo0.Model, Query, Prediction] {
    def train(pd: ProcessedData): PAlgo0.Model = PAlgo0.Model(id, pd)

    def batchPredict(m: PAlgo0.Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }
    
    def predict(m: PAlgo0.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }

  object PAlgo1 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class PAlgo1(id: Int = 0)
  extends PAlgorithm[ProcessedData, PAlgo1.Model, Query, Prediction] {
    def train(pd: ProcessedData): PAlgo1.Model = PAlgo1.Model(id, pd)

    def batchPredict(m: PAlgo1.Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }

    def predict(m: PAlgo1.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }
  
  object PAlgo2 {
    case class Model(id: Int, pd: ProcessedData)
    case class Params(id: Int) extends PIOParams
  }

  class PAlgo2(params: PAlgo2.Params)
  extends PAlgorithm[ProcessedData, PAlgo2.Model, Query, Prediction] {
    val id = params.id

    def train(pd: ProcessedData): PAlgo2.Model = PAlgo2.Model(id, pd)

    def batchPredict(m: PAlgo2.Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }

    def predict(m: PAlgo2.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }
  
  object LAlgo0 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class LAlgo0(id: Int = 0) 
  extends LAlgorithm[ProcessedData, LAlgo0.Model, Query, Prediction] {
    def train(pd: ProcessedData): LAlgo0.Model = LAlgo0.Model(id, pd)

    def predict(m: LAlgo0.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }
  
  object LAlgo1 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class LAlgo1(id: Int = 0) 
  extends LAlgorithm[ProcessedData, LAlgo1.Model, Query, Prediction] {
    def train(pd: ProcessedData): LAlgo1.Model = LAlgo1.Model(id, pd)
    
    def predict(m: LAlgo1.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }

  // N : P2L. As N is in the middle of P and L.
  object NAlgo0 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class NAlgo0 (id: Int = 0)
  extends P2LAlgorithm[ProcessedData, NAlgo0.Model, Query, Prediction] {
    def train(pd: ProcessedData): NAlgo0.Model = NAlgo0.Model(id, pd)
  
    def predict(m: NAlgo0.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }

  object NAlgo1 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class NAlgo1 (id: Int = 0)
  extends P2LAlgorithm[ProcessedData, NAlgo1.Model, Query, Prediction] {
    def train(pd: ProcessedData): NAlgo1.Model = NAlgo1.Model(id, pd)
   
    def predict(m: NAlgo1.Model, q: Query): Prediction = {
      Prediction(id, q, Some(m))
    }
  }
  
  class LServing0(id: Int = 0) extends LServing[Query, Prediction] {
    def serve(q: Query, ps: Seq[Prediction]): Prediction = {
      Prediction(id, q, ps=ps)
    }
  }

  object LServing1 {
    case class Params(id: Int) extends PIOParams
  }
  
  class LServing1(params: LServing1.Params) extends LServing[Query, Prediction] {
    def serve(q: Query, ps: Seq[Prediction]): Prediction = {
      Prediction(params.id, q, ps=ps)
    }
  }
}
