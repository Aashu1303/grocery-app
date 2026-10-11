package app.rasoi.core.domain.household

import app.rasoi.core.model.InviteCode
import kotlin.random.Random

/** Generates short invite codes from an alphabet without look-alike characters (0/O, 1/I). */
class InviteCodeGenerator(private val random: Random) {
    fun next(): InviteCode =
        InviteCode(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }.concatToString())

    companion object {
        const val LENGTH = 8
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** Accepts codes typed with spaces, dashes or lower case; null if it cannot be a valid code. */
        fun normalise(input: String): InviteCode? {
            val code = input.filter { it.isLetterOrDigit() }.uppercase()
            return code.takeIf { it.length == LENGTH && it.all { char -> char in ALPHABET } }?.let(::InviteCode)
        }
    }
}
