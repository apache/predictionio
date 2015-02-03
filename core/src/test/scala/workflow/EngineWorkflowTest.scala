package n.io.prediction.workflow

import org.specs2.mutable._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

import n.io.prediction.controller._
import n.io.prediction.core._
import grizzled.slf4j.{ Logger, Logging }

trait SparkSpec extends Specification {
  System.clearProperty("spark.driver.port")
  System.clearProperty("spark.hostPort")
  lazy val sc = new SparkContext("local[4]", "PIO SparkSpec")
}


object Engine0 {
  case class TrainingData(id: Int)
  case class EvalInfo(id: Int)
  case class ProcessedData(id: Int, td: TrainingData)
  case class PAlgo0Model(id: Int, pd: ProcessedData)
  case class PAlgo1Model(id: Int, pd: ProcessedData)

  case class Query(id: Int, ex: Int = 0, qx: Int = 0)
  case class Actual(id: Int, ex: Int = 0, qx: Int = 0)
  case class Prediction(
    id: Int, q: Query, models: Option[Any] = None, 
    ps: Seq[Prediction] = Seq[Prediction]())

  class PDataSource0(id: Int = 0) 
  extends PDataSource[TrainingData, EvalInfo, Query, Actual] {
    def readTrain(sc: SparkContext): TrainingData = TrainingData(id)
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
  
  class PPreparator0(id: Int = 0) extends PPreparator[TrainingData, ProcessedData] {
    def prepare(sc: SparkContext, td: TrainingData): ProcessedData = {
      ProcessedData(id, td)
    }
  }

  class PAlgo0(id: Int = 0)
  extends PAlgorithm[ProcessedData, PAlgo0Model, Query, Prediction] {
    def train(pd: ProcessedData): PAlgo0Model = PAlgo0Model(id, pd)

    def batchPredict(m: PAlgo0Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }
  }

  class PAlgo1(id: Int = 0)
  extends PAlgorithm[ProcessedData, PAlgo1Model, Query, Prediction] {
    def train(pd: ProcessedData): PAlgo1Model = PAlgo1Model(id, pd)

    def batchPredict(m: PAlgo1Model, qs: RDD[(Long, Query)])
    : RDD[(Long, Prediction)] = {
      qs.mapValues(q => Prediction(id, q, Some(m)))
    }
    
  }
  
  class LServing0(id: Int = 0) extends LServing[Query, Prediction] {
    def serve(q: Query, ps: Seq[Prediction]): Prediction = {
      Prediction(id, q, ps=ps)
    }
  }

  /*
  case class Query(z: Int)
  case class Actual(z: Int)
  */


  /*
  class PDataSource0(k: Int) 
    extends PDataSource[String, String, String, String] {
    def readTrain(sc: SparkContext): String = s"PD0($k)"
  }
  
  class PDataSource1(k: Int, e: Int, qan: Int) 
    extends PDataSource[String, String, String, String] {
    def readTrain(sc: SparkContext): String = s"PD1($k)"

    override
    def readEval(sc: SparkContext)
    : Seq[(String, String, RDD[(String, String)])] = {
      (0 until e).map { ei => {
        val qaSeq: Seq[(String, String)] = (0 until qan).map { qai => {
          (s"PD1(Q($ei,$qai))", s"PD1(A($ei,$qai))")
        }}
        (s"PD1($ei,$k)", s"PD1(E($ei,$k))", sc.parallelize(qaSeq))
      }}
    }
  }
  
  class LDataSource0(k: Int) 
    extends LDataSource[String, String, String, String] {
    def readTrain(): String = s"LD0($k)"
  }

  class PPreparator0 extends PPreparator[String, String] {
    def prepare(sc: SparkContext, td: String): String = s"PP0($td)"
  }

  class LPreparator0 extends LPreparator[String, String] {
    def prepare(td: String): String = s"LP0($td)"
  }
    
  class PAlgo0 extends PAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"PA0($pd)"

    def batchPredict(m: String, qs: RDD[(Long, String)])
    : RDD[(Long, String)] = {
      qs.mapValues(q => s"PA0(M($m),Q($q)")
    }
  }

  class PAlgo1 extends PAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"PA1($pd)"

    def batchPredict(m: String, qs: RDD[(Long, String)])
    : RDD[(Long, String)] = {
      qs.mapValues(q => s"PA1(M($m),Q($q)")
    }
  }
  */
 
  /*
  class LAlgo0 extends LAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"LA0($pd)"
  }

  class LAlgo1 extends LAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"LA1($pd)"
  }
 
  // N : P2L. As N is in the middle of P and L.
  class NAlgo0 extends P2LAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"NA0($pd)"
  }

  class NAlgo1 extends P2LAlgorithm[String, String, String, String] {
    def train(pd: String): String = s"NA1($pd)"
  }

  class LServing0 extends LServing[String, String] {
    def serve(q: String, ps: Seq[String]): String = {
      ps.mkString("LS0(", ",", ")")
    }
  }
  */
}


//class EngineWorkflowSpecDev extends Specification {
class EngineWorkflowSpecDev extends SparkSpec {
  import n.io.prediction.workflow.Engine0._

