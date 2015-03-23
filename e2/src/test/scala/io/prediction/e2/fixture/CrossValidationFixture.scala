package io.prediction.e2.fixture

import io.prediction.e2.engine.LabeledPoint

trait CrossValidationFixture {
  def dataset = {
    new {
      val Label1 = "l1"
      val Label2 = "l2"
      val Label3 = "l3"
      val Attribute1 = "a1"
      val NotAttribute1 = "na1"
      val Attribute2 = "a2"
      val NotAttribute2 = "na2"

      val labeledPoints = Seq(
        LabeledPoint(Label1, Array(Attribute1, Attribute2)),
        LabeledPoint(Label2, Array(NotAttribute1, Attribute2)),
        LabeledPoint(Label3, Array(Attribute1, NotAttribute2))
      )
    }
  }
}
