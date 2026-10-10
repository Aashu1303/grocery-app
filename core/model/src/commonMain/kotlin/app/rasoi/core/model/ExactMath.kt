package app.rasoi.core.model

internal fun addExact(a: Long, b: Long): Long {
    val result = a + b
    if ((a xor result) and (b xor result) < 0) throw ArithmeticException("long overflow")
    return result
}

internal fun subtractExact(a: Long, b: Long): Long {
    val result = a - b
    if ((a xor b) and (a xor result) < 0) throw ArithmeticException("long overflow")
    return result
}

internal fun multiplyExact(a: Long, b: Long): Long {
    val result = a * b
    val overflows = (a != 0L && (result / a != b || (a == -1L && b == Long.MIN_VALUE))) ||
        (b == -1L && a == Long.MIN_VALUE)
    if (overflows) throw ArithmeticException("long overflow")
    return result
}
