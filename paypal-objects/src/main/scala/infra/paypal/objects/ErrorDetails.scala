package infra.paypal.objects

import play.api.libs.json.Json

/**
 * @author alari (name.alari@gmail.com)
 * @since 21.10.13 16:43
 *
 *        This object represents an error returned in the response.
 *
 * @param field	string	Name of the field that caused the error.
 * @param issue	string	Reason for the error.
 */
case class ErrorDetails(field: String, issue: String)

object ErrorDetails {
  implicit val format = Json.format[ErrorDetails]
}