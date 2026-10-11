package app.rasoi.core.testing

import app.rasoi.core.domain.ledger.LedgerRepository
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus

/** In-memory ledger; set [failure] to make every call fail. Closing locks the open ledger into pending settlements. */
class FakeLedgerRepository(var open: Ledger, var upiVpa: String? = null) : LedgerRepository {
    var failure: AppError? = null
    val closed = mutableMapOf<PeriodMonth, Ledger>()
    val paid = mutableListOf<SettlementId>()
    val requestedMonths = mutableListOf<PeriodMonth?>()

    override suspend fun ledger(householdId: HouseholdId, month: PeriodMonth?): AppResult<Ledger> = result {
        requestedMonths += month
        val found = if (month == null || month == open.month) open else closed[month]
        found?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.NotFound("period"))
    }

    override suspend fun closeMonth(householdId: HouseholdId, month: PeriodMonth): AppResult<Ledger> = result {
        if (month != open.month) return@result AppResult.Failure(AppError.Conflict("period_not_open"))
        val locked = open.copy(
            status = PeriodStatus.LOCKED,
            transfers = open.transfers.mapIndexed { index, transfer ->
                transfer.copy(settlementId = SettlementId("s$index"), status = SettlementStatus.PENDING)
            },
        )
        closed[month] = locked
        val next = month.next()
        open = Ledger(next, PeriodStatus.OPEN, emptyList(), emptyList(), 0, listOf(next) + open.months)
        AppResult.Success(locked)
    }

    override suspend fun markPaid(settlementId: SettlementId): AppResult<LedgerTransfer> = result {
        paid += settlementId
        val month =
            closed.entries.firstOrNull { (_, ledger) -> ledger.transfers.any { it.settlementId == settlementId } }
                ?: return@result AppResult.Failure(AppError.NotFound("settlement"))
        val updated = month.value.transfers.map {
            if (it.settlementId == settlementId) it.copy(status = SettlementStatus.PAID) else it
        }
        closed[month.key] = month.value.copy(transfers = updated)
        AppResult.Success(updated.first { it.settlementId == settlementId })
    }

    override suspend fun myUpiVpa(): AppResult<String?> = result { AppResult.Success(upiVpa) }

    override suspend fun setUpiVpa(vpa: String?): AppResult<String?> = result {
        upiVpa = vpa
        AppResult.Success(vpa)
    }

    private inline fun <T> result(block: () -> AppResult<T>): AppResult<T> =
        failure?.let { AppResult.Failure(it) } ?: block()
}
