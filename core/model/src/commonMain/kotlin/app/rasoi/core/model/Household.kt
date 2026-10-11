package app.rasoi.core.model

import kotlinx.datetime.TimeZone

/** A shared flat. [timeZone] sets month boundaries for settlement. */
data class Household(val id: HouseholdId, val name: String, val timeZone: TimeZone)
