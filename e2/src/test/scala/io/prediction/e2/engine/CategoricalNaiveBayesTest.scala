/** Copyright 2015 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package io.prediction.e2.engine

import io.prediction.e2.fixture.{LabeledPointFixture, SharedSparkContext}
import org.scalatest.{Matchers, FlatSpec}

import scala.language.reflectiveCalls

class CategoricalNaiveBayesTest extends FlatSpec with Matchers
with SharedSparkContext with LabeledPointFixture {
  val Tolerance = .0001
  val labeledPoints = labeledPointFixture.labeledPoints

  "Model" should "have log priors and log likelihoods" in {
    val labeledPointsRdd = sc.parallelize(labeledPoints)
    val model = CategoricalNaiveBayes.train(labeledPointsRdd)

    model.priors(Banana) should be(-.7885 +- Tolerance)
    model.priors(Orange) should be(-1.7047 +- Tolerance)
    model.priors(OtherFruit) should be(-1.0116 +- Tolerance)

    model.likelihoods(Banana)(0)(Long) should be(-.2231 +- Tolerance)
    model.likelihoods(Banana)(0)(NotLong) should be(-1.6094 +- Tolerance)
    model.likelihoods(Banana)(1)(Sweet) should be(-.2231 +- Tolerance)
    model.likelihoods(Banana)(1)(NotSweet) should be(-1.6094 +- Tolerance)
    model.likelihoods(Banana)(2)(Yellow) should be(-.2231 +- Tolerance)
    model.likelihoods(Banana)(2)(NotYellow) should be(-1.6094 +- Tolerance)

    model.likelihoods(Orange)(0) should not contain key(Long)
    model.likelihoods(Orange)(0)(NotLong) should be(0.0)
    model.likelihoods(Orange)(1)(Sweet) should be(-.6931 +- Tolerance)
    model.likelihoods(Orange)(1)(NotSweet) should be(-.6931 +- Tolerance)
    model.likelihoods(Orange)(2)(NotYellow) should be(0.0)
    model.likelihoods(Orange)(2) should not contain key(Yellow)

    model.likelihoods(OtherFruit)(0)(Long) should be(-.6931 +- Tolerance)
    model.likelihoods(OtherFruit)(0)(NotLong) should be(-.6931 +- Tolerance)
    model.likelihoods(OtherFruit)(1)(Sweet) should be(-.2877 +- Tolerance)
    model.likelihoods(OtherFruit)(1)(NotSweet) should be(-1.3863 +- Tolerance)
    model.likelihoods(OtherFruit)(2)(Yellow) should be(-1.3863 +- Tolerance)
    model.likelihoods(OtherFruit)(2)(NotYellow) should be(-.2877 +- Tolerance)
  }

  "Model's log score" should "be the log score of the given point" in {
    val labeledPointsRdd = sc.parallelize(labeledPoints)
    val model = CategoricalNaiveBayes.train(labeledPointsRdd)

    val score =
      model.logScore(LabeledPoint(Banana, Array(Long, NotSweet, NotYellow)))

    score should not be None
    score.get should be(-4.2304 +- Tolerance)
  }

  it should "be negative infinity for a point with a non-existing feature" in {
    val labeledPointsRdd = sc.parallelize(labeledPoints)
    val model = CategoricalNaiveBayes.train(labeledPointsRdd)

    val score =
      model.logScore(LabeledPoint(Banana, Array(Long, NotSweet, "Not Exist")))

    score should not be None
    score.get should be(Double.NegativeInfinity)
  }

  it should "be none for a point with a non-existing label" in {
    val labeledPointsRdd = sc.parallelize(labeledPoints)
    val model = CategoricalNaiveBayes.train(labeledPointsRdd)

    val score =
      model.logScore(LabeledPoint("Not Exist", Array(Long, NotSweet, Yellow)))

    score should be(None)
  }
}
