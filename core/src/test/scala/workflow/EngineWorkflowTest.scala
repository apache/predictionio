package n.io.prediction.workflow

//import org.specs2.mutable._

//import org.apache.spark.SharedSparkContext
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
    
    def batchPredict(m: NAlgo0.Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }
  }

  object NAlgo1 {
    case class Model(id: Int, pd: ProcessedData)
  }

  class NAlgo1 (id: Int = 0)
  extends P2LAlgorithm[ProcessedData, NAlgo1.Model, Query, Prediction] {
    def train(pd: ProcessedData): NAlgo1.Model = NAlgo1.Model(id, pd)
    
    def batchPredict(m: NAlgo1.Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }
  }
  
  class LServing0(id: Int = 0) extends LServing[Query, Prediction] {
    def serve(q: Query, ps: Seq[Prediction]): Prediction = {
      Prediction(id, q, ps=ps)
    }
  }
}

class EngineWorkflowSuite extends FunSuite with SharedSparkContext {
  test("Simple") {
    val rdd: RDD[Int] = sc.parallelize(Seq(1,2,3))
    assert(rdd.collect === Array(1,2,3))
    assert(rdd.collect === Array(1,2,3))
  }
}

class EngineWorkflowTrainSuite extends FunSuite with SharedSparkContext {
  import n.io.prediction.workflow.Engine0._
  test("Parallel DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new PDataSource0(0),
      new PPreparator0(1),
      Seq(
        new PAlgo0(2),
        new PAlgo1(3),
        new PAlgo0(4)))

    val pd = ProcessedData(1, TrainingData(0))

    models should contain theSameElementsAs Seq(
      PAlgo0.Model(2, pd), PAlgo1.Model(3, pd), PAlgo0.Model(4, pd))
  }

  test("Local DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new LDataSource0(0),
      new LPreparator0(1),
      Seq(
        new LAlgo0(2),
        new LAlgo1(3),
        new LAlgo0(4)))
    
    val pd = ProcessedData(1, TrainingData(0))

    val expectedResults = Seq(
      LAlgo0.Model(2, pd),
      LAlgo1.Model(3, pd),
      LAlgo0.Model(4, pd))

    forAll(models.zip(expectedResults)) { case (model, expected) => 
      model shouldBe a [RDD[_]]
      val localModel = model.asInstanceOf[RDD[_]].collect
      localModel should contain theSameElementsAs Seq(expected)
    }
  }

  test("P2L DS/P/Algos") {
    val models = EngineWorkflow.train(
      sc,
      new PDataSource0(0),
      new PPreparator0(1),
      Seq(
        new NAlgo0(2),
        new NAlgo1(3),
        new NAlgo0(4)))

    val pd = ProcessedData(1, TrainingData(0))
    
    models should contain theSameElementsAs Seq(
      NAlgo0.Model(2, pd), NAlgo1.Model(3, pd), NAlgo0.Model(4, pd))
  }
}


class EngineWorkflowEvalDevSuite
extends FunSuite with Inside with SharedSparkContext {
  import n.io.prediction.workflow.Engine0._

  @transient lazy val logger = Logger[this.type] 
  
  test("Simple Parallel DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new PDataSource1(id = 1, en = en, qn = qn),
      new PPreparator0(id = 2),
      Seq(new PAlgo0(id = 3)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = PAlgo0.Model(3, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 1
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)))
        }}
      }

    }}

  }

  test("Parallel DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new PDataSource1(id = 1, en = en, qn = qn),
      new PPreparator0(id = 2),
      Seq(
        new PAlgo0(id = 3), 
        new PAlgo1(id = 4),
        new NAlgo1(id = 5)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = PAlgo0.Model(3, pd)
    val model1 = PAlgo1.Model(4, pd)
    val model2 = NAlgo1.Model(5, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 3
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)),
            Prediction(id = 4, q = q, models = Some(model1)),
            Prediction(id = 5, q = q, models = Some(model2))
          )
        }}
      }
    }}
  }
  
  test("Local DS/P/A/S") {
    val en = 2
    val qn = 5

    val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
    EngineWorkflow.eval(
      sc,
      new LDataSource0(id = 1, en = en, qn = qn),
      new LPreparator0(id = 2),
      Seq(
        new LAlgo0(id = 3), 
        new LAlgo1(id = 4),
        new LAlgo1(id = 5)),
      new LServing0(id = 10))

    val pd = ProcessedData(2, TrainingData(1))
    val model0 = LAlgo0.Model(3, pd)
    val model1 = LAlgo1.Model(4, pd)
    val model2 = LAlgo1.Model(5, pd)

    forAll(evalDataSet.zipWithIndex) { case (evalData, ex) => {
      val (evalInfo, qpaRDD) = evalData
      evalInfo shouldBe EvalInfo(1)

      val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
      forAll (qpaSeq) { case (q, p, a) => 
        val Query(qId, qEx, qQx) = q
        val Actual(aId, aEx, aQx) = a
        qId shouldBe aId
        qEx shouldBe ex
        aEx shouldBe ex
        qQx shouldBe aQx

        inside (p) { case Prediction(pId, pQ, pModels, pPs) => {
          pId shouldBe 10
          pQ shouldBe q
          pModels shouldBe None
          pPs should have size 3
          pPs shouldBe Seq(
            Prediction(id = 3, q = q, models = Some(model0)),
            Prediction(id = 4, q = q, models = Some(model1)),
            Prediction(id = 5, q = q, models = Some(model2))
          )
        }}
      }

    }}

  }
}

