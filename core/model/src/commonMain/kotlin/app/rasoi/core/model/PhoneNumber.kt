package app.rasoi.core.model

import kotlin.jvm.JvmInline

/** A phone number in E.164 form, as Firebase phone auth reports it. */
@JvmInline
value class PhoneNumber private constructor(val e164: String) {
    /** For logs and support screens: country code and last four digits only. */
    fun masked(): String {
        val country = if (e164.startsWith(INDIA)) INDIA else e164.take(2)
        return "$country •••••• ${e164.takeLast(VISIBLE_DIGITS)}"
    }

    override fun toString(): String = masked()

    companion object {
        private const val INDIA = "+91"
        private const val VISIBLE_DIGITS = 4
        private val E164 = Regex("""^\+[1-9]\d{7,14}$""")
        private val INDIAN_MOBILE = Regex("""^[6-9]\d{9}$""")

        /** Accepts E.164 input, or a 10-digit Indian mobile number with an optional leading 0. */
        fun parse(input: String): PhoneNumber? {
            val compact = input.filterNot { it == ' ' || it == '-' || it == '(' || it == ')' }
            val local = compact.removePrefix("0")
            return when {
                E164.matches(compact) && compact.isPlausibleIndianOrForeign() -> PhoneNumber(compact)
                INDIAN_MOBILE.matches(local) -> PhoneNumber(INDIA + local)
                else -> null
            }
        }

        private fun String.isPlausibleIndianOrForeign() =
            !startsWith(INDIA) || INDIAN_MOBILE.matches(removePrefix(INDIA))
    }
}
