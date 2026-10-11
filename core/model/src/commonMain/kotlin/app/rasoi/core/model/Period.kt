package app.rasoi.core.model

/** One settlement period of a household (design-plan 6.2), labelled by [month]. */
data class Period(val id: PeriodId, val householdId: HouseholdId, val month: PeriodMonth, val status: PeriodStatus)
