package app.rasoi.server.persistence

import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.UserId
import kotlin.uuid.Uuid

internal fun UserId.uuid(): Uuid = Uuid.parse(value)

internal fun HouseholdId.uuid(): Uuid = Uuid.parse(value)

internal fun Uuid.toUserId() = UserId(toString())

internal fun Uuid.toHouseholdId() = HouseholdId(toString())

internal fun Uuid.toLocationId() = LocationId(toString())

/** Parses an id from a URL path; null when it is not a UUID, which callers treat as "not found". */
internal fun parseUuid(raw: String): Uuid? = runCatching { Uuid.parse(raw) }.getOrNull()
