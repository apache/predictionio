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
  val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos()
  val offlineEvalSplitterInfos = config.getSettingsOfflineEvalSplitterInfos()

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
        ui = ParamUI(),
        constraint = ParamIntegerConstraint()),
      "cd" -> Param(
        id = "cd",
        name = "cd",
        description = None,
        defaultvalue = "cd",
        constraint = ParamBooleanConstraint(),
        ui = ParamUI(),
        scopes = Some(Set("manual"))),
      "ef" -> Param(
        id = "ef",
        name = "ef",
        description = None,
        defaultvalue = "ef",
        ui = ParamUI(),
        constraint = ParamStringConstraint()),
      "gh" -> Param(
        id = "gh",
        name = "gh",
        description = None,
        defaultvalue = "gh",
        constraint = ParamDoubleConstraint(),
        ui = ParamUI(),
        scopes = Some(Set("auto", "manual")))),
    paramorder = Seq("ab", "cd", "ef", "gh", "ij"),
    paramsections = Seq(),
    engineinfoid = "dummy",
    techreq = Seq(),
    datareq = Seq()))

  engineInfos.insert(EngineInfo(
    id = "v12",
    name = "v12",
    description = None,
    params = Map(
      "similarityFunction" -> Param(
        id = "similarityFunction",
        name = "similarityFunction",
        description = None,
        defaultvalue = "coocurrence",
        ui = ParamUI(),
        constraint = ParamStringConstraint()),
      "freshness" -> Param(
        id = "freshness",
        name = "freshness",
        description = None,
        defaultvalue = 0,
        ui = ParamUI(),
        constraint = ParamIntegerConstraint())),
    paramsections = Seq(),
    defaultalgoinfoid = "dummy",
    defaultofflineevalmetricinfoid = "dummy-metric",
    defaultofflineevalsplitterinfoid = "dummy-splitter"))

  offlineEvalMetricInfos.insert(OfflineEvalMetricInfo(
    id = "dummy-metric",
    name = "dummy-metric",
    description = None,
    engineinfoids = Seq("itemrec"),
    commands = None,
    params = Map(
      "foo" -> Param(
        id = "foo",
        name = "foo",
        description = None,
        defaultvalue = "bar",
        ui = ParamUI(),
        constraint = ParamStringConstraint()),
      "bar" -> Param(
        id = "bar",
        name = "bar",
        description = None,
        defaultvalue = 3.14,
        ui = ParamUI(),
        constraint = ParamDoubleConstraint())),
    paramsections = Seq(),
    paramorder = Seq()))

  offlineEvalSplitterInfos.insert(OfflineEvalSplitterInfo(
    id = "dummy-splitter",
    name = "dummy-splitter",
    description = None,
    engineinfoids = Seq("itemsim"),
    commands = None,
    params = Map(
      "foo" -> Param(
        id = "foo",
        name = "foo",
        description = None,
        defaultvalue = true,
        ui = ParamUI(),
        constraint = ParamBooleanConstraint()),
      "bar" -> Param(
        id = "bar",
        name = "bar",
        description = None,
        defaultvalue = 3,
        ui = ParamUI(),
        constraint = ParamIntegerConstraint())),
    paramsections = Seq(),
    paramorder = Seq()))

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
        "freshness" -> "4",
        "similarityFunction" -> "city"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            params("freshness") must be_==(4)
          }
        ))
    }

    "bind from good request 4" in new WithApplication {
      val f = Form(single("infoid" -> seqOfMapOfStringToAny))
      val bf = f.bind(Map(
        "infoid[1]" -> "dummy-splitter",
        "infoid[0]" -> "dummy-metric",
        "infotype[1]" -> "offlineevalsplitter",
        "infotype[0]" -> "offlineevalmetric",
        "foo[0]" -> "baz",
        "bar[0]" -> "12.345",
        "foo[1]" -> "false",
        "bar[1]" -> "54321"))
      bf.hasErrors must beFalse and
        (bf.fold(
          f => 1 must be_==(2),
          params => {
            (params(0)("foo") must be_==("baz")) and
              (params(0)("bar") must be_==(12.345)) and
              (params(1)("foo") must be_==(false)) and
              (params(1)("bar") must be_==(54321))
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
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("ab"))
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

    "bind from good request 8" in new WithApplication {
      val f = Form(single("infoid" -> seqOfMapOfStringToAny))
      val bf = f.bind(Map(
        "infoid[0]" -> "dummy-splitter",
        "infoid[1]" -> "dummy-metric",
        "infotype[0]" -> "offlineevalsplitter",
        "infotype[1]" -> "offlineevalmetric",
        "foo[0]" -> "baz",
        "bar[0]" -> "12.345",
        "foo[1]" -> "false",
        "bar[1]" -> "54321"))
      bf.hasErrors must beTrue and
        (bf.errors(0).key must be_==("foo[0]")) and
        (bf.errors(1).key must be_==("bar[0]"))
    }
  }

  "Helper.offlineEvalSplitterParamToString()" should {
    "convert splitter param to string correctly" in {
      val splitterInfo = OfflineEvalSplitterInfo(
        id = "dummy-splitter-x",
        name = "dummy-splitter",
        description = None,
        engineinfoids = Seq("itemsim"),
        commands = None,
        params = Map(
          "foo" -> Param(
            id = "foo",
            name = "Foo Name",
            description = None,
            defaultvalue = true,
            ui = ParamUI(),
            constraint = ParamBooleanConstraint()),
          "bar" -> Param(
            id = "bar",
            name = "Bar Name",
            description = None,
            defaultvalue = 3,
            ui = ParamUI(
              uitype = "selection",
              selections = Some(Seq(
                ParamSelectionUI("3", "Three"),
                ParamSelectionUI("4", "Four"),
                ParamSelectionUI("5", "Five")
              ))
            ),
            constraint = ParamIntegerConstraint())),
        paramsections = Seq(),
        paramorder = Seq("foo", "bar"))

      val splitter = OfflineEvalSplitter(
        id = 4,
        evalid = 5,
        name = "some name",
        infoid = "dummy-splitter-x",
        settings = Map("foo" -> false, "bar" -> 5)
      )

      val settingString = Helper.offlineEvalSplitterParamToString(splitter, Some(splitterInfo))
      val expectedString = "Foo Name: false, Bar Name: Five"

      val splitter2 = OfflineEvalSplitter(
        id = 4,
        evalid = 5,
        name = "some name",
        infoid = "dummy-splitter-x",
        settings = Map("foo" -> true, "bar" -> 3)
      )

      val settingString2 = Helper.offlineEvalSplitterParamToString(splitter2, Some(splitterInfo))
      val expectedString2 = "Foo Name: true, Bar Name: Three"

      settingString must beEqualTo(expectedString) and
        (settingString2 must beEqualTo(expectedString2))
    }
  }

  step {
    MongoConnection()(config.settingsDbName).dropDatabase()
    MongoConnection()(config.appdataDbName).dropDatabase()
    MongoConnection()(config.modeldataDbName).dropDatabase()
  }
}
