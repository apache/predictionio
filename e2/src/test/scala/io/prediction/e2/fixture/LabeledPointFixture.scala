package io.prediction.e2.fixture

import io.prediction.e2.engine.LabeledPoint

trait LabeledPointFixture {
  val Banana = "Banana"
  val Orange = "Orange"
  val OtherFruit = "Other Fruit"
  val NotLong = "Not Long"
  val Long = "Long"
  val NotSweet = "Not Sweet"
  val Sweet = "Sweet"
  val NotYellow = "Not Yellow"
  val Yellow = "Yellow"

  def labeledPointFixture = {
    new {
      val labeledPoints = Seq(
        LabeledPoint(Banana, Array(Long, Sweet, Yellow)),
        LabeledPoint(Banana, Array(Long, Sweet, Yellow)),
        LabeledPoint(Banana, Array(Long, Sweet, Yellow)),
        LabeledPoint(Banana, Array(Long, Sweet, Yellow)),
        LabeledPoint(Banana, Array(NotLong, NotSweet, NotYellow)),
        LabeledPoint(Orange, Array(NotLong, Sweet, NotYellow)),
        LabeledPoint(Orange, Array(NotLong, NotSweet, NotYellow)),
        LabeledPoint(OtherFruit, Array(Long, Sweet, NotYellow)),
        LabeledPoint(OtherFruit, Array(NotLong, Sweet, NotYellow)),
        LabeledPoint(OtherFruit, Array(Long, Sweet, Yellow)),
        LabeledPoint(OtherFruit, Array(NotLong, NotSweet, NotYellow))
      )
    }
  }
}
