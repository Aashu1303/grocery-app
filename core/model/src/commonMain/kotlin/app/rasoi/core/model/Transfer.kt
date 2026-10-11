package app.rasoi.core.model

/** [from] pays [to] [amount] to settle up. */
data class Transfer(val from: UserId, val to: UserId, val amount: Money)
