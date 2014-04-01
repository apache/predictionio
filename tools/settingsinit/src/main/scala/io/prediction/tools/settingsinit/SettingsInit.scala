package io.prediction.tools.settingsinit

import io.prediction.commons._
import io.prediction.commons.settings._

import scala.reflect.ClassTag
import scala.util.parsing.json.JSON

/** Extractors: http://stackoverflow.com/questions/4170949/how-to-parse-json-in-scala-using-standard-scala-classes */
class CC[T: ClassTag] {
  def unapply(a: Any)(implicit e: ClassTag[T]): Option[T] = {
    try { Some(e.runtimeClass.cast(a).asInstanceOf[T]) } catch { case _: Throwable => None }
  }
}

object M extends CC[Map[String, Any]]
object SM extends CC[Seq[Map[String, Any]]]
object MSS extends CC[Map[String, String]]
object SS extends CC[Seq[String]]
object OSS extends CC[Option[Seq[String]]]
object S extends CC[String]
object OS extends CC[Option[String]]
object D extends CC[Double]
object B extends CC[Boolean]

object SettingsInit {
  val config = new Config()

  def main(args: Array[String]) {
    val algoInfos = config.getSettingsAlgoInfos
    val engineInfos = config.getSettingsEngineInfos
    val offlineEvalSplitterInfos = config.getSettingsOfflineEvalSplitterInfos
    val offlineEvalMetricInfos = config.getSettingsOfflineEvalMetricInfos
    val paramGenInfos = config.getSettingsParamGenInfos
    val systemInfos = config.getSettingsSystemInfos

    val settingsFile = try { args(0) } catch {
      case e: Throwable =>
        println("Please specify the location of the initial settings file in the command line. Aborting.")
        sys.exit(1)
    }

    val settingsString = try { scala.io.Source.fromFile(settingsFile).mkString } catch {
      case e: Throwable =>
        println(s"Unable to open ${settingsFile}: ${e.getMessage}. Aborting.")
        sys.exit(1)
    }

    val settingsJson = JSON.parseFull(settingsString) getOrElse {
      println(s"Unable to parse initial settings file ${settingsFile}. Aborting.")
    }

    println("PredictionIO settings initialization starting")

    M.unapply(settingsJson) map { settings =>
      println("Deleting old SystemInfo entries...")
      systemInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        systemInfos.delete(info.id)
      }
      M.unapply(settings("systeminfos")) map { infos =>
        println("Populating SystemInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(value) = info("value")
          OS(description) = info.get("description")
        } yield {
          val si = SystemInfo(
            id = id,
            value = value,
            description = description)

          println(s"Deleting any old SystemInfo ID: ${id}")
          systemInfos.delete(id)
          println(s"Adding SystemInfo ID: ${id}")
          systemInfos.insert(si)
        }
      } getOrElse println("Cannot find any SystemInfo information. Skipping.")

      println("Deleting old EngineInfo entries...")
      engineInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        engineInfos.delete(info.id)
      }
      M.unapply(settings("engineinfos")) map { infos =>
        println("Populating EngineInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(name) = info("name")
          OS(description) = info.get("description")
          M(params) = info("params")
          SM(paramsections) = info("paramsections")
          S(defaultalgoinfoid) = info("defaultalgoinfoid")
          S(defaultofflineevalmetricinfoid) = info("defaultofflineevalmetricinfoid")
          S(defaultofflineevalsplitterinfoid) = info("defaultofflineevalsplitterinfoid")
        } yield {
          println(s"Processing EngineInfo ID: ${id}")
          val castedparams = mapToParams(params)
          val castedparamsections = mapToParamSections(paramsections.asInstanceOf[Seq[Map[String, Any]]])
          val ei = EngineInfo(
            id = id,
            name = name,
            description = description,
            params = castedparams,
            paramsections = castedparamsections,
            defaultalgoinfoid = defaultalgoinfoid,
            defaultofflineevalmetricinfoid = defaultofflineevalmetricinfoid,
            defaultofflineevalsplitterinfoid = defaultofflineevalsplitterinfoid)

          println(s"Deleting any old EngineInfo ID: ${id}")
          engineInfos.delete(id)
          println(s"Adding EngineInfo ID: ${id}")
          engineInfos.insert(ei)
        }
      } getOrElse println("Cannot find any EngineInfo information. Skipping.")

