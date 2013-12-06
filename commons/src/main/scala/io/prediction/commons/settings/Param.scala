package io.prediction.commons.settings

/** Param object.
  *
  * @param id ID.
  * @param name Parameter name.
  * @param description Parameter description.
  * @param defaultvalue Default value of the parameter.
  * @param constraint Constraint of the parameter.
  * @param ui UI information of the parameter.
  * @param scopes Scopes where this parameter is required.
  */
case class Param(
  id: String,
  name: String,
  description: Option[String],
  defaultvalue: Any,
  constraint: ParamConstraint,
  ui: ParamUI,
  scopes: Option[Set[String]] = None)

trait ParamConstraint {
  def paramtype: String
}

case class ParamBooleanConstraint(paramtype: String = "boolean") extends ParamConstraint

case class ParamDoubleConstraint(
  paramtype: String = "double",
  min: Option[Double] = None,
  max: Option[Double] = None)
  extends ParamConstraint

case class ParamIntegerConstraint(
  paramtype: String = "integer",
  min: Option[Int] = None,
  max: Option[Int] = None)
  extends ParamConstraint

case class ParamStringConstraint(paramtype: String = "string") extends ParamConstraint

case class ParamUI(
  uitype: String = "text",
  selections: Option[Seq[ParamSelectionUI]] = None,
  slidermin: Option[Int] = None,
  slidermax: Option[Int] = None,
  sliderstep: Option[Int] = None)

case class ParamSelectionUI(value: String, name: String)

case class ParamSection(
  name: String,
  sectiontype: String = "normal",
  description: Option[String] = None,
  subsections: Option[Seq[ParamSection]] = None,
  params: Option[Seq[String]] = None)
