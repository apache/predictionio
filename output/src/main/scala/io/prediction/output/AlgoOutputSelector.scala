package io.prediction.output

import io.prediction.commons.settings._

class AlgoOutputSelector(algos: Algos) {
  val multipleAlgoErrorMsg = "Deploying multiple algorithms is not yet supported. No results can be returned."

  def itemRecSelection(uid: String, n: Int, itypes: Option[List[String]])(implicit app: App, engine: Engine): Seq[String] = {
    /** Check engine type. */
    if (engine.enginetype != "itemrec") throw new RuntimeException("Not an itemrec engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.enginetype))

    val itemRecAlgos = algos.getDeployedByEngineid(engine.id)

    if (!itemRecAlgos.hasNext) throw new RuntimeException("No deployed algorithm for specified engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.enginetype))

    implicit val algo = itemRecAlgos.next()

    /** Multiple deployment not yet supported. */
    if (itemRecAlgos.hasNext) throw new RuntimeException(multipleAlgoErrorMsg)

    ItemRecAlgoOutput.output(uid, n, itypes)
  }

  def itemSimSelection(iid: String, n: Int, itypes: Option[List[String]])(implicit app: App, engine: Engine): Seq[String] = {
    /** Check engine type. */
    if (engine.enginetype != "itemsim") throw new RuntimeException("Not an itemsim engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.enginetype))

    val itemSimAlgos = algos.getDeployedByEngineid(engine.id)

    if (!itemSimAlgos.hasNext) throw new RuntimeException("No deployed algorithm for specified engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.enginetype))

    implicit val algo = itemSimAlgos.next()

    /** Multiple deployment not yet supported. */
    if (itemSimAlgos.hasNext) throw new RuntimeException(multipleAlgoErrorMsg)

    ItemSimAlgoOutput.output(iid, itypes)
  }
}
