package io.prediction.data.view

import io.prediction.data.storage.Event
import io.prediction.data.storage.Events
import io.prediction.data.storage.EventValidation
import io.prediction.data.storage.DataMap
import io.prediction.data.storage.Storage

import scala.concurrent.ExecutionContext.Implicits.global // TODO

import grizzled.slf4j.Logger

class TestSource(val appId: Int) {
  @transient lazy val logger = Logger[this.type]
  @transient lazy val batchView = new LBatchView(appId,
    None, None)

  def run() = {
    println(batchView.events)
  }
}

object QuickTest {

  def main(args: Array[String]) {
    val ts = new TestSource(args(0).toInt)
    ts.run()
  }
}
