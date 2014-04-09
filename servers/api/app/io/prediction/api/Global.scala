import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends WithFilters(CORSFilter()) with GlobalSettings {
  val notFound = NotFound("Your request is not supported.")

  override def onHandlerNotFound(request: RequestHeader) = {
    Future.successful(notFound)
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    Future.successful(notFound)
  }
}
