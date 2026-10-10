package app.rasoi.server.persistence

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationKind
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.uuid.Uuid

class LocationStore(private val db: RasoiDatabase) {
    suspend fun list(householdId: HouseholdId): List<Location> = db.query {
        LocationsTable.selectAll()
            .where { LocationsTable.householdId eq householdId.uuid() }
            .orderBy(LocationsTable.name to SortOrder.ASC)
            .map {
                Location(
                    id = it[LocationsTable.id].toLocationId(),
                    householdId = householdId,
                    name = it[LocationsTable.name],
                    kind = LocationKind.valueOf(it[LocationsTable.kind]),
                )
            }
    }

    suspend fun create(householdId: HouseholdId, name: String, kind: LocationKind): AppResult<Location> = db.query {
        val duplicate = LocationsTable.selectAll()
            .where {
                (LocationsTable.householdId eq householdId.uuid()) and
                    (LocationsTable.name.lowerCase() eq name.lowercase())
            }
            .any()
        if (duplicate) return@query AppResult.Failure(AppError.Conflict("location_exists"))
        val id = Uuid.random()
        LocationsTable.insert {
            it[LocationsTable.id] = id
            it[LocationsTable.householdId] = householdId.uuid()
            it[LocationsTable.name] = name
            it[LocationsTable.kind] = kind.name
        }
        AppResult.Success(Location(id.toLocationId(), householdId, name, kind))
    }
}
