package io.prediction.engines.stock

object DecisionTreePrinter {
  import org.apache.spark.mllib.tree.model._

  def p(model: DecisionTreeModel)(
    implicit featureMakerSeq: Seq[FeatureMaker]
    ): Unit = {
    println("Print model")
    p("", model.topNode)
  }

  def p(prefix: String, node: Node)(
    implicit featureMakerSeq: Seq[FeatureMaker]
    ): Unit = {
    if (node.isLeaf) {
      val stats = node.stats.get
      val stdev = math.sqrt(stats.impurity)
      println(
        f"${prefix}%-8s " +
        f"impurity = ${stats.impurity}% 6.4f " +
        f"predict = ${stats.predict}% 6.4f " +
        f"stdev = $stdev% 6.4f")
    } else {
      val split = node.split.get
      if (split.featureType == FeatureType.Continuous) {
        println(f"${prefix}%-8s " + 
          f"f:${featureMakerSeq(split.feature)}%-8s " +
          f"v:${split.threshold}% 6.4f" )
      } else {
        println(f"${prefix}%-8s " + 
          f"f:${featureMakerSeq(split.feature)}%-8s " +
          f"c:${split.categories}" )
      }
      println(f"         " + node.stats.get)
      p(prefix + "L", node.leftNode.get)
      p(prefix + "R", node.rightNode.get)
    }
  }
}

