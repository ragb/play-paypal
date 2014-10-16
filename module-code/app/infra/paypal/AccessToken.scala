package infra.paypal

import play.api.libs.json.Json

/**
 * @author alari
 * @since 7/24/14
 */
case class AccessToken(scope: String, access_token: String, token_type: String, app_id: String, expires_in: Int) extends Serializable with Codomain

object AccessToken {
  implicit val format = Json.format[AccessToken]
}