      println("Deleting old AlgoInfo entries...")
      algoInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        algoInfos.delete(info.id)
      }
      M.unapply(settings("algoinfos")) map { infos =>
        println("Populating AlgoInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(name) = info("name")
          OS(description) = info.get("description")
          OSS(batchcommands) = info.get("batchcommands")
          OSS(offlineevalcommands) = info.get("offlineevalcommands")
          SS(paramorder) = info("paramorder")
          M(params) = info("params")
          SM(paramsections) = info("paramsections")
          S(engineinfoid) = info("engineinfoid")
          SS(techreq) = info("techreq")
          SS(datareq) = info("datareq")
        } yield {
          println(s"Processing AlgoInfo ID: ${id}")
          val castedparams = mapToParams(params)
          val castedparamsections = mapToParamSections(paramsections.asInstanceOf[Seq[Map[String, Any]]])
          val ai = AlgoInfo(
            id = id,
            name = name,
            description = description,
            batchcommands = batchcommands,
            offlineevalcommands = offlineevalcommands,
            params = castedparams,
            paramsections = castedparamsections,
            paramorder = paramorder,
            engineinfoid = engineinfoid,
            techreq = techreq,
            datareq = datareq)

          println(s"Deleting any old AlgoInfo ID: ${id}")
          algoInfos.delete(id)
          println(s"Adding AlgoInfo ID: ${id}")
          algoInfos.insert(ai)
        }
      } getOrElse println("Cannot find any AlgoInfo information. Skipping.")

      println("Deleting old OfflineEvalSplitterInfo entries...")
      offlineEvalSplitterInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        offlineEvalSplitterInfos.delete(info.id)
      }
      M.unapply(settings("offlineevalsplitterinfos")) map { infos =>
        println("Populating OfflineEvalSplitterInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(name) = info("name")
          SS(engineinfoids) = info("engineinfoids")
          OS(description) = info.get("description")
          OSS(commands) = info.get("commands")
          M(params) = info("params")
          SM(paramsections) = info("paramsections")
          SS(paramorder) = info("paramorder")
        } yield {
          println(s"Processing OfflineEvalSplitterInfo ID: ${id}")
          val castedparams = mapToParams(params)
          val castedparamsections = mapToParamSections(paramsections.asInstanceOf[Seq[Map[String, Any]]])
          val mi = OfflineEvalSplitterInfo(
            id = id,
            name = name,
            engineinfoids = engineinfoids,
            description = description,
            commands = commands,
            params = castedparams,
            paramsections = castedparamsections,
            paramorder = paramorder)

          println(s"Deleting any old OfflineEvalSplitterInfo ID: ${id}")
          offlineEvalSplitterInfos.delete(id)
          println(s"Adding OfflineEvalSplitterInfo ID: ${id}")
          offlineEvalSplitterInfos.insert(mi)
        }
      } getOrElse println("Cannot find any OfflineEvalSplitterInfo information. Skipping.")

      println("Deleting old OfflineEvalMetricInfo entries...")
      offlineEvalMetricInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        offlineEvalMetricInfos.delete(info.id)
      }
      M.unapply(settings("offlineevalmetricinfos")) map { infos =>
        println("Populating OfflineEvalMetricInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(name) = info("name")
          SS(engineinfoids) = info("engineinfoids")
          OS(description) = info.get("description")
          OSS(commands) = info.get("commands")
          M(params) = info("params")
          SM(paramsections) = info("paramsections")
          SS(paramorder) = info("paramorder")
        } yield {
          println(s"Processing OfflineEvalMetricInfo ID: ${id}")
          val castedparams = mapToParams(params)
          val castedparamsections = mapToParamSections(paramsections.asInstanceOf[Seq[Map[String, Any]]])
          val mi = OfflineEvalMetricInfo(
            id = id,
            name = name,
            engineinfoids = engineinfoids,
            description = description,
            commands = commands,
            params = castedparams,
            paramsections = castedparamsections,
            paramorder = paramorder)

          println(s"Deleting any old OfflineEvalMetricInfo ID: ${id}")
          offlineEvalMetricInfos.delete(id)
          println(s"Adding OfflineEvalMetricInfo ID: ${id}")
          offlineEvalMetricInfos.insert(mi)
        }
      } getOrElse println("Cannot find any OfflineEvalMetricInfo information. Skipping.")

      println("Deleting old ParamGenInfo entries...")
      paramGenInfos.getAll() foreach { info =>
        println(s"- ${info.id}")
        paramGenInfos.delete(info.id)
      }
      M.unapply(settings("paramgeninfos")) map { infos =>
        println("Populating ParamGenInfos...")
        for {
          id <- infos.keys
          M(info) = infos(id)
          S(name) = info("name")
          OS(description) = info.get("description")
          OSS(commands) = info.get("commands")
          SS(paramorder) = info("paramorder")
          MSS(paramnames) = info("paramnames")
          MSS(paramdescription) = info("paramdescription")
          MSS(paramdefaults) = info("paramdefaults")
        } yield {
          val mi = ParamGenInfo(
            id = id,
            name = name,
            description = description,
            commands = commands,
            paramorder = paramorder,
            paramnames = paramnames,
            paramdescription = paramdescription,
            paramdefaults = paramdefaults)

          paramGenInfos.get(id) map { m =>
            println(s"Updating ParamGenInfo ID: ${id}")
            paramGenInfos.update(mi)
          } getOrElse {
            println(s"Adding ParamGenInfo ID: ${id}")
            paramGenInfos.insert(mi)
          }
        }
      } getOrElse println("Cannot find any ParamGenInfo information. Skipping.")
    } getOrElse println("Root level is not an object. Aborting.")

    println("PredictionIO settings initialization finished")
  }

  def mapToParams(params: Map[String, Any]): Map[String, Param] = {
    /**
     * Take care of integers that are parsed as double from JSON
     * http://www.ecma-international.org/ecma-262/5.1/#sec-4.3.19
     */
    params map { p =>
      val param = p._2.asInstanceOf[Map[String, Any]]
      val paramconstraint = param("constraint").asInstanceOf[Map[String, Any]]
      val paramui = param("ui").asInstanceOf[Map[String, Any]]
      val constraint = paramconstraint("paramtype").asInstanceOf[String] match {
        case "boolean" => ParamBooleanConstraint()
        case "double" => ParamDoubleConstraint(min = paramconstraint.get("min").map(_.asInstanceOf[Double]), max = paramconstraint.get("max").map(_.asInstanceOf[Double]))
        case "integer" => ParamIntegerConstraint(min = paramconstraint.get("min").map(_.asInstanceOf[Int]), max = paramconstraint.get("max").map(_.asInstanceOf[Int]))
        case "string" => ParamStringConstraint()
        case _ => ParamStringConstraint()
      }
      val uitype = paramui("uitype").asInstanceOf[String]
      val ui = uitype match {
        case "selection" => {
          val selectionsSeq = paramui("selections").asInstanceOf[Seq[Map[String, String]]]
          ParamUI(uitype = "selection", selections = Some(selectionsSeq.map(s => ParamSelectionUI(name = s("name"), value = s("value")))))
        }
        case "slider" =>
          ParamUI(
            uitype = "slider",
            slidermin = paramui.get("slidermin").map(_.asInstanceOf[Double].toInt),
            slidermax = paramui.get("slidermax").map(_.asInstanceOf[Double].toInt),
            sliderstep = paramui.get("sliderstep").map(_.asInstanceOf[Double].toInt))
        case _ => ParamUI(uitype = uitype)
      }
      val casteddefault = constraint.paramtype match {
        case "integer" => param("defaultvalue").asInstanceOf[Double].toInt
        case _ => param("defaultvalue")
      }
      (p._1, Param(
        id = p._1,
        name = param("name").asInstanceOf[String],
        description = param.get("description") map { _.asInstanceOf[String] },
        defaultvalue = casteddefault,
        constraint = constraint,
        ui = ui))
    }
  }

  def mapToParamSections(paramsections: Seq[Map[String, Any]]): Seq[ParamSection] = {
    paramsections map { paramsection =>
      ParamSection(
        name = paramsection("name").asInstanceOf[String],
        sectiontype = paramsection("sectiontype").asInstanceOf[String],
        description = paramsection.get("description").map(_.asInstanceOf[String]),
        subsections = paramsection.get("subsections").map(ss => mapToParamSections(ss.asInstanceOf[Seq[Map[String, Any]]])),
        params = paramsection.get("params").map(_.asInstanceOf[Seq[String]]))
    }
  }
}
