package io.prediction.core.deploy.worker

import akka.actor.Actor

import io.prediction.core.deploy.Process

class ProcessExecutor extends Actor {

  def receive = {
    case Process(workId, command, cwd, extraEnv, Nil) =>
      val proc = scala.sys.process.Process(command, cwd, extraEnv: _*)
      val returnCode = proc.!
      sender ! Worker.WorkComplete(returnCode)
  }

}
