package io.prediction.tools.conncheck

import io.prediction.commons._

object ConnCheck {
  val config = new Config()

  def main(args: Array[String]) {
    val connchecks = Seq(
      config.settingsDbConnectable(),
      config.appdataDbConnectable(),
      config.appdataTrainingDbConnectable(),
      config.appdataTestDbConnectable(),
      config.modeldataDbConnectable(),
      config.modeldataTrainingDbConnectable())

    if (!connchecks.reduce((a, b) => a && b)) {
      if (!connchecks(0)) {
        println(s"Cannot connect to settings database ${config.settingsDbType}://${config.settingsDbHost}:${config.settingsDbPort}/${config.settingsDbName}.")
      }
      if (!connchecks(1)) {
        println(s"Cannot connect to app data database ${config.appdataDbType}://${config.appdataDbHost}:${config.appdataDbPort}/${config.appdataDbName}.")
      }
      if (!connchecks(2)) {
        println(s"Cannot connect to app data training database ${config.appdataTrainingDbType}://${config.appdataTrainingDbHost}:${config.appdataTrainingDbPort}/${config.appdataTrainingDbName}.")
      }
      if (!connchecks(3)) {
        println(s"Cannot connect to app data test database ${config.appdataTestDbType}://${config.appdataTestDbHost}:${config.appdataTestDbPort}/${config.appdataTestDbName}.")
      }
      if (!connchecks(4)) {
        println(s"Cannot connect to model data database ${config.modeldataDbType}://${config.modeldataDbHost}:${config.modeldataDbPort}/${config.modeldataDbName}.")
      }
      if (!connchecks(5)) {
        println(s"Cannot connect to model data training database ${config.modeldataTrainingDbType}://${config.modeldataTrainingDbHost}:${config.modeldataTrainingDbPort}/${config.modeldataTrainingDbName}.")
      }
      println("Aborting.")
      sys.exit(1)
    }
  }
}
