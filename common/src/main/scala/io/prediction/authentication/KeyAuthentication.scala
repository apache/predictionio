package io.prediction.authentication

/**
  * This is a (very) simple authentication for the dashboard and engine servers
  * It is highly recommended to implement a stonger authentication mechanism
  */

import java.io.File

import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import spray.http.HttpRequest
import spray.routing.{AuthenticationFailedRejection, RequestContext}
import spray.routing.authentication._
import spray.routing.directives.AuthMagnet
import scala.concurrent.Future


trait KeyAuthentication {

  object ServerKey {
    private val config = ConfigFactory.load("server.conf")
    val get = config.getString("io.prediction.server.accessKey")
    val param = "accessKey"
  }

  def withAccessKeyFromFile: RequestContext => Future[Authentication[HttpRequest]] = {
    ctx: RequestContext =>
      val accessKeyParamOpt = ctx.request.uri.query.get(ServerKey.param)
      Future {

        val passedKey = accessKeyParamOpt.getOrElse {
          Left(AuthenticationFailedRejection(
            AuthenticationFailedRejection.CredentialsRejected, List()))
        }

        if (passedKey.equals(ServerKey.get)) Right(ctx.request)
        else Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))

      }
  }
}
