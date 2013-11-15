package io.prediction.commons.settings

/** Param object.
  *
  * @param id ID.
  * @param name Parameter name.
  * @param description Parameter description.
  * @param defaultvalue Default value of the parameter.
  * @param constraint Constraint of the parameter. Valid values are integer, double, string, boolean, and regular expression.
  * @param scopes Scopes where this parameter is required.
  */
case class Param(
  id: String,
  name: String,
  description: Option[String],
  defaultvalue: Any,
  constraint: String,
  scopes: Option[Set[String]] = None)
