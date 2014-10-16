package infra.paypal

import play.api.Play
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.ws.{WSResponse, WS, WSAuthScheme}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author alari (name.alari@gmail.com)
 * @since 18.10.13 16:44
 */
object Paypal {
  private lazy val token = Play.configuration.getString("paypal.token").get
  private lazy val secret = Play.configuration.getString("paypal.secret").get
  private lazy val endPoint = Play.configuration.getString("paypal.endPoint").get

  val TokenCacheKey = "paypal.token"

  def requestToken(implicit ec: ExecutionContext): Future[AccessToken] =
    Cache.getAs[AccessToken](TokenCacheKey) match {
      case Some(a) => Future(a)
      case None => WS.url(s"https://$endPoint/v1/oauth2/token")
        .withAuth(token, secret, scheme = WSAuthScheme.BASIC)
        .withHeaders(
          "Accept" -> "application/json",
          "Content-Type" -> "application/x-www-form-urlencoded",
          "Accept-Language" -> "en_US")
        .post("grant_type=client_credentials").flatMap(readResponse[AccessToken]).map {
          r =>
          Cache.set(TokenCacheKey, r, Duration(r.expires_in, "second"))
          r
      }
    }

  def post[R <: Codomain] = new {
    def apply[C <: Domain](token: AccessToken, resource: String, content: C)(implicit reads: Reads[R], writes: Writes[C], ec: ExecutionContext): Future[R] =
      getRequest(token, resource).post(writes.writes(content)).flatMap(readResponse[R])

    def apply[C <: Domain](resource: String, content: C)
                (implicit reads: Reads[R], writes: Writes[C], ec: ExecutionContext): Future[R] =
      requestToken.flatMap {
        apply(_, resource, content)
      }
  }

  def get[R <: Codomain](token: AccessToken, resource: String)(implicit reads: Reads[R], ec: ExecutionContext): Future[R] =
    getRequest(token, resource).get().flatMap(readResponse[R])

  def get[R <: Codomain](resource: String)(implicit reads: Reads[R], ec: ExecutionContext): Future[R] = requestToken.flatMap {
    get(_, resource)
  }

  /**
   * Reads response, parses it into either ApiError, JsonError, or given R type
   *
   * @param resp Webservice call response
   * @tparam R Type of successful response
   * @return
   */
  private def readResponse[R <: Codomain : Reads](resp: WSResponse): Future[R] = errorReader(respReader[R]).applyM(resp)

  def respReader[R: Reads]: WSResponse => R =
    resp =>
      implicitly[Reads[R]].reads(resp.json) match {
        case JsSuccess(v, _) => v
        case e: JsError => throw JsonError(resp.status, resp.statusText, e, Some(resp.body))
      }

  val errorReader = infra.wscalacl.ApiPostProcess(PaypalError)

  private def getRequest(token: AccessToken, resource: String)(implicit ec: ExecutionContext) = WS.url(s"https://$endPoint/v1/$resource")
    .withHeaders(
      "Content-Type" -> "application/json",
      "Authorization" -> s"Bearer ${token.access_token}")

}