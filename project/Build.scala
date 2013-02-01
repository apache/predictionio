import sbt._
import Keys._

object PredictionIOBuild extends Build {
  lazy val root = Project(id = "predictionio", base = file(".")) aggregate(commons)

  lazy val commons = Project(id = "predictionio-commons", base = file("commons"))
}
