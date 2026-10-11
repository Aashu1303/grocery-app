package app.rasoi.core.domain.bills

import app.rasoi.core.domain.split.LineAllocator
import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.BillWarning
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId
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
    fun everyBillButALockedOneCanBeEdited() {
        BillStatus.entries.filter { it != BillStatus.LOCKED }.forEach { status ->
            assertEquals(AppResult.Success(Unit), BillRules.ensureEditable(Builders.bill(status = status)))
        }
        assertEquals(
            AppResult.Failure(AppError.Conflict("bill_not_editable")),
            BillRules.ensureEditable(Builders.bill(status = BillStatus.LOCKED)),
        )
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

    @Test
    fun newLinesAreCategorizedFromTheirName() {
        val edit = BillEdit(
            lines = listOf(
                LineEdit(null, "Amul Paneer", 1, Money(9_000)),
                LineEdit(null, "Colgate Toothpaste", 1, Money(2_000)),
                LineEdit(null, "Mystery item", 1, Money(500), isFood = true),
            ),
        )
        var next = 0
        val result = BillRules.apply(Builders.bill(), edit, { BillLineId("n${next++}") }, { ChargeId("c") })
        val lines = assertIs<AppResult.Success<Bill>>(result).value.lines
        assertEquals(
            listOf(Category.DAIRY to true, Category.PERSONAL_CARE to false, Category.OTHER to true),
            lines.map { it.category to it.isFood },
        )
    }

    @Test
    fun editingALineKeepsItsCategoryAndOnlyChangesFoodWhenAsked() {
        val dairy = Builders.line(id = "l1", name = "Paneer").copy(category = Category.DAIRY, isFood = true)
        val bill = Builders.bill(lines = listOf(dairy))

        fun foodAfter(isFood: Boolean?): Pair<Category, Boolean> {
            val edit = BillEdit(lines = listOf(LineEdit(dairy.id, "Fresh paneer", 1, dairy.amount, isFood)))
            val result = BillRules.apply(bill, edit, { BillLineId("n") }, { ChargeId("c") })
            return assertIs<AppResult.Success<Bill>>(result).value.lines.single().let { it.category to it.isFood }
        }
        assertEquals(Category.DAIRY to true, foodAfter(null))
        assertEquals(Category.DAIRY to false, foodAfter(false))
    }

    private val meera = UserId("u1")
    private val aarav = UserId("u2")
    private val rice = Builders.line(id = "l1", name = "Rice", amount = 10_001, units = 2)

    private fun entries(vararg people: Pair<UserId, Int>) = people.map { (user, units) -> SplitEntry(user, units) }

    private fun split(mode: SplitMode, vararg people: Pair<UserId, Int>) = LineSplit(mode, entries(*people))

    private fun allocationsFor(split: LineSplit) =
        assertIs<AppResult.Success<List<Allocation>>>(LineAllocator.allocate(rice, split)).value

    private fun equalBill() = Builders.bill(lines = listOf(rice)).copy(
        allocations = allocationsFor(
            split(
                SplitMode.EQUAL,
                meera to 1,
                aarav to 1,
            ),
        ),
    )

    private fun applied(bill: Bill, edit: BillEdit) =
        BillRules.apply(bill, edit, { BillLineId("n") }, { ChargeId("c") })

    private fun appliedBill(bill: Bill, edit: BillEdit) = assertIs<AppResult.Success<Bill>>(applied(bill, edit)).value

    @Test
    fun anEditCanSetTheSplitOfALineAndThePayer() {
        val edit = BillEdit(payerId = aarav, splits = listOf(LineSplitEdit(rice.id, split(SplitMode.ONE, aarav to 1))))
        val bill = appliedBill(Builders.bill(lines = listOf(rice)), edit)
        assertEquals(aarav, bill.payerId)
        assertEquals(listOf(aarav to 10_001L), bill.allocations.map { it.userId to it.amount.paise })
    }

    @Test
    fun anInvalidSplitIsReportedByPosition() {
        val base = Builders.bill(lines = listOf(rice))
        val short = LineSplitEdit(rice.id, split(SplitMode.QUANTITY, meera to 1))
        assertEquals(
            listOf("splits[0]" to "units_mismatch"),
            violations(applied(base, BillEdit(splits = listOf(short)))),
        )
        val stray = LineSplitEdit(BillLineId("zzz"), split(SplitMode.ONE, meera to 1))
        assertEquals(listOf("splits[0]" to "unknown_line"), violations(applied(base, BillEdit(splits = listOf(stray)))))
    }

    @Test
    fun changingALinesAmountReallocatesItsExistingSplit() {
        val edit = BillEdit(lines = listOf(LineEdit(rice.id, "Rice", 2, Money(20_001))))
        assertEquals(listOf(10_001L, 10_000L), appliedBill(equalBill(), edit).allocations.map { it.amount.paise })
    }

    @Test
    fun aQuantitySplitIsDroppedWhenTheUnitCountNoLongerMatches() {
        val base = Builders.bill(lines = listOf(rice))
            .copy(allocations = allocationsFor(split(SplitMode.QUANTITY, meera to 1, aarav to 1)))
        val edit = BillEdit(lines = listOf(LineEdit(rice.id, "Rice", 3, rice.amount)))
        assertEquals(emptyList(), appliedBill(base, edit).allocations)
    }

    @Test
    fun removingALineRemovesItsAllocations() {
        val edit = BillEdit(lines = listOf(LineEdit(null, "Salt", 1, Money(500))))
        assertEquals(emptyList(), appliedBill(equalBill(), edit).allocations)
    }

    @Test
    fun aChargeKeepsItsDistributionUnlessTheEditChangesIt() {
        val bill = Builders.bill(charges = listOf(Builders.charge(id = "c1")))
        val keep = BillEdit(charges = listOf(ChargeEdit(ChargeId("c1"), ChargeKind.OTHER, "Fee", Money(500))))
        assertEquals(ChargeDistribution.PROPORTIONAL, appliedBill(bill, keep).charges.single().distribution)
        val change = ChargeEdit(ChargeId("c1"), ChargeKind.OTHER, "Fee", Money(500), ChargeDistribution.EQUAL)
        assertEquals(
            ChargeDistribution.EQUAL,
            appliedBill(bill, BillEdit(charges = listOf(change))).charges.single().distribution,
        )
    }

    @Test
    fun aPayerOrSplitAloneIsAValidEdit() {
        assertIs<AppResult.Success<BillEdit>>(BillRules.validate(BillEdit(payerId = aarav)))
        val only = BillEdit(splits = listOf(LineSplitEdit(rice.id, split(SplitMode.ONE, meera to 1))))
        assertIs<AppResult.Success<BillEdit>>(BillRules.validate(only))
    }
}
