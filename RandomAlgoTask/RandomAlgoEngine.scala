package org.sample.helloworld

import io.prediction.controller._

import scala.io.Source
import scala.collection.immutable.HashMap

// For random algorithm
import scala.util.Random

case class UserItemPair(userId:Int, recommendationId:Int)

// all data need to be serializable
class MyTrainingData(
  // list of (day, temperature) tuples
  val temperatures: List[(String, Double)]
) extends Serializable

class MyQuery(
  val userId: Int,
  val seed: Option[Long]
) extends Serializable {
  val randomSeed : Long= seed.getOrElse(5)
}

class MyModel(
  val recommendations: Map[Int, List[UserItemPair]]
) extends Serializable {
  override def toString = recommendations.toString
}

class MyPrediction(
  val recommendationList: List[UserItemPair]
) extends Serializable

// controller components
class MyDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams,
  MyTrainingData, MyQuery, EmptyActual] {

  /* override this to return Training Data only */
  override
  def readTraining(): MyTrainingData = {
    val lines = Source.fromFile("../data/helloworld/data.csv").getLines()
      .toList.map{ line =>
        val data = line.split(",")
        (data(0), data(1).toDouble)
      }

    new MyTrainingData(lines)
  }
}

class MyAlgorithm extends LAlgorithm[EmptyAlgorithmParams, MyTrainingData,
  MyModel, MyQuery, MyPrediction] {

  override
  def train(pd: MyTrainingData): MyModel = {

    // Temporary data object... ten random pairs of user_id and parameter
    val sampleUserId = 888
    val sampleTargetItems = List( UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt),
                           UserItemPair(Random.nextInt, Random.nextInt) )

    // Push randomization to prediction step, so that seed can be param of
    // query.
    // val shuffledTargetItems = Random.shuffle(sampleTargetItems)

    new MyModel( Map(sampleUserId -> sampleTargetItems) )
  }

  override
  def predict(model: MyModel, query: MyQuery): MyPrediction = {
    Random.setSeed(query.randomSeed)
    val randomRec = Random.shuffle(model.recommendations(query.userId))
    new MyPrediction(randomRec)
  }
}

// factory
object MyEngineFactory extends IEngineFactory {
  override
  def apply() = {
    /* SimpleEngine only requires one DataSouce and one Algorithm */
    new SimpleEngine(
      classOf[MyDataSource],
      classOf[MyAlgorithm]
    )
  }
}
