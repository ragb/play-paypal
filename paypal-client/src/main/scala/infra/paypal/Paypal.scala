package infra.paypal

import _root_.infra.paypal.objects.ErrorObject
import com.fasterxml.jackson.databind.JsonMappingException
import play.api.libs.json._
import scala.concurrent.duration._
import scala.concurrent.Future

import akka.http.scaladsl.client._
import akka.http.scaladsl.model._
import akka.actor._
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers._
import scala.collection.mutable.ListBuffer
import akka.pattern.{ ask, pipe }
import akka.http.scaladsl.unmarshalling._
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import akka.util.Timeout
import scala.util.control.NonFatal
import akka.event.Logging
import akka.stream.Materializer
import scala.util.Failure
import scala.language.postfixOps
import java.util.concurrent.TimeUnit

/**
 * @author alari (name.alari@gmail.com)
 * @since 18.10.13 16:44
 */
class Paypal(implicit system: ActorSystem, mat: Materializer) extends RequestBuilding with PlayJsonSupport {
  import system.dispatcher
  val log = Logging(system, getClass)
  private val configuration = ConfigFactory.load()
  private val token = configuration.getString("paypal.token")
  private val secret = configuration.getString("paypal.secret")
  private val endPoint = configuration.getString("paypal.endPoint")
  private val initializeAuthToken = configuration.getBoolean("paypal.initializeAuthToken")
  private val initializeAuthTokenDelay = FiniteDuration(configuration.getDuration("paypal.initializeAuthTokenDelay").toNanos, TimeUnit.NANOSECONDS)

  def doRequest(r: HttpRequest) = Http().singleRequest(r)
  private def requestToken = doRequest(Post(s"https://$endPoint/v1/oauth2/token", FormData("grant_type" -> "client_credentials"))
    .addHeader(Accept(MediaTypes.`application/json`))
    .addHeader(Authorization(BasicHttpCredentials(token, secret)))
    .addHeader(`Accept-Language`(LanguageRange("en_US")))) ~> readResponse(AccessToken.format)
  class TokenActor() extends Actor with ActorLogging {
    import TokenActor._

    var accessToken: Option[AccessToken] = None
    var tokenTime: Long = 0
    val waitingForToken = ListBuffer.empty[ActorRef]
    def receive = {
      case GetToken =>
        accessToken.filter { t => (t.expires_in + tokenTime) < (System.currentTimeMillis / 1000) }
          .fold({
            waitingForToken += sender
            requestNewToken()
          })(t => sender ! t)
      case RefreshToken =>
        requestNewToken()
    }

    def waitingForTokenResponse: Receive = {
      case token: AccessToken =>
        log.info(s"Got new access token expiring in ${token.expires_in}")
        accessToken = Some(token)
        // be smart and retreeve the token before it expires
        val secs = token.expires_in
        context.system.scheduler.scheduleOnce(secs seconds, self, RefreshToken)
        waitingForToken foreach { _ ! token }
        waitingForToken.clear()
        context.become(receive)
      case GetTokenFailure(e) =>
        log.error(e, "Error retreeving token")
        tokenTime = 0
        waitingForToken.foreach { _ ! Failure(e) }
        waitingForToken.clear()
        context.become(receive)
      case GetToken =>
        waitingForToken += sender
    }

    private def requestNewToken() {
      log.info("Retreeving new paypal authentication token")
      tokenTime = System.currentTimeMillis / 1000
      accessToken = None
      context.become(waitingForTokenResponse)
      requestToken recover { case NonFatal(t) => GetTokenFailure(t) } pipeTo self

    }

  }

  object TokenActor {
    case object GetToken
    case class GetTokenFailure(t: Throwable)
    case object RefreshToken
    def props = Props(new TokenActor)
  }

  import TokenActor.GetToken

  val tokenActor = system.actorOf(TokenActor.props)
  if (initializeAuthToken) {
    system.scheduler.scheduleOnce(initializeAuthTokenDelay, tokenActor, RefreshToken)
  }
  implicit val timeout = Timeout(10 seconds)
  private def retrieveToken = (tokenActor ? GetToken).mapTo[AccessToken]
  private def addRequestToken(request: HttpRequest)(token: AccessToken) = {
    request
      .addHeader(
        Authorization(OAuth2BearerToken(token.access_token))
      )
  }

  def authenticatedRequest[R](request: HttpRequest)(implicit reads: Reads[R]) = {
    def go(retry: Boolean): Future[R] = {
      def authRequest = retrieveToken map addRequestToken(request)
      authRequest ~>  {_ flatMap doRequest} ~> readResponse(reads) recoverWith {
        case ApiError(401, _, _) if retry => go(false)
      }
    }
    go(true)
  }

  def post[C, R](resource: String, content: C)(implicit reads: Reads[R], writes: Writes[C]) = authenticatedRequest[R](Post(s"https://$endPoint/v1/$resource", content).addHeader(Accept(MediaTypes.`application/json`)))
  def get[R](resource: String)(implicit reads: Reads[R]) = authenticatedRequest[R](Get(s"https://$endPoint/v1/$resource").addHeader(Accept(MediaTypes.`application/json`)))

  /**
   * Reads response, parses it into either ApiError, JsonError, or given R type
   *
   * @param resp Webservice call response
   * @param reads Json reader into R
   * @tparam R Type of successful response
   * @return
   */
  private def readResponse[R](reads: Reads[R])(f: Future[HttpResponse]): Future[R] = f flatMap { resp =>
    resp.status.intValue() match {
      case i: Int if i >= 200 && i < 300 =>
        Unmarshal(resp).to[JsValue] map { js =>
          reads.reads(js) match {
            case JsSuccess(v, _) => v
            case e: JsError => throw JsonError(resp.status.intValue, resp.status.reason, e, Some(js.toString))
          }
        }
      case _ =>
        Unmarshal(resp).to[JsValue] map { js =>
          ErrorObject.format.reads(js) match {
            case JsSuccess(v, _) =>
              throw ApiError(resp.status.intValue, resp.status.reason, v)
            case e: JsError => throw JsonError(resp.status.intValue, resp.status.reason, e, Some(e.toString))
          }
        } recover {
          case e: JsonMappingException =>
            throw FatalError(resp.status.intValue(), resp.status.reason(), e)
        }
    }
  }

}
