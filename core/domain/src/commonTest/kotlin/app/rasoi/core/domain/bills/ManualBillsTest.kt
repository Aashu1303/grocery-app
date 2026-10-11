package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.UserId
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ManualBillsTest {
    private val meera = UserId("u1")
    private var counter = 0

    private fun draft(
        title: String = "Gas cylinder",
        lines: List<LineEdit> = listOf(LineEdit(null, "Cylinder", 1, Money(95_000))),
        charges: List<ChargeEdit> = emptyList(),
    ) = ManualBillDraft(title, LocalDate(2026, 10, 3), meera, lines, charges)

    private fun create(draft: ManualBillDraft): AppResult<Bill> = ManualBills.create(
        id = BillId("b1"),
        householdId = HouseholdId("h1"),
        creator = meera,
        draft = draft,
        today = LocalDate(2026, 10, 11),
        newLineId = { BillLineId("line${++counter}") },
        newChargeId = { ChargeId("charge${++counter}") },
    )

    private fun violations(result: AppResult<*>) =
        ((result as AppResult.Failure).error as AppError.Validation).violations.map { it.field to it.code }

    @Test
    fun buildsADraftWhoseTotalIsLinesPlusCharges() {
        val bill = assertIs<AppResult.Success<Bill>>(
            create(
                draft(
                    lines = listOf(
                        LineEdit(null, "Cylinder", 1, Money(95_000)),
                        LineEdit(null, "Regulator", 1, Money(5_000)),
                    ),
                    charges = listOf(ChargeEdit(null, ChargeKind.DELIVERY, "Delivery", Money(2_000))),
                ),
            ),
        ).value
        assertEquals(Money(102_000), bill.total)
        assertEquals(BillStatus.DRAFT, bill.status)
        assertEquals(Platform.MANUAL, bill.platform)
        assertEquals("Gas cylinder", bill.title)
        assertEquals(meera, bill.createdBy)
        assertEquals(meera, bill.payerId)
        assertEquals(ImportConfidence.HIGH, bill.confidence)
        assertEquals(emptyList(), BillRules.warnings(bill))
    }

    @Test
    fun usesTodayWhenNoDateIsGiven() {
        val bill = (create(draft().copy(invoiceDate = null)) as AppResult.Success).value
        assertEquals(LocalDate(2026, 10, 11), bill.invoiceDate)
        assertEquals(LocalDate(2026, 10, 3), (create(draft()) as AppResult.Success).value.invoiceDate)
    }

    @Test
    fun givesEveryManualBillItsOwnInvoiceNumber() {
        val bill = (create(draft()) as AppResult.Success).value
        assertEquals("manual-b1", bill.invoiceNumber)
    }

    @Test
    fun trimsTheTitleAndRejectsABlankOne() {
        assertEquals("Rent", ((create(draft(title = "  Rent ")) as AppResult.Success).value).title)
        assertEquals(listOf("title" to "required"), violations(create(draft(title = " "))))
    }

    @Test
    fun needsAtLeastOneLine() {
        assertEquals(listOf("lines" to "required"), violations(create(draft(lines = emptyList()))))
    }

    @Test
    fun reportsInvalidLinesByPosition() {
        val result = create(draft(lines = listOf(LineEdit(null, " ", 0, Money(100)))))
        assertEquals(
            listOf("lines[0].name" to "required", "lines[0].units" to "out_of_range"),
            violations(result),
        )
    }

    @Test
    fun rejectsABillThatTotalsLessThanZero() {
        val result = create(draft(charges = listOf(ChargeEdit(null, ChargeKind.DISCOUNT, "Promo", Money(-96_000)))))
        assertEquals(listOf("total" to "negative"), violations(result))
    }
}
