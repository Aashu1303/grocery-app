package app.rasoi.core.domain.ledger

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Balance
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.LedgerTransfer
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.Role
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import app.rasoi.core.model.Transfer
import app.rasoi.core.model.UserId
import app.rasoi.core.testing.FakeLedgerRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LedgerUseCasesTest {
    private val october = PeriodMonth(2026, 10)
    private val householdId = HouseholdId("h1")
    private val owed = Transfer(UserId("u2"), UserId("u1"), Money(15_000))
    private val open = Ledger(
        october,
        PeriodStatus.OPEN,
        listOf(Balance(UserId("u1"), Money(15_000)), Balance(UserId("u2"), Money(-15_000))),
        listOf(LedgerTransfer(owed, payeeUpiVpa = "meera@okbank")),
        pendingBills = 1,
        months = listOf(october),
    )
    private val repository = FakeLedgerRepository(open)

    @Test
    fun cooksNeverSeeTheLedger() = runTest {
        val get = GetLedgerUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), get(householdId, Role.COOK))
        assertEquals(AppResult.Success(open), get(householdId, Role.MEMBER))
        assertEquals(listOf<PeriodMonth?>(null), repository.requestedMonths)
    }

    @Test
    fun onlyOwnersCloseAMonthAndTheNextOneOpens() = runTest {
        val close = CloseMonthUseCase(repository)
        assertEquals(AppResult.Failure(AppError.Forbidden), close(householdId, Role.MEMBER, october))
        assertEquals(
            AppResult.Failure(AppError.Conflict("period_not_open")),
            close(householdId, Role.OWNER, PeriodMonth(2026, 9)),
        )
        val closed = assertIs<AppResult.Success<Ledger>>(close(householdId, Role.OWNER, october)).value
        assertEquals(PeriodStatus.LOCKED, closed.status)
        assertEquals(SettlementStatus.PENDING, closed.transfers.single().status)
        assertEquals(PeriodMonth(2026, 11), repository.open.month)
    }

    @Test
    fun markingATransferPaidReturnsItAsPaid() = runTest {
        CloseMonthUseCase(repository)(householdId, Role.OWNER, october)
        val paid = MarkPaidUseCase(repository)(SettlementId("s0"))
        assertEquals(SettlementStatus.PAID, (paid as AppResult.Success).value.status)
    }

    @Test
    fun theUpiAddressIsCheckedTrimmedAndCanBeCleared() = runTest {
        val update = UpdateUpiUseCase(repository)
        assertEquals(
            AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("upiVpa", "invalid")))),
            update("not a vpa"),
        )
        assertEquals(null, repository.upiVpa)
        assertEquals(AppResult.Success("meera@okbank"), update("  meera@okbank "))
        assertEquals(AppResult.Success(null), update("   "))
        assertEquals(null, repository.upiVpa)
    }
}
