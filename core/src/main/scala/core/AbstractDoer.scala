package io.prediction.core

import io.prediction.BaseParams
import scala.reflect._

abstract class AbstractDoer[P <: BaseParams : ClassTag](val p: P) 
extends Serializable {
  override def toString() : String = {
    val t = classTag[P].runtimeClass.getName
    val v = p.toString()
    s"Doer type: $t value: $v"
  }
  
  def paramsClass() = classTag[P]
}

object Doer {
  def apply[C <: AbstractDoer[_ <: BaseParams]](
    cls: Class[_ <: C],
    params: BaseParams): C = { 
    val constr = cls.getConstructors()(0)
    return constr.newInstance(params).asInstanceOf[C]
  }
}

/* Below are test functions. To be removed. */

class PDoer(p: Params) extends AbstractDoer[Params](p) {
}

case class Params(val a: Int) extends BaseParams {
}


object Test {
  def main(args: Array[String]) {
    val a = new PDoer(new Params(20))

    val c: Class[_ <: AbstractDoer[_ <: BaseParams]] = classOf[PDoer]

    val p = new Params(20)

    /*
    val constr = c.getConstructors()(0)

    val ci: AbstractDoer[_] = 
      constr.newInstance(p).asInstanceOf[AbstractDoer[BaseParams]]
    */
    val ci = Doer(c, p)

    println(a)

    println(ci)
  }
}



