//import sbt._
//import sbt.Keys._
//import xerial.sbt.Pack._
//
//object Build extends sbt.Build {
//  lazy val root = Project(
//    base = file("."),
//    settings = Defaults.defaultSettings ++ packSettings ++
//      Seq(
//        // Map from program name -> Main class (full path)
//        packMain := Map("conncheck" -> "io.prediction.tools.conncheck.ConnCheck")
//        // Add custom settings here
//      )
//  )
//}
