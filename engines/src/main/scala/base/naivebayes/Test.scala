package io.prediction.engines.base.naivebayes

import nak.classify._
import nak.data._
import breeze.linalg._

object Run {
  def main(args: Array[String]) {
    //val c = Counter(Seq(("A", 1.0), ("B", 1.0), ("C", 0)):_*)

    val trainingData = Seq[Example[Boolean, Counter[String, Double]]](
      Example(true, Counter(Seq(("A", 1.0), ("B", 1.0), ("C", 0.0)):_*)),
      Example(true, Counter(Seq(("A", 0.0), ("B", 1.0), ("C", 1.0)):_*)),
      Example(true, Counter(Seq(("A", 1.0), ("B", 1.0), ("C", 0.0)):_*)),
      Example(true, Counter(Seq(("A", 1.0), ("B", 1.0), ("C", 1.0)):_*)),
      Example(false, Counter(Seq(("A", 1.0), ("B", 0.0), ("C", 0.0)):_*)),
      Example(false, Counter(Seq(("A", 0.0), ("B", 0.0), ("C", 1.0)):_*)),
      Example(false, Counter(Seq(("A", 1.0), ("B", 0.0), ("C", 1.0)):_*)))

    val trainer = new NaiveBayes.Trainer[Boolean, String]()

    val model = trainer.train(trainingData)

    trainingData.foreach { example => {
      val scores = model.scores(example.features)
      println()
      println(example)
      println(scores)
      println(scores.mapValues(math.exp))
      println(model(example.features))
    }}

  }
}
