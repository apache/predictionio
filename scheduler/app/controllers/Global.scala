import java.util.Date

import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    Logger.info("Quartz scheduler is starting...")

    io.prediction.scheduler.Scheduler.scheduler.start()
  }

  override def onStop(app: Application) = {
    Logger.info("Quartz scheduler is shutting down...")

    io.prediction.scheduler.Scheduler.scheduler.shutdown()
  }
}