  //System.clearProperty("spark.driver.port")
  //System.clearProperty("spark.hostPort")
  //val sc = SparkContextSetup.sc
  //val sc = new SparkContext("local[4]", "BaseEngineSpec test")
  @transient lazy val logger = Logger[this.type] 

  "EngineWorkflowSpec.train" should {

    "Parallel DS/P/Algos" in {
      val models = EngineWorkflow.train(
        sc,
        new Engine0.PDataSource0(0),
        new Engine0.PPreparator0(1),
        Seq(
          new Engine0.PAlgo0(2),
          new Engine0.PAlgo1(3),
          new Engine0.PAlgo0(4)))

      val pd = ProcessedData(1, TrainingData(0))

      models must beEqualTo(
        Seq(PAlgo0Model(2, pd), PAlgo1Model(3, pd), PAlgo0Model(4, pd)))
    }





    "XX" in {
      val a = ProcessedData(0, TrainingData(1))
      val b = ProcessedData(0, TrainingData(1))
      val c = ProcessedData(0, TrainingData(2))
      
      a === b
      a !== c
    }

    "YY" in {
      val pd = ProcessedData(1, TrainingData(3))

      val p0 = Prediction(1, Query(2), Some(PAlgo0Model(3, pd)))
      val p1 = Prediction(1, Query(2), Some(PAlgo0Model(4, pd)))
      val p2 = Prediction(1, Query(2), Some(PAlgo1Model(3, pd)))

      p0 !== p1
      p0 !== p2
      p1 !== p2
    }

    /*
    "Parallel DS/P/Algos" in {
      val models = EngineWorkflow.train(
        sc,
        new Engine0.PDataSource0(9527),
        new Engine0.PPreparator0(),
        Seq(
          new Engine0.PAlgo0(),
          new Engine0.PAlgo1(),
          new Engine0.PAlgo0()))

      models must beEqualTo(
        Seq("PA0(PP0(PD0(9527)))", "PA1(PP0(PD0(9527)))", "PA0(PP0(PD0(9527)))")
      )
    }
    */

    /*
    "Parallel DS/P/Algos" in {
      val models = EngineWorkflow.train(
        sc,
        new Engine0.PDataSource0(9527),
        new Engine0.PPreparator0(),
        Seq(
          new Engine0.PAlgo0(),
          new Engine0.PAlgo1(),
          new Engine0.PAlgo0()))

      models must beEqualTo(
        Seq("PA0(PP0(PD0(9527)))", "PA1(PP0(PD0(9527)))", "PA0(PP0(PD0(9527)))")
      )
    }
  
    "Local DS/P/Algos" in {
      val models = EngineWorkflow.train(
        sc,
        new Engine0.LDataSource0(9527),
        new Engine0.LPreparator0(),
        Seq(
          new Engine0.LAlgo0(),
          new Engine0.LAlgo1(),
          new Engine0.LAlgo0()))

      val expectedResults = Seq(
        "LA0(LP0(LD0(9527)))", 
        "LA1(LP0(LD0(9527)))", 
        "LA0(LP0(LD0(9527)))")

      foreach(models.zip(expectedResults)) { case (model, expected) => 
        model must beAnInstanceOf[RDD[String]]
        val localModel = model.asInstanceOf[RDD[String]].collect
        localModel.toSeq must contain(exactly(expected))
      }
    }

    "P2L DS/P/Algos" in {
      val models = EngineWorkflow.train(
        sc,
        new Engine0.PDataSource0(9527),
        new Engine0.PPreparator0(),
        Seq(
          new Engine0.NAlgo0(),
          new Engine0.NAlgo1(),
          new Engine0.NAlgo0()))
      
      models must beEqualTo(
        Seq("NA0(PP0(PD0(9527)))", "NA1(PP0(PD0(9527)))", "NA0(PP0(PD0(9527)))")
      )
    }
    */

  }

  "EngineWorkflowSpec.eval" should {
    "Parallel DS/P/A/S" in {
      val id = 167
      val en = 2
      val qn = 5

      val evalDataSet: Seq[(EvalInfo, RDD[(Query, Prediction, Actual)])] = 
      EngineWorkflow.eval(
        sc,
        new PDataSource1(id = id, en = en, qn = qn),
        new PPreparator0(),
        Seq(new PAlgo0()),
        new LServing0())

      //val qRegex = """(\w+)\(Q\((\d+),(\d+)\)\)""".r
      //val aRegex = """(\w+)\(A\((\d+),(\d+)\)\)""".r

      foreach(evalDataSet.zipWithIndex) { case (evalData, ex) => {
        val (evalInfo, qpaRDD) = evalData
        evalInfo === EvalInfo(id)
        //evalInfo === s"PD1(E($ex,$k))"

        val qpaSeq: Seq[(Query, Prediction, Actual)] = qpaRDD.collect
        foreach (qpaSeq) { case (q, p, a) => 
          val Query(qId, _, _) = q
          val Actual(aId, _, _) = a
          qId == aId
        }

      }}

    }
  }
}

