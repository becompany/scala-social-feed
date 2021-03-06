package ch.becompany.http.oauth

import akka.http.scaladsl.model.headers.GenericHttpCredentials
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, headers}
import akka.http.scaladsl.unmarshalling.Unmarshal
import ch.becompany.http.HttpClient
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService

import scala.concurrent.{ExecutionContext, Future}

trait OAuthSupport extends HttpClient {

  private val oauthHeaderName = "OAuth"

  val oauthConfig: OAuthConfig

  def oauthHeader(req: HttpRequest)(implicit ex: ExecutionContext): Future[HttpHeader] =
    (
      if (req.entity.isKnownEmpty()) Future(None)
      else Unmarshal(req.entity).to[String].map(Some(_))
    ).map(body => oauthHeader(req, body))

  def oauthHeader(request: HttpRequest, body: Option[String])(implicit ex: ExecutionContext): HttpHeader = {
    val req = DefaultConsumerService.createOauthenticatedRequest(
      KoauthRequest(
        method = request.method.value,
        url = request.uri.toString,
        authorizationHeader = None,
        body = body
      ),
      oauthConfig.consumerKey,
      oauthConfig.consumerSecret,
      oauthConfig.accessToken,
      oauthConfig.accessTokenSecret
    )
    headers.Authorization(GenericHttpCredentials(
      oauthHeaderName, req.header.substring(oauthHeaderName.length + 1)))
  }

  abstract override def additionalHeaders(request: HttpRequest)(implicit ec: ExecutionContext): Future[Seq[HttpHeader]] = {
    val headersFuture = super.additionalHeaders(request)
    val oauthHeaderFuture = oauthHeader(request)
    for (headers <- headersFuture; oauthHeader <- oauthHeaderFuture)
      yield headers :+ oauthHeader
  }

}
