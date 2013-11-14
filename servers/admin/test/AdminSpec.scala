package controllers

import io.prediction.commons.Config
import io.prediction.commons.settings._

import play.api.data._
import play.api.data.Forms._
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

import com.mongodb.casbah.Imports._

import PredictionIOForms._

class AdminSpec extends Specification {
  "PredictionIO Admin Specification".txt

  /** Setup test data. */
  val config = new Config
  val algoInfos = config.getSettingsAlgoInfos()

  algoInfos.insert(AlgoInfo(
    id = "dummy",
    name = "dummy",
    description = None,
    batchcommands = None,
    offlineevalcommands = None,
    params = Map(
      "ab" -> Param(
        id = "ab",
        name = "ab",
        description = None,
        defaultvalue = "ab",
        constraint = "integer"),
      "cd" -> Param(
        id = "cd",
        name = "cd",
        description = None,
        defaultvalue = "cd",
        constraint = "boolean"),
      "ef" -> Param(
        id = "ef",
        name = "ef",
        description = None,
        defaultvalue = "ef",
        constraint = "string"),
      "gh" -> Param(
        id = "gh",
        name = "gh",
        description = None,
        defaultvalue = "gh",
        constraint = "double")),
    paramorder = Seq("ab", "cd", "ef", "gh", "ij"),
    engineinfoid = "dummy",
    techreq = Seq(),
    datareq = Seq()))

  "PredictionIO Forms" should {
    "bind from good request" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "456.789"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            params("ef") must be_==("deadbeef") and
              (params("ab") must be_==(123))
              (params("cd") must be_==(false))
              (params("gh") must be_==(456.789))
          }
        ))
    }

    "bind from bad request 1" in new WithApplication {
      val f = Form(tuple(
        "algoinfoid" -> mapOfStringToAny,
        "dummy" -> nonEmptyText))
      val bf = f.bind(Map(
        "dummy" -> "something"))
      bf.hasErrors must beTrue
    }

    "bind from bad request 2" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "ab" -> "123asdf",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "456.789"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("ab"))
    }

    "bind from bad request 3" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "ab" -> "123",
        "cd" -> "fals",
        "ef" -> "deadbeef",
        "gh" -> "456.789"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("cd"))
    }

    "bind from bad request 4" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "d456.789d"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("gh"))
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
    MongoConnection()(config.appdataDbName).dropDatabase()
    MongoConnection()(config.modeldataDbName).dropDatabase()
  }
}
