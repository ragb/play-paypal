package infra.paypal.objects

import play.api.libs.json.Json
import infra.paypal.format.EnumFormat

/**
 * @author alari (name.alari@gmail.com)
 * @since 21.10.13 16:11
 *
 *        This object represents a payer’s funding instrument, such as a credit card or token that represents a credit card.
 *
 * @param id	string	ID of the credit card. This ID is provided in the response when storing credit cards. Required if using a stored credit card.
 * @param payer_id	string	A unique identifier that you can assign and track when storing a credit card or using a stored credit card. This ID can help to avoid unintentional use or misuse of credit cards. This ID can be any value you would like to associate with the saved card, such as a UUID, username, or email address. Required when using a stored credit card if a payer_id was originally provided when storing the credit card in vault.
 * @param number	string	Credit card number. Numeric characters only with no spaces or punctuation. The string must conform with modulo and length required by each credit card type. Redacted in responses. Required.
 * @param type	string	Credit card type. Valid types are: visa, mastercard, discover, amex Required.
 * @param expire_month	integer	Expiration month with no leading zero. Acceptable values are 1 through 12. Required.
 * @param expire_year	integer	4-digit expiration year. Required.
 * @param cvv2	string	3-4 digit card validation code.
 * @param first_name	string	Cardholder’s first name.
 * @param last_name	string	Cardholder’s last name.
 * @param billing_address	address	Billing address associated with card.
 * @param state	string	State of the credit card funding instrument: expired or ok. Assigned in response.
 * @param valid_until	string	Funding instrument expiration date. Assigned in response.
 */
case class CreditCard(
  id: Option[String],
  payer_id: Option[String],
  number: String,
  `type`: CardType.Value,
  expire_month: String,
  expire_year: String,
  cvv2: Option[String] = None,
  first_name: String,
  last_name: String,
  billing_address: Option[Address] = None,
  state: Option[CardState.Value] = None,
  valid_until: Option[String] = None
)

object CardType extends Enumeration with EnumFormat {
  val Visa = Value("visa")
  val MasterCard = Value("mastercard")
  val AmericanExpress = Value("amex")
  val Discover = Value("discover")

  implicit val format = valueFormat
}

object CardState extends Enumeration with EnumFormat {
  val Ok = Value("ok")
  val Expired = Value("expired")

  implicit val format = valueFormat
}

object CreditCard {
  implicit val format = Json.format[CreditCard]
}