package io.prediction.core

import io.prediction.BaseParams
import java.lang.NoSuchMethodException
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
  def apply[C <: AbstractDoer[_ <: BaseParams]] (
    cls: Class[_ <: C], params: BaseParams): C = {

    // Subclasses only allows two kind of constructors.
    // 1. Emtpy constructor.
    // 2. Constructor with P <: BaseParams.
    // We first try (1), if failed, we try (2).
    // There is a tech challenge with (2) as it is difficult to get the
    // constructor directly, as P is erased in this context. Instead, we
    // brutally extract the first constructor from the constructor list, and
    // pass params to it.

    // First try get zero constructor
    try {
      val zeroConstr = cls.getConstructor()
      return zeroConstr.newInstance().asInstanceOf[C]
    } catch {
      case e: NoSuchMethodException => {
        // No zero constructor found. Brutally get the first one.
        val constr = cls.getConstructors()(0)
        return constr.newInstance(params).asInstanceOf[C]
      }
    }
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



