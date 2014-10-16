package infra.paypal

import com.fasterxml.jackson.databind.JsonMappingException
import infra.paypal.objects.ErrorObject
import play.api.libs.json.{JsSuccess, JsError}
import play.api.libs.ws.WSResponse

sealed trait PaypalError extends Throwable with Codomain{
  def status: Int
}

object PaypalError extends (WSResponse => Option[PaypalError]) {
  def apply(resp: WSResponse): Option[PaypalError] =
    try {
      ErrorObject.format.reads(resp.json) match {
        case JsSuccess(v, _) =>
          Some(ApiError(resp.status, resp.statusText, v))
        case e: JsError =>
          resp.status match {
            case s if s >= 200 && s < 300 =>
              None
            case _ =>
              Some(JsonError(resp.status, resp.statusText, e, Some(resp.body)))
          }
      }
    } catch {
      case e: JsonMappingException =>
        Some(FatalError(resp.status, resp.statusText, e))
    }
}

case class JsonError(status: Int, statusText: String, err: JsError, body: Option[String] = None) extends PaypalError {
  override def toString = s"$status $statusText -> $body -> ${JsError.toFlatJson(err)}"
  override def getMessage = this.toString
}

case class ApiError(status: Int, statusText: String, err: ErrorObject) extends PaypalError {
  override def toString = s"$status $statusText -> $err"
  override def getMessage = this.toString
}

case class FatalError(status: Int, statusText: String, err: Throwable) extends PaypalError {
  override def toString = s"$status $statusText -> $err"
  override def getMessage = this.toString
}