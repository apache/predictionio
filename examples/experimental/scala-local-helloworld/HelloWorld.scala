package org.sample.helloworld

import io.prediction.controller._

import scala.io.Source
import scala.collection.immutable.HashMap

// all data need to be serializable
case class MyTrainingData(
  // list of (day, temperature) tuples
  temperatures: List[(String, Double)]
)

case class MyQuery(
  day: String
)

case class MyModel(
  temperatures: HashMap[String, Double]
) {
  override def toString = temperatures.toString
}

case class MyPredictedResult(
  temperature: Double
)

// controller components
class MyDataSource extends LDataSource[EmptyDataSourceParams, EmptyDataParams,
  MyTrainingData, MyQuery, EmptyActualResult] {

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
  MyModel, MyQuery, MyPredictedResult] {

  override
  def train(pd: MyTrainingData): MyModel = {
    // calculate average value of each day
    val average = pd.temperatures
      .groupBy(_._1) // group by day
      .mapValues{ list =>
        val tempList = list.map(_._2) // get the temperature
        tempList.sum / tempList.size
      }

    // trait Map is not serializable, use concrete class HashMap
    new MyModel(HashMap[String, Double]() ++ average)
  }

  override
  def predict(model: MyModel, query: MyQuery): MyPredictedResult = {
    val temp = model.temperatures(query.day)
    new MyPredictedResult(temp)
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
