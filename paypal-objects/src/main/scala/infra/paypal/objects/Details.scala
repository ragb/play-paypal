package infra.paypal.objects

import play.api.libs.json.Json

/**
 * @author alari (name.alari@gmail.com)
 * @since 21.10.13 16:40
 *
 *        This object defines amount details
 *
 * @param shipping	string	Amount charged for shipping. 10 characters max with support for 2 decimal places.
 * @param subtotal	string	Sub-total (amount) of items being paid for. 10 characters max with support for 2 decimal places.
 * @param tax	string	Amount charged for tax. 10 characters max with support for 2 decimal places.
 * @param fee	string	Fee charged by PayPal. In case of a refund, this is the fee amount refunded to the original recipient of the payment. 10 characters max. Assigned in response.
 */
case class Details(shipping: Option[String], subtotal: Option[String], tax: Option[String], fee: Option[String] = None)

object Details {
  implicit val format = Json.format[Details]
}
