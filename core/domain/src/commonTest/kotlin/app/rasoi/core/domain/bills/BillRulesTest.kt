package app.rasoi.core.domain.bills

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.testing.Builders
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BillRulesTest {
    private fun violations(result: AppResult<*>): List<Pair<String, String>> =
        ((result as AppResult.Failure).error as AppError.Validation).violations.map { it.field to it.code }

    @Test
    fun aReconciledTemplateBillHasNoWarnings() {
        val bill = Builders.bill(lines = listOf(Builders.line()), charges = listOf(Builders.charge(amount = -500)))
        assertEquals(emptyList(), BillRules.warnings(bill))
    }

    @Test
    fun reportsATotalMismatchWithBothAmounts() {
        val bill = Builders.bill(lines = listOf(Builders.line(amount = 4_000)), total = Money(4_100))
        assertEquals(
            listOf<BillWarning>(BillWarning.TotalMismatch(Money(4_000), Money(4_100))),
            BillRules.warnings(bill),
        )
    }

    @Test
    fun flagsBillsReadByTheFallback() {
        val bill = Builders.bill(confidence = ImportConfidence.LOW, total = Money(1))
        assertEquals(
            listOf(BillWarning.TotalMismatch(Money(4_000), Money(1)), BillWarning.ParsedByFallback),
            BillRules.warnings(bill),
        )
    }

    @Test
    fun onlyDraftsCanBeEditedInThisMilestone() {
        assertEquals(AppResult.Success(Unit), BillRules.ensureEditable(Builders.bill(status = BillStatus.DRAFT)))
        BillStatus.entries.filter { it != BillStatus.DRAFT }.forEach { status ->
            assertEquals(
                AppResult.Failure(AppError.Conflict("bill_not_editable")),
                BillRules.ensureEditable(Builders.bill(status = status)),
            )
        }
    }

    @Test
    fun acceptsAValidEdit() {
        val edit = BillEdit(
            lines = listOf(
                LineEdit(BillLineId("line1"), "Red onion", 2, Money(4_000)),
                LineEdit(null, "Free bag", 1, Money.ZERO),
            ),
            charges = listOf(ChargeEdit(ChargeId("c1"), ChargeKind.DELIVERY, "Delivery", Money(-1_000))),
            total = Money(3_000),
        )
        assertEquals(AppResult.Success(edit), BillRules.validate(edit))
    }

    @Test
    fun reportsEveryInvalidFieldByPosition() {
        val edit = BillEdit(
            lines = listOf(
                LineEdit(null, " ", 1, Money(100)),
                LineEdit(null, "Tea", 0, Money(-1)),
                LineEdit(null, "x".repeat(BillRules.MAX_NAME_LENGTH + 1), BillRules.MAX_UNITS + 1, Money(1)),
            ),
            charges = listOf(ChargeEdit(null, ChargeKind.OTHER, "", Money(1))),
            total = Money(-5),
        )
        assertEquals(
            listOf(
                "lines[0].name" to "required",
                "lines[1].units" to "out_of_range",
                "lines[1].amount" to "negative",
                "lines[2].name" to "too_long",
                "lines[2].units" to "out_of_range",
                "charges[0].label" to "required",
                "total" to "negative",
            ),
            violations(BillRules.validate(edit)),
        )
    }

    @Test
    fun aBillNeedsAtLeastOneLineAndAtMostTheLimit() {
        assertEquals(listOf("lines" to "required"), violations(BillRules.validate(BillEdit(lines = emptyList()))))
        val tooMany = List(BillRules.MAX_LINES + 1) { LineEdit(null, "Item", 1, Money(1)) }
        assertEquals(listOf("lines" to "too_many"), violations(BillRules.validate(BillEdit(lines = tooMany))))
    }

    @Test
    fun anEmptyEditIsRejected() {
        assertEquals(listOf("body" to "empty"), violations(BillRules.validate(BillEdit())))
    }

    @Test
    fun applyingAnEditKeepsIdsAndAssignsNewOnes() {
        val bill = Builders.bill(
            lines = listOf(Builders.line(id = "line1", name = "Onion"), Builders.line(id = "line2", name = "Tea")),
            charges = listOf(Builders.charge(id = "c1")),
        )
        val edit = BillEdit(
            lines = listOf(
                LineEdit(BillLineId("line2"), "Assam tea", 2, Money(9_000)),
                LineEdit(null, "Salt", 1, Money(2_000)),
            ),
            total = Money(11_500),
        )
        var next = 0
        val updated = BillRules.apply(bill, edit, { BillLineId("new${next++}") }, { ChargeId("x") })
        val value = assertIs<AppResult.Success<Bill>>(updated).value
        assertEquals(
            listOf("line2" to "Assam tea", "new0" to "Salt"),
            value.lines.map { it.id.value to it.displayName },
        )
        assertEquals("Tea", value.lines.first().rawName)
        assertEquals("Salt", value.lines.last().rawName)
        assertEquals(bill.charges, value.charges)
        assertEquals(Money(11_500), value.total)
    }

    @Test
    fun applyingAnEditWithAForeignIdFails() {
        val edit = BillEdit(lines = listOf(LineEdit(BillLineId("elsewhere"), "Tea", 1, Money(1))))
        val result = BillRules.apply(Builders.bill(), edit, { BillLineId("n") }, { ChargeId("n") })
        assertEquals(listOf("lines[0].id" to "unknown"), violations(result))
        val charge = BillEdit(charges = listOf(ChargeEdit(ChargeId("nope"), ChargeKind.OTHER, "Fee", Money(1))))
        val chargeResult = BillRules.apply(Builders.bill(), charge, { BillLineId("n") }, { ChargeId("n") })
        assertEquals(listOf("charges[0].id" to "unknown"), violations(chargeResult))
    }
}
