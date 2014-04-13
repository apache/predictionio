package io.prediction.commons.settings

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

/**
 * Contains metadata for a parameter that is used in an Info object.
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

/**
 * Base trait for a parameter constraint.
 */
trait ParamConstraint {
  /**
   * Parameter type.
   * Can be "boolean", "double", "integer", "long", or "string".
   */
  def paramtype: String
}

/**
 * Indicates a parameter of type Boolean.
 */
case class ParamBooleanConstraint(paramtype: String = "boolean")
  extends ParamConstraint

/**
 * Indicates a parameter of type Double.
 *
 * @param min Minimum allowed value.
 * @param max Maximum allowed value.
 */
case class ParamDoubleConstraint(
  paramtype: String = "double",
  min: Option[Double] = None,
  max: Option[Double] = None)
    extends ParamConstraint

/**
 * Indicates a parameter of type Int.
 *
 * @param min Minimum allowed value.
 * @param max Maximum allowed value.
 */
case class ParamIntegerConstraint(
  paramtype: String = "integer",
  min: Option[Int] = None,
  max: Option[Int] = None)
    extends ParamConstraint

/**
 * Indicates a parameter of type Long.
 *
 * @param min Minimum allowed value.
 * @param max Maximum allowed value.
 */
case class ParamLongConstraint(
  paramtype: String = "long",
  min: Option[Long] = None,
  max: Option[Long] = None)
    extends ParamConstraint

/**
 * Indicates a parameter of type String.
 */
case class ParamStringConstraint(paramtype: String = "string")
  extends ParamConstraint

/**
 * Defines the parameter's user interface that will be used in the
 * administration user interface.
 *
 * @param uitype User interface type. Can be "text", "slider", and "selection".
 * @param selections List of selectable items to be displayed when UI type is
 *                   "selection".
 * @param slidermin Minimum value of the slider when UI type is "slider".
 * @param slidermax Maximum value of the slider when UI type is "slider".
 * @param sliderstep Step size of the slider when UI type is "slider".
 */
case class ParamUI(
  uitype: String = "text",
  selections: Option[Seq[ParamSelectionUI]] = None,
  slidermin: Option[Int] = None,
  slidermax: Option[Int] = None,
  sliderstep: Option[Int] = None)

/**
 * A selectable item for the selection user interface.
 *
 * @param value Selection's value.
 * @param name Selection's name.
 */
case class ParamSelectionUI(value: String, name: String)

/**
 * Defines a section that contains parameters to be displayed in the user
 * interface.
 *
 * @param name Section's name.
 * @param sectiontype Section's type. Can be "normal" and "tuning".
 * @param description Section's description.
 * @param subsections A list of subsections.
 * @param params A list of parameters of this section.
 */
case class ParamSection(
  name: String,
  sectiontype: String = "normal",
  description: Option[String] = None,
  subsections: Option[Seq[ParamSection]] = None,
  params: Option[Seq[String]] = None)

/** json4s serializer for the Param class. */
class ParamSerializer extends CustomSerializer[Param](format => (
  {
    case x: JObject =>
      implicit val formats = Serialization.formats(ShortTypeHints(List(
        classOf[ParamBooleanConstraint],
        classOf[ParamDoubleConstraint],
        classOf[ParamIntegerConstraint],
        classOf[ParamLongConstraint],
        classOf[ParamStringConstraint])))
      val constraint = (x \ "constraint").extract[ParamConstraint]
      val dv = x \ "defaultvalue"
      val defaultvalue = constraint match {
        case c: ParamBooleanConstraint => dv.extract[Boolean]
        case c: ParamDoubleConstraint => dv.extract[Double]
        case c: ParamIntegerConstraint => dv.extract[Int]
        case c: ParamLongConstraint => dv.extract[Long]
        case c: ParamStringConstraint => dv.extract[String]
      }
      Param(
        id = (x \ "id").extract[String],
        name = (x \ "name").extract[String],
        description = (x \ "description").extract[Option[String]],
        defaultvalue = defaultvalue,
        constraint = constraint,
        ui = (x \ "ui").extract[ParamUI],
        scopes = (x \ "scopes").extract[Option[Set[String]]])
  },
  {
    case p: Param =>
      implicit val formats = Serialization.formats(ShortTypeHints(List(
        classOf[ParamBooleanConstraint],
        classOf[ParamDoubleConstraint],
        classOf[ParamIntegerConstraint],
        classOf[ParamLongConstraint],
        classOf[ParamStringConstraint])))
      JObject(
        JField("id", Extraction.decompose(p.id)) ::
          JField("name", Extraction.decompose(p.name)) ::
          JField("description", Extraction.decompose(p.description)) ::
          JField("defaultvalue", Extraction.decompose(p.defaultvalue)) ::
          JField("constraint", Extraction.decompose(p.constraint)) ::
          JField("ui", Extraction.decompose(p.ui)) ::
          JField("scopes", Extraction.decompose(p.scopes)) :: Nil)
  })
)
