package io.prediction.commons.settings.mongodb

import io.prediction.commons.MongoUtils
import io.prediction.commons.settings._

import com.mongodb.casbah.Imports._

/** MongoDB implementation of Param. */
object MongoParam {
  def dbObjToParam(id: String, dbObj: DBObject) = {
    val constraintObj = dbObj.as[DBObject]("constraint")
    Param(
      id = id,
      name = dbObj.as[String]("name"),
      description = dbObj.getAs[String]("description"),
      defaultvalue = dbObj("defaultvalue"),
      constraint = constraintObj.as[String]("paramtype") match {
        case "boolean" => ParamBooleanConstraint()
        case "double" => ParamDoubleConstraint(
          min = constraintObj.getAs[Double]("min"),
          max = constraintObj.getAs[Double]("max"))
        case "integer" => ParamIntegerConstraint(
          min = constraintObj.getAs[Int]("min"),
          max = constraintObj.getAs[Int]("max"))
        case "long" => ParamLongConstraint(
          min = constraintObj.getAs[Long]("min"),
          max = constraintObj.getAs[Long]("max"))
        case "string" => ParamStringConstraint()
        case _ => ParamStringConstraint()
      },
      ui = asParamUI(dbObj.as[DBObject]("ui")),
      scopes = dbObj.getAs[MongoDBList]("scopes") map {
        _.toSet.map((x: Any) => x.toString)
      })
  }

  def paramToDBObj(param: Param) = {
    MongoDBObject(
      "name" -> param.name,
      "defaultvalue" -> param.defaultvalue,
      "constraint" -> (MongoDBObject("paramtype" ->
        param.constraint.paramtype) ++ (param.constraint.paramtype match {
        case "boolean" => MongoUtils.emptyObj
        case "double" =>
          (param.constraint.asInstanceOf[ParamDoubleConstraint].min map {
            x => MongoDBObject("min" -> x)
          } getOrElse MongoUtils.emptyObj) ++
            (param.constraint.asInstanceOf[ParamDoubleConstraint].max map {
              x => MongoDBObject("max" -> x)
            } getOrElse MongoUtils.emptyObj)
        case "integer" =>
          (param.constraint.asInstanceOf[ParamIntegerConstraint].min map {
            x => MongoDBObject("min" -> x)
          } getOrElse MongoUtils.emptyObj) ++
            (param.constraint.asInstanceOf[ParamIntegerConstraint].max map {
              x => MongoDBObject("max" -> x)
            } getOrElse MongoUtils.emptyObj)
        case "long" =>
          (param.constraint.asInstanceOf[ParamLongConstraint].min map {
            x => MongoDBObject("min" -> x)
          } getOrElse MongoUtils.emptyObj) ++
            (param.constraint.asInstanceOf[ParamLongConstraint].max map {
              x => MongoDBObject("max" -> x)
            } getOrElse MongoUtils.emptyObj)
        case "string" => MongoUtils.emptyObj
        case _ => MongoUtils.emptyObj
      })),
      "ui" -> (MongoDBObject("uitype" -> param.ui.uitype) ++
        param.ui.slidermin.map(m => MongoDBObject("slidermin" -> m))
        .getOrElse(MongoUtils.emptyObj) ++
        param.ui.slidermax.map(m => MongoDBObject("slidermax" -> m))
        .getOrElse(MongoUtils.emptyObj) ++
        param.ui.sliderstep.map(m => MongoDBObject("sliderstep" -> m))
        .getOrElse(MongoUtils.emptyObj) ++
        (param.ui.selections map { selections =>
          MongoDBObject("selections" -> (selections map { selection =>
            MongoDBObject("value" -> selection.value, "name" -> selection.name)
          }))
        } getOrElse MongoUtils.emptyObj))) ++
      (param.description map { d =>
        MongoDBObject("description" -> d)
      } getOrElse MongoUtils.emptyObj) ++
      (param.scopes map { s =>
        MongoDBObject("scopes" -> s.toSeq)
      } getOrElse MongoUtils.emptyObj)
  }

  def dbObjToParamSection(dbObj: DBObject): ParamSection = ParamSection(
    name = dbObj.as[String]("name"),
    sectiontype = dbObj.as[String]("sectiontype"),
    description = dbObj.getAs[String]("description"),
    subsections = dbObj.getAs[Seq[DBObject]]("subsections").map(
      _.map(dbObjToParamSection(_))),
    params = dbObj.getAs[Seq[String]]("params"))

  def paramSectionToDBObj(paramsection: ParamSection): MongoDBObject = {
    MongoDBObject("name" -> paramsection.name,
      "sectiontype" -> paramsection.sectiontype) ++
      (paramsection.description map { d =>
        MongoDBObject("description" -> d)
      } getOrElse MongoUtils.emptyObj) ++
      (paramsection.subsections map { ss =>
        MongoDBObject("subsections" -> ss.map(s => paramSectionToDBObj(s)))
      } getOrElse MongoUtils.emptyObj) ++
      (paramsection.params map { ps =>
        MongoDBObject("params" -> ps)
      } getOrElse MongoUtils.emptyObj)
  }

  private def asParamUI(uiObj: DBObject): ParamUI = {
    ParamUI(
      uitype = uiObj.as[String]("uitype"),
      selections = uiObj.getAs[MongoDBList]("selections") map { selections =>
        selections map { s =>
          val selection = s.asInstanceOf[DBObject]
          ParamSelectionUI(selection.as[String]("value"),
            selection.as[String]("name"))
        }
      },
      slidermin = uiObj.getAs[Int]("slidermin"),
      slidermax = uiObj.getAs[Int]("slidermax"),
      sliderstep = uiObj.getAs[Int]("sliderstep"))
  }
}
