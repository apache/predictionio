package io.prediction.output

import io.prediction.commons.settings._

class AlgoOutputSelector(algos: Algos) {
  val multipleAlgoErrorMsg = "Deploying multiple algorithms is not yet supported. No results can be returned."

  def itemRecSelection(uid: String, n: Int, itypes: Option[Seq[String]], latlng: Option[Tuple2[Double, Double]], within: Option[Double], unit: Option[String])(implicit app: App, engine: Engine): Seq[String] = {
    implicit val algo = itemRecAlgoSelection(engine)

    itemrec.ItemRecAlgoOutput.output(uid, n, itypes, latlng, within, unit)
  }

  def itemRecAlgoSelection(engine: Engine): Algo = {
    /** Check engine type. */
    if (engine.infoid != "itemrec") throw new RuntimeException("Not an itemrec engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val itemRecAlgos = algos.getDeployedByEngineid(engine.id)

    if (!itemRecAlgos.hasNext) throw new RuntimeException("No deployed algorithm for specified engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val algo = itemRecAlgos.next()

    /** Multiple deployment not yet supported. */
    if (itemRecAlgos.hasNext) throw new RuntimeException(multipleAlgoErrorMsg)

    algo
  }

  def itemSimSelection(iid: String, n: Int, itypes: Option[Seq[String]], latlng: Option[Tuple2[Double, Double]], within: Option[Double], unit: Option[String])(implicit app: App, engine: Engine): Seq[String] = {
    implicit val algo = itemSimAlgoSelection(engine)

    itemsim.ItemSimAlgoOutput.output(iid, n, itypes, latlng, within, unit)
  }

  def itemSimAlgoSelection(engine: Engine): Algo = {
    /** Check engine type. */
    if (engine.infoid != "itemsim") throw new RuntimeException("Not an itemsim engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val itemSimAlgos = algos.getDeployedByEngineid(engine.id)

    if (!itemSimAlgos.hasNext) throw new RuntimeException("No deployed algorithm for specified engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val algo = itemSimAlgos.next()

    /** Multiple deployment not yet supported. */
    if (itemSimAlgos.hasNext) throw new RuntimeException(multipleAlgoErrorMsg)

    algo
  }

  def itemRankSelection(uid: String, iids: Seq[String])(implicit app: App, engine: Engine): Seq[String] = {
    implicit val algo = itemRankAlgoSelection(engine)

    itemrank.ItemRankAlgoOutput.output(uid, iids)
  }

  def itemRankAlgoSelection(engine: Engine): Algo = {
    /** Check engine type. */
    if (engine.infoid != "itemrank") throw new RuntimeException("Not an itemrank engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val itemRankAlgos = algos.getDeployedByEngineid(engine.id)

    if (!itemRankAlgos.hasNext) throw new RuntimeException("No deployed algorithm for specified engine (id: %d, name: %s, type: %s)" format (engine.id, engine.name, engine.infoid))

    val algo = itemRankAlgos.next()

    /** Multiple deployment not yet supported. */
    if (itemRankAlgos.hasNext) throw new RuntimeException(multipleAlgoErrorMsg)

    algo
  }
}
