package io.prediction.commons.modeldata

import io.prediction.commons.Config
import io.prediction.commons.Spec
import io.prediction.commons.settings.{ Algo, App }

import org.specs2._
import org.specs2.specification.Step
import com.mongodb.casbah.Imports._
import com.github.nscala_time.time.Imports._

class ModelMetadataKeyvals extends Specification {
  def is = s2"""

    PredictionIO Model Data Metadata Keyval Specification

    MetadataKeyvals can be implemented by:
    1. MongoMetadataKeyvals ${mongoMetadataKeyvals}

  """

  def mongoMetadataKeyvals = s2"""

    MongoMetadataKeyvals should

    - behave like any MetadataKeyvals implementation
    ${metadataKeyvals(newMongoMetadataKeyvals)}

    (clean up database after test)
    ${Step(Spec.mongoClient(mongoDbName).dropDatabase())}

  """

  def metadataKeyvals(metadataKeyvals: MetadataKeyvals) = s2"""

    upserting and getting MetadataKeyval ${upsertAndGet(metadataKeyvals)}

  """

  val mongoDbName = "predictionio_modeldata_mongometadatakeyval_test"

  def newMongoMetadataKeyvals = new mongodb.MongoMetadataKeyvals(
    new Config, Spec.mongoClient(mongoDbName))

  def upsertAndGet(keyvals: MetadataKeyvals) = {
    val algoid = 1
    val modelset = true
    val otherAlgoid = 2
    val notExistsAlgoid = 3
    val keyval0 = MetadataKeyval(algoid, modelset, "foo", "1")
    val keyval1 = MetadataKeyval(algoid, modelset, "bar", "3")
    val keyval2 = MetadataKeyval(algoid, modelset, "foo", "2")
    val keyval3 = MetadataKeyval(otherAlgoid, modelset, "bar", "4")

    keyvals.upsert(keyval0)
    keyvals.upsert(keyval1)
    keyvals.upsert(keyval2)
    keyvals.upsert(keyval3)

    (keyvals.get(algoid, modelset, "foo") must beSome("2")) and
      (keyvals.get(algoid, modelset, "bar") must beSome("3")) and
      (keyvals.get(otherAlgoid, modelset, "foo") must beNone) and
      (keyvals.get(otherAlgoid, modelset, "bar") must beSome("4")) and
      (keyvals.get(notExistsAlgoid, modelset, "bar") must beNone)
  }
}
