package app.rasoi.core.model

data class Location(val id: LocationId, val householdId: HouseholdId, val name: String, val kind: LocationKind)
