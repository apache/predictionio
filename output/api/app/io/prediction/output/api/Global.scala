import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {
  val notFound = NotFound("Your request is not supported.")

  override def onHandlerNotFound(request: RequestHeader): Result = {
    notFound
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    notFound
  }
}
