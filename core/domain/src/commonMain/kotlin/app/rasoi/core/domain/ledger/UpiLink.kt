package app.rasoi.core.domain.ledger

import app.rasoi.core.model.Money

/** A `upi://pay` deep link that opens the payer's UPI app with the payee, amount and note filled in (ADR 0022). */
object UpiLink {
    private val VPA = Regex("""^[A-Za-z0-9._-]+@[A-Za-z0-9]+$""")
    private const val HEX = "0123456789ABCDEF"
    private const val BYTE_MASK = 0xFF
    private const val NIBBLE = 4
    private const val LOW_NIBBLE = 0x0F

    fun isValidVpa(vpa: String): Boolean = VPA.matches(vpa)

    /** Null when [vpa] is not a valid address or [amount] is not positive. */
    fun build(vpa: String, payeeName: String, amount: Money, note: String): String? {
        if (!isValidVpa(vpa) || amount.paise <= 0) return null
        return "upi://pay?pa=$vpa&pn=${encode(payeeName)}&am=${amount.toDecimalString()}&cu=INR&tn=${encode(note)}"
    }

    /** Percent-encodes everything except RFC 3986 unreserved characters, as UTF-8. */
    private fun encode(text: String): String = buildString {
        text.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and BYTE_MASK
            val char = value.toChar()
            if (value < BYTE_MASK / 2 && (char.isLetterOrDigit() || char in "-._~")) {
                append(char)
            } else {
                append('%').append(HEX[value shr NIBBLE]).append(HEX[value and LOW_NIBBLE])
            }
        }
    }
}
