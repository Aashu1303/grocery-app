package app.rasoi.server.persistence

import app.rasoi.core.domain.ledger.DebtSimplifier
import app.rasoi.core.domain.ledger.PeriodBalances
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Money
import app.rasoi.core.model.Period
import app.rasoi.core.model.PeriodId
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Settlement
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer
import kotlinx.datetime.TimeZone
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** What closing a period produced: the locked period, the one that carries on, and the transfers that settle it. */
data class ClosedPeriod(val closed: Period, val next: Period, val settlements: List<Settlement>)

class PeriodStore(private val db: RasoiDatabase) {
    /** The household's open period, created for [month] when it has none. */
    suspend fun open(householdId: HouseholdId, month: PeriodMonth, now: Instant): Period {
        findOpen(householdId)?.let { return it }
        try {
            db.query {
                PeriodsTable.insert {
                    it[id] = Uuid.random()
                    it[PeriodsTable.householdId] = householdId.uuid()
                    it[PeriodsTable.month] = month.toString()
                    it[status] = PeriodStatus.OPEN.name
                    it[createdAt] = now
                }
            }
        } catch (error: ExposedSQLException) {
            // Another request opened the period first; that one wins.
            if (error.sqlState != UNIQUE_VIOLATION) throw error
        }
        return requireNotNull(findOpen(householdId)) { "No open period after creating one" }
    }

    suspend fun findOpen(householdId: HouseholdId): Period? = db.query {
        PeriodsTable.selectAll()
            .where {
                (PeriodsTable.householdId eq householdId.uuid()) and
                    (PeriodsTable.status eq PeriodStatus.OPEN.name)
            }
            .singleOrNull()?.toPeriod()
    }

    suspend fun find(id: PeriodId): Period? = db.query {
        PeriodsTable.selectAll().where { PeriodsTable.id eq id.uuid() }.singleOrNull()?.toPeriod()
    }

    suspend fun findByMonth(householdId: HouseholdId, month: PeriodMonth): Period? = db.query {
        PeriodsTable.selectAll()
            .where { (PeriodsTable.householdId eq householdId.uuid()) and (PeriodsTable.month eq month.toString()) }
            .singleOrNull()?.toPeriod()
    }

    /** The household's periods, newest month first. */
    suspend fun list(householdId: HouseholdId): List<Period> = db.query {
        PeriodsTable.selectAll()
            .where { PeriodsTable.householdId eq householdId.uuid() }
            .orderBy(PeriodsTable.month to SortOrder.DESC)
            .map { it.toPeriod() }
    }

    /** Every open period with its household's time zone, for the job that closes months that have ended. */
    suspend fun openPeriods(): List<Pair<Period, TimeZone>> = db.query {
        PeriodsTable.join(HouseholdsTable, JoinType.INNER, PeriodsTable.householdId, HouseholdsTable.id)
            .selectAll()
            .where { PeriodsTable.status eq PeriodStatus.OPEN.name }
            .map { it.toPeriod() to TimeZone.of(it[HouseholdsTable.timeZone]) }
    }

    suspend fun settlements(periodId: PeriodId): List<Settlement> = db.query {
        SettlementsTable.selectAll().where { SettlementsTable.periodId eq periodId.uuid() }
            .orderBy(SettlementsTable.amountPaise to SortOrder.DESC, SettlementsTable.id to SortOrder.ASC)
            .map { it.toSettlement() }
    }

    suspend fun findSettlement(id: SettlementId): Settlement? = db.query {
        SettlementsTable.selectAll().where { SettlementsTable.id eq id.uuid() }.singleOrNull()?.toSettlement()
    }

    /** Marks a pending transfer paid; a transfer that is already paid is returned as it is. */
    suspend fun markPaid(id: SettlementId, now: Instant): Settlement? = db.query {
        SettlementsTable.update({ (SettlementsTable.id eq id.uuid()) and (SettlementsTable.status eq PENDING) }) {
            it[status] = SettlementStatus.PAID.name
            it[paidAt] = now
        }
        SettlementsTable.selectAll().where { SettlementsTable.id eq id.uuid() }.singleOrNull()?.toSettlement()
    }

