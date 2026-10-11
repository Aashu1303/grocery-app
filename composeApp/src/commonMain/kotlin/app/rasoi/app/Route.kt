package app.rasoi.app

import kotlinx.serialization.Serializable

/** Type-safe navigation routes. */
@Serializable
sealed interface Route {
    @Serializable
    data object Kitchen : Route

    @Serializable
    data object Pantry : Route

    @Serializable
    data object Bills : Route

    @Serializable
    data object Balances : Route

    @Serializable
    data object Catalog : Route

    @Serializable
    data object Household : Route

    @Serializable
    data object ImportInvoices : Route

    @Serializable
    data class BillReview(val billId: String) : Route
}
