import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScalaBuild extends Build {
  lazy val root = Project(id = "scala", 
      base = file(".")) aggregate(commons, itemsim_algo_itemsimcf, itemrec_algo_knnitembased, itemrec_metric_map, itemrec_evaluation_trainingtestsplit)

  lazy val root2 = Project(id = "scala-assembly", 
      base = file(".")) dependsOn(commons, itemsim_algo_itemsimcf, itemrec_algo_knnitembased, itemrec_metric_map, itemrec_evaluation_trainingtestsplit)
      
  lazy val commons = Project(id = "scala-commons",
      base = file("commons"))
                            
  lazy val itemsim_algo_itemsimcf = Project(id = "scala-itemsim-algo-itemsimcf",
      base = file("engines/itemsim/algorithms/itemsimcf")) dependsOn(commons)
      
  lazy val itemrec_algo_knnitembased = Project(id = "scala-itemrec-algo-knnitembased",
      base = file("engines/itemrec/algorithms/knnitembased")) dependsOn(commons)
      
  lazy val itemrec_metric_map = Project(id = "scala-itemrec-metric-map",
      base = file("engines/itemrec/metrics/map")) dependsOn(commons)

  lazy val itemrec_evaluation_trainingtestsplit = Project(id = "scala-itemrec-evaluation-trainingtestsplit",
      base = file("engines/itemrec/evaluations/trainingtestsplit")) dependsOn(commons)
  
}
