package io.prediction.api

import controllers.Default
import play.api.mvc.{ SimpleResult, RequestHeader, Filter }

case class CORSFilter(allowedDomainString: Option[String] = None) extends Filter {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  lazy val allowedDomains = allowedDomainString.orElse(play.api.Play.current.configuration.getString("cors.allowed.domains"))
    .getOrElse("").split(",")

  def isPreflight(r: RequestHeader) = {
    r.method.toLowerCase.equals("options") && r.headers.get("Access-Control-Request-Method").nonEmpty
  }

  /**
   * If the origin of the request is allowed, return the origin.
   * Otherwise, return "" (no CORS access allowed).
   */
  def getOriginAllowed(requestOrigin: String): String = {
    for (allowedDomain <- allowedDomains) {
      if (allowedDomain == "*" || requestOrigin == allowedDomain) {
        return requestOrigin
      }
    }
    return ""
  }

  def apply(f: (RequestHeader) => Future[SimpleResult])(request: RequestHeader): Future[SimpleResult] = {
    if (isPreflight(request)) {
      Future.successful(Default.Ok.withHeaders(
        "Access-Control-Allow-Origin" -> getOriginAllowed(request.headers.get("Origin").getOrElse("")),
        "Access-Control-Allow-Methods" -> "GET, POST, DELETE"
      ))
    } else {
      f(request).map(_.withHeaders(
        "Access-Control-Allow-Origin" -> getOriginAllowed(request.headers.get("Origin").getOrElse(""))
      ))
    }
  }
}