package app.rasoi.server.http

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeLoggingTest {
    @Test
    fun logLineKeepsTypesAndFramesButDropsMessages() {
        val cause = IllegalStateException("Key (phone)=(+919812345678) already exists")
        val error = RuntimeException("wrapped +919812345678", cause)
        val line = describeForLog(error)
        assertTrue(line.startsWith("java.lang.RuntimeException"))
        assertTrue(line.contains("caused by java.lang.IllegalStateException"))
        assertTrue(line.contains("at "))
        assertFalse(line.contains("9812345678"))
    }
}
