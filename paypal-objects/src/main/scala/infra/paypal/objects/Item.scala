package infra.paypal.objects

import play.api.libs.json.Json

/**
 * @author alari (name.alari@gmail.com)
 * @since 21.10.13 16:19
 *
 *        This object defines a item object
 *
 * @param quantity	string	Number of a particular item. 10 characters max. Required.
 * @param name	string	Item name. 127 characters max. Required.
 * @param price	string	Item cost. 10 characters max. Required.
 * @param currency	string	3-letter currency code. Required.
 * @param sku	string	Stock keeping unit corresponding (SKU) to item. 50 characters max.
 */
case class Item(
  quantity: Int,
  name: String, price: BigDecimal, currency: String, sku: Option[String] = None
)

object Item {
  implicit val wbd = infra.paypal.Implicits.writesBigDecimal

  implicit val format = Json.format[Item]
}
