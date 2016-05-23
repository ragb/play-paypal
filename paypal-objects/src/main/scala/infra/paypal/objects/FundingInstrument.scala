package infra.paypal.objects

import play.api.libs.json.Json

/**
 * @author alari (name.alari@gmail.com)
 * @since 21.10.13 16:08
 *
 *        This object represents a payer’s funding instrument (credit card).
 *
 * @param credit_card	credit_card	Credit card details. Required if creating a funding instrument.
 * @param credit_card_token	credit_card_token	Token for credit card details stored with PayPal. You can use this in place of a credit card. Required if not passing credit card details.
 */
case class FundingInstrument(credit_card: Option[CreditCard], credit_card_token: Option[CreditCardToken])

object FundingInstrument {
  implicit val format = Json.format[FundingInstrument]
}
