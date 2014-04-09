import controllers.Default
import play.api.mvc.{ SimpleResult, RequestHeader, Filter }

case class CORSFilter() extends Filter {
  import scala.concurrent._
  import ExecutionContext.Implicits.global

  def isPreflight(r: RequestHeader) = {
    r.method.toLowerCase.equals("options") && r.headers.get("Access-Control-Request-Method").nonEmpty
  }

  def apply(f: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    if (isPreflight(request)) {
      Future.successful(Default.Ok.withHeaders(
        "Access-Control-Allow-Origin" -> "*",
        "Access-Control-Allow-Methods" -> "GET, POST, DELETE"
      ))
    } else {
      f(request).map(_.withHeaders(
        "Access-Control-Allow-Origin" -> "*"
      ))
    }
  }
}