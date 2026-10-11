package app.rasoi.server.persistence

import app.rasoi.core.domain.household.DefaultLocation
import app.rasoi.core.model.Household
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Member
import app.rasoi.core.model.Role
import app.rasoi.core.model.UserId
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.time.Instant
import kotlin.uuid.Uuid

class HouseholdStore(private val db: RasoiDatabase) {
    /** Creates the household, its owner membership and default locations in one transaction. */
    suspend fun create(
        name: String,
        timeZone: TimeZone,
        owner: UserId,
        locations: List<DefaultLocation>,
        now: Instant,
    ): Household = db.query {
        val id = Uuid.random()
        HouseholdsTable.insert {
            it[HouseholdsTable.id] = id
            it[HouseholdsTable.name] = name
            it[HouseholdsTable.timeZone] = timeZone.id
            it[createdAt] = now
        }
        insertMembership(id, owner.uuid(), Role.OWNER, now)
        LocationsTable.batchInsert(locations) { location ->
            this[LocationsTable.id] = Uuid.random()
            this[LocationsTable.householdId] = id
            this[LocationsTable.name] = location.name
            this[LocationsTable.kind] = location.kind.name
        }
        Household(id.toHouseholdId(), name, timeZone)
    }

    suspend fun find(id: HouseholdId): Household? = db.query {
        HouseholdsTable.selectAll().where { HouseholdsTable.id eq id.uuid() }.singleOrNull()?.toHousehold()
    }

    suspend fun roleOf(householdId: HouseholdId, userId: UserId): Role? = db.query {
        MembershipsTable.selectAll()
            .where {
                (MembershipsTable.householdId eq householdId.uuid()) and (MembershipsTable.userId eq userId.uuid())
            }
            .singleOrNull()
            ?.let { Role.valueOf(it[MembershipsTable.role]) }
    }

    suspend fun householdsOf(userId: UserId): List<Pair<Household, Role>> = db.query {
        HouseholdsTable.join(MembershipsTable, JoinType.INNER, HouseholdsTable.id, MembershipsTable.householdId)
            .selectAll()
            .where { MembershipsTable.userId eq userId.uuid() }
            .orderBy(HouseholdsTable.createdAt to SortOrder.ASC)
            .map { it.toHousehold() to Role.valueOf(it[MembershipsTable.role]) }
    }

    suspend fun members(householdId: HouseholdId): List<Member> = db.query {
        MembershipsTable.join(UsersTable, JoinType.INNER, MembershipsTable.userId, UsersTable.id)
            .selectAll()
            .where { MembershipsTable.householdId eq householdId.uuid() }
            .orderBy(MembershipsTable.joinedAt to SortOrder.ASC)
            .map {
                Member(
                    it[UsersTable.id].toUserId(),
                    it[UsersTable.displayName],
                    Role.valueOf(it[MembershipsTable.role]),
                )
            }
    }
}

internal fun insertMembership(householdId: Uuid, userId: Uuid, role: Role, now: Instant) {
    MembershipsTable.insert {
        it[MembershipsTable.householdId] = householdId
        it[MembershipsTable.userId] = userId
        it[MembershipsTable.role] = role.name
        it[joinedAt] = now
    }
}

private fun ResultRow.toHousehold() = Household(
    id = this[HouseholdsTable.id].toHouseholdId(),
    name = this[HouseholdsTable.name],
    timeZone = TimeZone.of(this[HouseholdsTable.timeZone]),
)
