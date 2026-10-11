package app.rasoi.core.model

/** A period takes new bills while OPEN; once LOCKED its approved bills and settlements are final. */
enum class PeriodStatus { OPEN, LOCKED }
