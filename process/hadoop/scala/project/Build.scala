import sbt._
import Keys._

object PredictionIOAlgorithmsHadoopScalaBuild extends Build {
  lazy val root = Project(id = "scala", 
      base = file(".")) aggregate(commons, 
        itemsim_algo_itemsimcf, 
        itemrec_algo_knnitembased,
        itemrec_algo_randomrank,
        itemrec_algo_latestrank,
        itemrec_algo_mahout_itembased,
        itemrec_metric_map,
        itemrec_evaluation_trainingtestsplit)

  lazy val root2 = Project(id = "scala-assembly", 
      base = file(".")) dependsOn(commons, 
        itemsim_algo_itemsimcf, 
        itemrec_algo_knnitembased,
        itemrec_algo_randomrank,
        itemrec_algo_latestrank,
        itemrec_algo_mahout_itembased,
        itemrec_metric_map,
        itemrec_evaluation_trainingtestsplit)
      
  lazy val commons = Project(id = "scala-commons",
      base = file("commons"))

  /* itemsim */
  lazy val itemsim_algo_itemsimcf = Project(id = "scala-itemsim-algo-itemsimcf",
      base = file("engines/itemsim/algorithms/itemsimcf")) dependsOn(commons)
      
  /* itemrec */
  lazy val itemrec_algo_knnitembased = Project(id = "scala-itemrec-algo-knnitembased",
      base = file("engines/itemrec/algorithms/knnitembased")) dependsOn(commons)

  lazy val itemrec_algo_randomrank = Project(id = "scala-itemrec-algo-randomrank",
      base = file("engines/itemrec/algorithms/randomrank")) dependsOn(commons)

  lazy val itemrec_algo_latestrank = Project(id = "scala-itemrec-algo-latestrank",
      base = file("engines/itemrec/algorithms/latestrank")) dependsOn(commons)

  lazy val itemrec_algo_mahout_itembased = Project(id = "scala-itemrec-algo-mahout-itembased",
      base = file("engines/itemrec/algorithms/mahout/itembased")) dependsOn(commons)
      
  lazy val itemrec_metric_map = Project(id = "scala-itemrec-metric-map",
      base = file("engines/itemrec/metrics/map")) dependsOn(commons)

  lazy val itemrec_evaluation_trainingtestsplit = Project(id = "scala-itemrec-evaluation-trainingtestsplit",
      base = file("engines/itemrec/evaluations/trainingtestsplit")) dependsOn(commons)
  
}
