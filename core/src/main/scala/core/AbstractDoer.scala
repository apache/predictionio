package io.prediction.core

import io.prediction.BaseParams
import java.lang.NoSuchMethodException
import scala.reflect._
  
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import org.json4s.native.JsonMethods
import org.json4s.native.JsonMethods._

abstract class AbstractDoer[P <: BaseParams : ClassTag]
extends Serializable {
  override def toString() : String = {
    val t = classTag[P].runtimeClass.getName
    s"Doer type: $t"
  }
  
  def paramsClass() = classTag[P]
}


object Doer {
  def apply[C <: AbstractDoer[_ <: BaseParams]] (
    cls: Class[_ <: C], params: BaseParams): C = {

    // Subclasses only allows two kind of constructors.
    // 1. Emtpy constructor.
    // 2. Constructor with P <: BaseParams.
    // We first try (2), if failed, we try (1).
    try {
      val constr = cls.getConstructor(params.getClass)
      return constr.newInstance(params).asInstanceOf[C]
    } catch {
      case e: NoSuchMethodException => {
        //val constr = cls.getConstructors()(0)
        val zeroConstr = cls.getConstructor()
        return zeroConstr.newInstance().asInstanceOf[C]
      }
    }
  }
}

/* Below are test functions. To be removed. */

class PDoer(p: XParams) extends AbstractDoer[XParams] {
}

object PDoer {
  val p = manifest[XParams]
  def q() = manifest[XParams]
}

case class XParams(val a: Int) extends BaseParams {
}


object Test {

  def main(args: Array[String]) {
    val a = new PDoer(new XParams(20))

    val c: Class[_ <: AbstractDoer[_ <: BaseParams]] = classOf[PDoer]

    val pClass = c.getConstructors()(0).getParameterTypes()(0)

    //val p = new Params(20)
    implicit val formats = DefaultFormats

    val j = JsonMethods.parse("""{"a" : 1}""")

    val z = pClass.cast(
      Extraction.extract(j, reflect.TypeInfo(pClass, None))(formats))
       
    println(z)
    return
  }
}



