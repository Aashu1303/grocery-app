package app.rasoi.server.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SessionTokensTest {
    private val tokens = SessionTokens()

    @Test
    fun tokensAreLongUrlSafeAndUnique() {
        val all = List(1_000) { tokens.newToken() }
        assertEquals(1_000, all.toSet().size)
        assertTrue(all.all { it.length == 43 && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } })
    }

    @Test
    fun hashIsStableHexAndDiffersFromTheToken() {
        val token = tokens.newToken()
        assertEquals(tokens.hash(token), tokens.hash(token))
        assertEquals(64, tokens.hash(token).length)
        assertNotEquals(token, tokens.hash(token))
    }
}