    /**
     * Closes [period] in one transaction: locks it, locks its approved bills, moves every other bill to the next
     * period (opened for [nextMonth]) and stores the simplified transfers that settle the approved bills. Rows of
     * the bills are locked and their versions bumped, so a save that read an older copy fails instead of undoing it.
     */
    suspend fun close(period: Period, nextMonth: PeriodMonth, now: Instant): AppResult<ClosedPeriod> = db.query {
        val claimed = PeriodsTable.update({ (PeriodsTable.id eq period.id.uuid()) and (PeriodsTable.status eq OPEN) }) {
            it[status] = PeriodStatus.LOCKED.name
            it[closedAt] = now
        }
        if (claimed == 0) return@query AppResult.Failure(AppError.Conflict("period_closed"))
        val nextId = Uuid.random()
        PeriodsTable.insert {
            it[id] = nextId
            it[householdId] = period.householdId.uuid()
            it[month] = nextMonth.toString()
            it[status] = PeriodStatus.OPEN.name
            it[createdAt] = now
        }
        val rows = BillsTable.selectAll().where { BillsTable.periodId eq period.id.uuid() }.forUpdate().toList()
        val bills = loadBills(rows)
        val (approved, others) = bills.partition { it.status == BillStatus.APPROVED }
        touch(approved.map { it.id.uuid() }, now) {
            it[status] = BillStatus.LOCKED.name
        }
        touch(others.map { it.id.uuid() }, now) {
            it[periodId] = nextId
        }
        val transfers = DebtSimplifier.simplify(PeriodBalances.of(approved))
        val settlements = transfers.map {
            Settlement(Uuid.random().toSettlementId(), period.id, it, SettlementStatus.PENDING)
        }
        SettlementsTable.batchInsert(settlements) { settlement ->
            this[SettlementsTable.id] = settlement.id.uuid()
            this[SettlementsTable.periodId] = period.id.uuid()
            this[SettlementsTable.fromUser] = settlement.transfer.from.uuid()
            this[SettlementsTable.toUser] = settlement.transfer.to.uuid()
            this[SettlementsTable.amountPaise] = settlement.transfer.amount.paise
            this[SettlementsTable.status] = settlement.status.name
        }
        val next = Period(nextId.toPeriodId(), period.householdId, nextMonth, PeriodStatus.OPEN)
        AppResult.Success(ClosedPeriod(period.copy(status = PeriodStatus.LOCKED), next, settlements))
    }

    private fun touch(
        ids: List<Uuid>,
        now: Instant,
        change: BillsTable.(org.jetbrains.exposed.v1.core.statements.UpdateStatement) -> Unit,
    ) {
        if (ids.isEmpty()) return
        BillsTable.update({ BillsTable.id inList ids }) {
            it[version] = BillsTable.version + 1
            it[updatedAt] = now
            BillsTable.change(it)
        }
    }

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
        const val OPEN = "OPEN"
        const val PENDING = "PENDING"
    }
}

private fun ResultRow.toPeriod() = Period(
    id = this[PeriodsTable.id].toPeriodId(),
    householdId = this[PeriodsTable.householdId].toHouseholdId(),
    month = requireNotNull(PeriodMonth.parse(this[PeriodsTable.month])) { "Stored month is not YYYY-MM" },
    status = PeriodStatus.valueOf(this[PeriodsTable.status]),
)

private fun ResultRow.toSettlement() = Settlement(
    id = this[SettlementsTable.id].toSettlementId(),
    periodId = this[SettlementsTable.periodId].toPeriodId(),
    transfer = Transfer(
        from = this[SettlementsTable.fromUser].toUserId(),
        to = this[SettlementsTable.toUser].toUserId(),
        amount = Money(this[SettlementsTable.amountPaise]),
    ),
    status = SettlementStatus.valueOf(this[SettlementsTable.status]),
)
