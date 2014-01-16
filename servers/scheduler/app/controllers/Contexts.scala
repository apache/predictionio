package io.prediction.scheduler

import play.api.Play.current
import play.api.libs.concurrent.Akka

object Contexts {
  implicit val stepExecutionContext = Akka.system.dispatchers.lookup("io.prediction.scheduler.steps")
}
