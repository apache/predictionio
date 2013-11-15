package controllers

import io.prediction.commons.Config
import io.prediction.commons.settings._

import play.api.data._
import play.api.data.Forms._
import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

import com.mongodb.casbah.Imports._

import Forms._

class AdminSpec extends Specification {
  "PredictionIO Admin Specification".txt

  /** Setup test data. */
  val config = new Config
  val algoInfos = config.getSettingsAlgoInfos()
  val engineInfos = config.getSettingsEngineInfos()

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
        constraint = "boolean",
        scopes = Some(Set("manual"))),
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
        constraint = "double",
        scopes = Some(Set("auto", "manual")))),
    paramorder = Seq("ab", "cd", "ef", "gh", "ij"),
    engineinfoid = "dummy",
    techreq = Seq(),
    datareq = Seq()))

  engineInfos.insert(EngineInfo(
    id = "v12",
    name = "v12",
    description = None,
    defaultsettings = Map(
      "similarityFunction" -> Param(
        id = "similarityFunction",
        name = "similarityFunction",
        description = None,
        defaultvalue = "coocurrence",
        constraint = "string"),
      "freshness" -> Param(
        id = "freshness",
        name = "freshness",
        description = None,
        defaultvalue = 0,
        constraint = "integer")),
    defaultalgoinfoid = "dummy"))

  "PredictionIO Forms" should {
    "bind from good request 1" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "infotype" -> "algo",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "456.789"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            params("ef") must be_==("deadbeef") and
              (params("ab") must be_==(123)) and
              (params("cd") must be_==(false)) and
              (params("gh") must be_==(456.789))
          }
        ))
    }

    "bind from good request 2" in new WithApplication {
      val f = Form(single("anyid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "anyid" -> "v12",
        "infotype" -> "engine",
        "freshness" -> "4",
        "similarityFunction" -> "tanimoto"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            params("freshness") must be_==(4) and
              (params("similarityFunction") must be_==("tanimoto"))
          }
        ))
    }

    "bind from good request 3" in new WithApplication {
      val f = Form(single("anyid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "anyid" -> "v12",
        "infotype" -> "engine",
        "freshness" -> "4"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            params("freshness") must be_==(4)
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
        "infotype" -> "algo",
        "ab" -> "123asdf",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "456.789"))
      val params = bf.get
      bf.hasErrors must beFalse and
        (params.get("ab") must beNone)
    }

    "bind from bad request 3" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "infotype" -> "algo",
        "scope" -> "manual",
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
        "infotype" -> "algo",
        "scope" -> "auto",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "d456.789d"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("gh"))
    }

    "bind from bad request 5" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "d456.789d"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("infotype"))
    }

    "bind from bad request 6" in new WithApplication {
      val f = Form(single("algoinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "algoinfoid" -> "dummy",
        "infotype" -> "bad",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "d456.789d"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("infotype"))
    }

    "bind from bad request 7" in new WithApplication {
      val f = Form(single("engineinfoid" -> mapOfStringToAny))
      val bf = f.bind(Map(
        "engineinfoid" -> "bad",
        "infotype" -> "engine",
        "ab" -> "123",
        "cd" -> "false",
        "ef" -> "deadbeef",
        "gh" -> "d456.789d"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("engineinfoid"))
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
    MongoConnection()(config.appdataDbName).dropDatabase()
    MongoConnection()(config.modeldataDbName).dropDatabase()
  }
}
