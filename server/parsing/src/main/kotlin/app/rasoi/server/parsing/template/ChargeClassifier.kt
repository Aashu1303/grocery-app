package app.rasoi.server.parsing.template

import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money

/** Maps a printed charge label and its sign to a [ChargeKind]. */
internal object ChargeClassifier {
    fun classify(label: String, amount: Money): ChargeKind {
        val text = label.lowercase()
        return when {
            amount.isNegative || "discount" in text || "promo" in text -> ChargeKind.DISCOUNT
            "round" in text -> ChargeKind.ROUND_OFF
            "handling" in text -> ChargeKind.HANDLING
            "delivery" in text -> ChargeKind.DELIVERY
            else -> ChargeKind.OTHER
        }
    }
}
