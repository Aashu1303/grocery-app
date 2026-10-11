package app.rasoi.core.data

import app.rasoi.core.contract.ApiHeaders
import app.rasoi.core.contract.ContractJson
import app.rasoi.core.contract.auth.UpdateProfileRequestDto
import app.rasoi.core.data.repository.DefaultLedgerRepository
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Ledger
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.core.model.PeriodStatus
import app.rasoi.core.model.SettlementId
import app.rasoi.core.model.SettlementStatus
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LedgerRepositoryTest {
    private val ledgerJson = """
        {"month":"2026-10","status":"LOCKED",
         "balances":[{"userId":"u1","netPaise":15000},{"userId":"u2","netPaise":-15000}],
         "transfers":[{"from":"u2","to":"u1","amountPaise":15000,"settlementId":"s1","status":"PENDING",
                       "payeeUpiVpa":"meera@okbank"}],
         "pendingBills":2,"months":["2026-11","2026-10"]}
    """.trimIndent()

    @Test
    fun readsALedgerForAMonthOrTheOpenPeriod() = runTest {
        val server = MockServer { json(ledgerJson) }
        val repository = DefaultLedgerRepository(server.api)
        val ledger = (repository.ledger(HouseholdId("h1"), PeriodMonth(2026, 10)) as AppResult.Success<Ledger>).value
        assertEquals(PeriodStatus.LOCKED, ledger.status)
        assertEquals(Money(-15_000), ledger.balances.last().net)
        val transfer = ledger.transfers.single()
        assertEquals(Money(15_000), transfer.transfer.amount)
        assertEquals(SettlementId("s1") to SettlementStatus.PENDING, transfer.settlementId to transfer.status)
        assertEquals("meera@okbank", transfer.payeeUpiVpa)
        assertEquals(listOf(PeriodMonth(2026, 11), PeriodMonth(2026, 10)), ledger.months)
        assertEquals(2, ledger.pendingBills)

        repository.ledger(HouseholdId("h1"), null)
        assertEquals("https://api.rasoi.test/households/h1/ledger?period=2026-10", server.requests[0].url.toString())
        assertEquals("https://api.rasoi.test/households/h1/ledger", server.requests[1].url.toString())
    }

    @Test
    fun closingAndMarkingPaidArePostsWithIdempotencyKeys() = runTest {
        val server = MockServer {
            if (it.url.encodedPath.endsWith("/paid")) {
                json("""{"from":"u2","to":"u1","amountPaise":15000,"settlementId":"s1","status":"PAID"}""")
            } else {
                json(ledgerJson)
            }
        }
        val repository = DefaultLedgerRepository(server.api)
        assertIs<AppResult.Success<Ledger>>(repository.closeMonth(HouseholdId("h1"), PeriodMonth(2026, 10)))
        val paid = repository.markPaid(SettlementId("s1"))
        assertEquals(SettlementStatus.PAID, (paid as AppResult.Success).value.status)
        val (close, pay) = server.requests
        assertEquals(
            HttpMethod.Post to "https://api.rasoi.test/households/h1/periods/2026-10/close",
            close.method to close.url.toString(),
        )
        assertEquals("https://api.rasoi.test/settlements/s1/paid", pay.url.toString())
        assertEquals("key-1", close.headers[ApiHeaders.IDEMPOTENCY_KEY])
    }

    @Test
    fun theUpiAddressIsReadFromMeAndPatched() = runTest {
        val server = MockServer { json("""{"id":"u1","displayName":"Meera","upiVpa":"meera@okbank"}""") }
        val repository = DefaultLedgerRepository(server.api)
        assertEquals(AppResult.Success("meera@okbank"), repository.setUpiVpa("meera@okbank"))
        val request = server.requests.single()
        assertEquals(HttpMethod.Patch to "https://api.rasoi.test/me", request.method to request.url.toString())
        val sent = ContractJson.decodeFromString(
            UpdateProfileRequestDto.serializer(),
            (request.body as TextContent).text,
        )
        assertEquals("meera@okbank", sent.upiVpa)
    }

    @Test
    fun aClosedMonthComesBackAsAConflict() = runTest {
        val server = MockServer { json("""{"code":"period_not_open","message":"x"}""", HttpStatusCode.Conflict) }
        val result = DefaultLedgerRepository(server.api).closeMonth(HouseholdId("h1"), PeriodMonth(2026, 9))
        assertEquals(AppResult.Failure(AppError.Conflict("period_not_open")), result)
    }
}
