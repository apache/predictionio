import sbt._
import Keys._

object Common {
  def packCommonJvmOpts = Seq("-Dconfig.file=${PROG_HOME}/conf/predictionio.conf", "-Dio.prediction.base=${PROG_HOME}")
}
