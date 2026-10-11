package app.rasoi.server.service

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.ledger.DebtSimplifier
import app.rasoi.core.domain.ledger.PeriodBalances
import app.rasoi.core.domain.ledger.PeriodRules
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Period
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.Settlement
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.core.model.map
import app.rasoi.core.model.mapError
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.PeriodStore
import app.rasoi.server.persistence.UserStore
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * Settlement periods (design-plan 6.3, ADR 0022): every bill belongs to the household's open period; the balances of
 * its approved bills are settled by a few transfers when the month ends or an owner closes it early.
 */
class PeriodService(
    private val households: HouseholdService,
    private val periods: PeriodStore,
    private val bills: BillStore,
    private val users: UserStore,
    private val clock: Clock,
) {
    /** The open period new bills join; the first one is opened for the current month in the household's zone. */
    suspend fun current(householdId: HouseholdId): Period {
        periods.findOpen(householdId)?.let { return it }
        val today = PeriodRules.today(clock.now(), households.timeZone(householdId))
        val month = periods.list(householdId).firstOrNull()?.let { PeriodRules.successor(it.month, today) }
            ?: PeriodMonth.of(today)
        return periods.open(householdId, month, clock.now())
    }

    /** Balances and transfers of [month], or of the open period when it is null. */
    suspend fun ledger(userId: UserId, householdId: HouseholdId, month: PeriodMonth?): AppResult<Ledger> =
        households.authorize(userId, householdId, Permission.VIEW_LEDGER).flatMap {
            val period = if (month == null) current(householdId) else periods.findByMonth(householdId, month)
            period?.let { AppResult.Success(ledgerOf(it)) } ?: AppResult.Failure(AppError.NotFound("period"))
        }

    /** Closes the open period early; only owners may. */
    suspend fun close(userId: UserId, householdId: HouseholdId, month: PeriodMonth): AppResult<Ledger> =
        households.authorize(userId, householdId, Permission.CLOSE_PERIOD).flatMap {
            val open = current(householdId)
            if (open.month != month) {
                AppResult.Failure(AppError.Conflict("period_not_open"))
            } else {
                closePeriod(open, households.timeZone(householdId))
            }
        }

    /** Closes every open period whose month has ended in its household's time zone; returns how many. */
    suspend fun closeDue(): Int {
        val now = clock.now()
        return periods.openPeriods()
            .filter { (period, zone) -> PeriodRules.isDue(period.month, PeriodRules.today(now, zone)) }
            .count { (period, zone) -> closePeriod(period, zone) is AppResult.Success }
    }

    /** Marks a transfer paid; either side of it, or an owner, may. */
    suspend fun markPaid(userId: UserId, settlementId: SettlementId): AppResult<LedgerTransfer> {
        val settlement = periods.findSettlement(settlementId) ?: return notFound()
        val period = periods.find(settlement.periodId) ?: return notFound()
        return households.authorize(userId, period.householdId, Permission.SETTLE)
            .mapError { if (it is AppError.NotFound) AppError.NotFound("settlement") else it }
            .flatMap { role -> ensureParty(settlement, userId, role) }
            .map {
                val paid = periods.markPaid(settlementId, clock.now()) ?: settlement
                transferOf(paid)
            }
    }

    private suspend fun closePeriod(period: Period, zone: TimeZone): AppResult<Ledger> {
        val now = clock.now()
        val next = PeriodRules.successor(period.month, PeriodRules.today(now, zone))
        return periods.close(period, next, now).map { ledgerOf(it.closed) }
    }

    private suspend fun ledgerOf(period: Period): Ledger {
        val inPeriod = bills.list(period.householdId, period.id)
        val balances = PeriodBalances.of(inPeriod)
        val transfers = if (period.status == PeriodStatus.LOCKED) {
            periods.settlements(period.id).map { transferOf(it) }
        } else {
            DebtSimplifier.simplify(balances).map { LedgerTransfer(it, payeeUpiVpa = vpaOf(it)) }
        }
        val pending = inPeriod.count { it.status == BillStatus.PENDING || it.status == BillStatus.CHANGES_REQUESTED }
        return Ledger(period, balances, transfers, pending, periods.list(period.householdId).map { it.month })
    }

    private suspend fun transferOf(settlement: Settlement) = LedgerTransfer(
        transfer = settlement.transfer,
        settlementId = settlement.id,
        status = settlement.status,
        payeeUpiVpa = vpaOf(settlement.transfer),
    )

    private suspend fun vpaOf(transfer: Transfer): String? = users.find(transfer.to)?.upiVpa

    private fun ensureParty(settlement: Settlement, userId: UserId, role: Role): AppResult<Unit> =
        if (userId == settlement.transfer.from || userId == settlement.transfer.to || role == Role.OWNER) {
            AppResult.Success(Unit)
        } else {
            AppResult.Failure(AppError.Forbidden)
        }

    private fun notFound(): AppResult<LedgerTransfer> = AppResult.Failure(AppError.NotFound("settlement"))
}
