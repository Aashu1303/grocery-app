package app.rasoi.server.demo

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.ApprovalStateDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillLineEditDto
import app.rasoi.core.contract.bills.BillStatusDto
import app.rasoi.core.contract.bills.BillWarningCodeDto
import app.rasoi.core.contract.bills.CategoryDto
import app.rasoi.core.contract.bills.CreateBillRequestDto
import app.rasoi.core.contract.bills.DecideApprovalRequestDto
import app.rasoi.core.contract.bills.LineSplitEditDto
import app.rasoi.core.contract.bills.PlatformDto
import app.rasoi.core.contract.bills.QuantityDto
import app.rasoi.core.contract.bills.QuantityUnitDto
import app.rasoi.core.contract.bills.SplitEntryDto
import app.rasoi.core.contract.bills.SplitModeDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.RoleDto
import app.rasoi.core.contract.ledger.LedgerDto
import app.rasoi.core.contract.ledger.PeriodStatusDto
import app.rasoi.core.contract.ledger.SettlementStatusDto
import app.rasoi.core.contract.ledger.TransferDto
import app.rasoi.core.contract.pantry.CreatePantryItemRequestDto
import app.rasoi.core.contract.pantry.PantryItemDto
import app.rasoi.core.contract.pantry.PantryStatusDto
import app.rasoi.core.contract.pantry.UpdatePantryItemRequestDto
import app.rasoi.core.domain.ledger.UpiLink
import app.rasoi.core.model.Money
import app.rasoi.core.model.PeriodMonth
import app.rasoi.server.support.Invoices
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** People and the household the demo creates; later milestones extend the scenario with bills and pantry. */
data class DemoHousehold(
    val owner: SessionDto,
    val flatmate: SessionDto,
    val cook: SessionDto,
    val outsider: SessionDto,
    val household: HouseholdDto,
    val bills: List<BillDto> = emptyList(),
)

/**
 * The end-to-end demo scenario from design-plan section 8, run against a real server over HTTP. Each step asserts
 * what a user would see. Steps for bills, pantry, kitchen and the ledger are added in the milestones that build them.
 * [afterApprovalWindow] lets a test move the clock 48 hours and run the sweep; against a live server it stays null and
 * the silent approval is not exercised.
 */
class DemoScenario(private val api: DemoApi, private val afterApprovalWindow: (suspend () -> Unit)? = null) {
    suspend fun run(): DemoHousehold {
        assertEquals("ok", api.health().status)
        val people = signInAndInvite()
        storagePlaces(people)
        val demo = people.copy(bills = importInvoices(people))
        duplicatesAreRejected(demo)
        approveTheBills(demo, splitTheBills(demo))
        pantryFollowsTheKitchen(demo)
        closeTheMonth(demo)
        nonMembersSeeNothing(demo)
        return demo
    }

    /** Step 1: two flatmates and a cook sign in; one creates a household and invites the others. */
    private suspend fun signInAndInvite(): DemoHousehold {
        val owner = api.signIn("+919800000001", "Meera Iyer")
        val flatmate = api.signIn("+919800000002", "Aarav Shah")
        val cook = api.signIn("+919800000003", "Lakshmi")
        val outsider = api.signIn("+919800000099", "Eve")
        val created = api.createHousehold(owner, "Demo Flat 4B")

        val memberInvite = api.createdInvite(owner, created.id, RoleDto.MEMBER)
        val cookInvite = api.createdInvite(owner, created.id, RoleDto.COOK)
        api.accept(flatmate, memberInvite.code).expect(HttpStatusCode.OK)
        api.accept(cook, cookInvite.code).expect(HttpStatusCode.OK)
        assertEquals(
            "invite_used",
            api.accept(outsider, memberInvite.code).expect(HttpStatusCode.Conflict).body<ErrorDto>().code,
        )
        api.invite(flatmate, created.id, RoleDto.MEMBER).expect(HttpStatusCode.Forbidden)

        val household: HouseholdDto = api.household(owner, created.id).expect(HttpStatusCode.OK).body()
        assertEquals(
            mapOf("Meera Iyer" to RoleDto.OWNER, "Aarav Shah" to RoleDto.MEMBER, "Lakshmi" to RoleDto.COOK),
            household.members.associate { it.displayName to it.role },
        )
        listOf(owner, flatmate, cook).forEach {
            assertEquals(listOf(created.id), api.me(it).households.map { h -> h.id })
        }
        assertTrue(api.me(outsider).households.isEmpty())
        return DemoHousehold(owner, flatmate, cook, outsider, household)
    }

    /** Storage places used by the pantry: members add them, cooks see them but cannot change them. */
    private suspend fun storagePlaces(demo: DemoHousehold) {
        val id = demo.household.id
        api.addLocation(demo.flatmate, id, "Spice rack", LocationKindDto.SHELF).expect(HttpStatusCode.Created)
        api.addLocation(demo.cook, id, "Balcony", LocationKindDto.OTHER).expect(HttpStatusCode.Forbidden)
        assertEquals(
            listOf("Freezer", "Fridge", "Shelf", "Spice rack"),
            api.locationList(demo.cook, id).map { it.name },
        )
    }

    /**
     * Step 2: import all four invoices, including one order delivered as two PDFs, and resolve a reconciliation
     * warning. The PDFs are rendered from the redacted fixtures; the Zepto one carries a misprinted total.
     */
    private suspend fun importInvoices(demo: DemoHousehold): List<BillDto> {
        val id = demo.household.id
        val zepto = api.imported(demo.owner, id, Invoices.upload(Invoices.pdf("zepto", "867.00" to "868.00")))
        assertEquals(listOf(BillWarningCodeDto.TOTAL_MISMATCH), zepto.warnings.map { it.code })
        val resolved: BillDto = api.editBill(demo.owner, zepto.id, UpdateBillRequestDto(totalPaise = 86_700))
            .expect(HttpStatusCode.OK).body()
        assertTrue(resolved.warnings.isEmpty())

        val blinkit = api.imported(
            demo.flatmate,
            id,
            Invoices.upload(Invoices.pdf("blinkit"), Invoices.pdf("blinkit", "INV0000000000002" to "INV0000000000009")),
        )
        val instamart = api.imported(demo.owner, id, Invoices.upload(Invoices.pdf("instamart")))
        val handpickd = api.imported(demo.owner, id, Invoices.upload(Invoices.pdf("handpickd")))
        api.importInvoices(demo.cook, id, Invoices.upload(Invoices.pdf("zepto"))).expect(HttpStatusCode.Forbidden)

        val bills = listOf(resolved, blinkit, instamart, handpickd)
        assertEquals(
            mapOf(
                PlatformDto.ZEPTO to 86_700L,
                PlatformDto.BLINKIT to 25_800L,
                PlatformDto.INSTAMART to 46_901L,
                PlatformDto.HANDPICKD to 13_000L,
            ),
            bills.associate { it.platform to it.totalPaise },
        )
        assertTrue(bills.all { it.warnings.isEmpty() })
        assertEquals(blinkit, api.bill(demo.flatmate, blinkit.id).expect(HttpStatusCode.OK).body<BillDto>())
        api.bill(demo.owner, blinkit.id).expect(HttpStatusCode.NotFound)
        return bills
    }

    /** Step 7: uploading an invoice twice is rejected. */
    private suspend fun duplicatesAreRejected(demo: DemoHousehold) {
        val response = api.importInvoices(demo.flatmate, demo.household.id, Invoices.upload(Invoices.pdf("handpickd")))
        assertEquals("duplicate_invoice", response.expect(HttpStatusCode.Conflict).body<ErrorDto>().code)
    }

    private fun entries(vararg people: SessionDto) = people.map { SplitEntryDto(it.user.id) }

    /** Lines alternate between the owner, both of them and the flatmate, so every split mode on offer is used. */
    private fun mixedSplits(bill: BillDto, demo: DemoHousehold) = bill.lines.mapIndexed { index, line ->
        when (index % 3) {
            0 -> LineSplitEditDto(line.id, SplitModeDto.ONE, entries(demo.owner))
            1 -> LineSplitEditDto(line.id, SplitModeDto.EQUAL, entries(demo.owner, demo.flatmate))
            else -> LineSplitEditDto(line.id, SplitModeDto.ONE, entries(demo.flatmate))
        }
    }

    private suspend fun split(session: SessionDto, bill: BillDto, splits: List<LineSplitEditDto>): BillDto =
        api.editBill(session, bill.id, UpdateBillRequestDto(splits = splits)).expect(HttpStatusCode.OK).body()

    /**
     * Step 3: lines are split between the flatmates, a bill is typed in by hand and fees are shared. A bill cannot be
     * submitted before every line is split, and a cook never owes a share. Returns the hand-typed bill.
     */
    private suspend fun splitTheBills(demo: DemoHousehold): BillDto {
        val id = demo.household.id
        val (zepto, blinkit, instamart) = demo.bills
        val handpickd = demo.bills.last()
        api.submitBill(demo.owner, instamart.id).expect(HttpStatusCode.BadRequest)
        val cooksLine = LineSplitEditDto(instamart.lines.first().id, SplitModeDto.ONE, entries(demo.cook))
        api.editBill(demo.owner, instamart.id, UpdateBillRequestDto(splits = listOf(cooksLine)))
            .expect(HttpStatusCode.BadRequest)

        val splitZepto = split(demo.owner, zepto, mixedSplits(zepto, demo))
        assertEquals(zepto.totalPaise, splitZepto.shares.sumOf { it.totalPaise })
        assertEquals(setOf(demo.owner.user.id, demo.flatmate.user.id), splitZepto.shares.map { it.userId }.toSet())
        val splitBlinkit = split(demo.flatmate, blinkit, mixedSplits(blinkit, demo))
        assertEquals(blinkit.totalPaise, splitBlinkit.shares.sumOf { it.totalPaise })

        val gas = CreateBillRequestDto(
            title = "Gas cylinder",
            invoiceDate = "2026-10-05",
            lines = listOf(
                BillLineEditDto(displayName = "Cylinder", units = 1, amountPaise = 95_000),
                BillLineEditDto(displayName = "Hose clips", units = 3, amountPaise = 6_000),
            ),
        )
        api.createBill(demo.cook, id, gas).expect(HttpStatusCode.Forbidden)
        val manual: BillDto = api.createBill(demo.flatmate, id, gas).expect(HttpStatusCode.Created).body()
        assertEquals(BillStatusDto.DRAFT, manual.status)
        val (cylinder, clips) = manual.lines
        val byQuantity = LineSplitEditDto(
            clips.id,
            SplitModeDto.QUANTITY,
            listOf(SplitEntryDto(demo.owner.user.id, 2), SplitEntryDto(demo.flatmate.user.id, 1)),
        )
        val equally = LineSplitEditDto(cylinder.id, SplitModeDto.EQUAL, entries(demo.owner, demo.flatmate))
        val splitGas = split(demo.flatmate, manual, listOf(equally, byQuantity))
        // The cylinder is shared 47,500 each; the clips go 4,000 to the owner (2 of 3) and 2,000 to the flatmate.
        assertEquals(
            mapOf(demo.owner.user.id to 51_500L, demo.flatmate.user.id to 49_500L),
            splitGas.shares.associate { it.userId to it.totalPaise },
        )

        // Drafts are private to their uploader; the cook cannot see money at all.
        assertEquals(setOf(zepto.id, instamart.id, handpickd.id), api.billList(demo.owner, id).map { it.id }.toSet())
        assertEquals(setOf(blinkit.id, manual.id), api.billList(demo.flatmate, id).map { it.id }.toSet())
        api.bills(demo.cook, id).expect(HttpStatusCode.Forbidden)
        return manual
    }

    private suspend fun decide(session: SessionDto, bill: BillDto, state: ApprovalStateDto, comment: String? = null) =
        api.decide(session, bill.id, DecideApprovalRequestDto(state, comment))

    /**
     * Step 4: submitted bills ask the members who owe a share. One member asks for changes and approves after the
     * fix, an edit asks only the member it affects, and silence for 48 hours counts as approval.
     */
    private suspend fun approveTheBills(demo: DemoHousehold, gas: BillDto) {
        val (zepto, blinkit) = demo.bills
        val pending: BillDto = api.submitBill(demo.owner, zepto.id).expect(HttpStatusCode.OK).body()
        assertEquals(BillStatusDto.PENDING, pending.status)
        assertEquals(listOf(demo.flatmate.user.id), pending.approvals.map { it.userId })
        api.submitBill(demo.flatmate, zepto.id).expect(HttpStatusCode.Forbidden)
        decide(demo.cook, zepto, ApprovalStateDto.APPROVED).expect(HttpStatusCode.Forbidden)
        decide(demo.owner, zepto, ApprovalStateDto.APPROVED).expect(HttpStatusCode.Forbidden)
        decide(demo.flatmate, zepto, ApprovalStateDto.CHANGES_REQUESTED).expect(HttpStatusCode.BadRequest)
        val requested: BillDto = decide(demo.flatmate, zepto, ApprovalStateDto.CHANGES_REQUESTED, "Those were mine")
            .expect(HttpStatusCode.OK).body()
        assertEquals(BillStatusDto.CHANGES_REQUESTED, requested.status)

        api.submitBill(demo.owner, zepto.id).expect(HttpStatusCode.OK)
        val approved: BillDto = decide(demo.flatmate, zepto, ApprovalStateDto.APPROVED).expect(HttpStatusCode.OK).body()
        assertEquals(BillStatusDto.APPROVED, approved.status)
        decide(demo.flatmate, zepto, ApprovalStateDto.APPROVED).expect(HttpStatusCode.Conflict)

        val asked: BillDto = api.submitBill(demo.flatmate, gas.id).expect(HttpStatusCode.OK).body()
        assertEquals(listOf(demo.owner.user.id), asked.approvals.map { it.userId })
        decide(demo.owner, gas, ApprovalStateDto.APPROVED).expect(HttpStatusCode.OK)
        val raised = UpdateBillRequestDto(
            lines = listOf(
                BillLineEditDto(gas.lines[0].id, "Cylinder", 1, 96_000),
                BillLineEditDto(gas.lines[1].id, "Hose clips", 3, 6_000),
            ),
            totalPaise = 102_000,
        )
        val edited: BillDto = api.editBill(demo.flatmate, gas.id, raised).expect(HttpStatusCode.OK).body()
        assertEquals(BillStatusDto.PENDING, edited.status)
        assertEquals(ApprovalStateDto.PENDING, edited.approvals.single().state)
        api.editBill(demo.cook, gas.id, raised).expect(HttpStatusCode.Forbidden)

        api.submitBill(demo.flatmate, blinkit.id).expect(HttpStatusCode.OK)
        afterApprovalWindow?.let { moveClock ->
            moveClock()
            listOf(blinkit, gas).forEach { bill ->
                val settled: BillDto = api.bill(demo.flatmate, bill.id).expect(HttpStatusCode.OK).body()
                assertEquals(BillStatusDto.APPROVED, settled.status)
                assertTrue(settled.approvals.single().automatic)
            }
        }
    }

    /**
     * Step 5: the pantry fills from food lines (not from the non-food ones), items move between storage places and
     * are marked finished or discarded, and the cook adds something by hand.
     */
    private suspend fun pantryFollowsTheKitchen(demo: DemoHousehold) {
        val id = demo.household.id
        val foodLines = demo.bills.flatMap { it.lines }.filter { it.isFood }
        val items = api.pantryItems(demo.cook, id)
        assertEquals(foodLines.map { it.id }.toSet(), items.mapNotNull { it.sourceLineId }.toSet())
        assertTrue(demo.bills.flatMap { it.lines }.any { !it.isFood }, "the Blinkit hot/cold pack is not food")

        val freezer = demo.household.locations.single { it.name == "Freezer" }.id
        val shelf = demo.household.locations.single { it.name == "Shelf" }.id
        val beans = items.single { it.name.startsWith("Beans") }
        val moved = api.updatePantryItem(demo.flatmate, id, beans.id, UpdatePantryItemRequestDto(locationId = freezer))
            .expect(HttpStatusCode.OK).body<PantryItemDto>()
        assertEquals(freezer, moved.locationId)

        val milk = items.first { it.name.contains("Milk", ignoreCase = true) }
        val jam = items.first { it.id != milk.id && it.id != beans.id }
        api.updatePantryItem(demo.cook, id, milk.id, UpdatePantryItemRequestDto(status = PantryStatusDto.FINISHED))
            .expect(HttpStatusCode.OK)
        api.updatePantryItem(demo.owner, id, jam.id, UpdatePantryItemRequestDto(status = PantryStatusDto.DISCARDED))
            .expect(HttpStatusCode.OK)
        assertEquals(items.size - 2, api.pantryItems(demo.owner, id).size)
        assertEquals(items.size, api.pantryItems(demo.owner, id, "all").size)

        val rice = CreatePantryItemRequestDto(
            name = "Basmati rice",
            category = CategoryDto.STAPLES,
            quantity = QuantityDto(5_000_000, QuantityUnitDto.MILLIGRAM),
            locationId = shelf,
        )
        api.addPantryItem(demo.cook, id, rice).expect(HttpStatusCode.Created)
        assertEquals(items.size - 1, api.pantryItems(demo.owner, id).size)
    }

    /**
     * Step 8: balances come from the approved bills only and sum to zero, the month closes into stored transfers
     * with a UPI link for each, and a transfer is marked paid. Bills still awaiting approval carry on to next month.
     */
    private suspend fun closeTheMonth(demo: DemoHousehold) {
        val id = demo.household.id
        api.setUpi(demo.owner, "meera@okbank").expect(HttpStatusCode.OK)
        api.setUpi(demo.flatmate, "aarav@okhdfc").expect(HttpStatusCode.OK)
        api.setUpi(demo.owner, "no spaces allowed").expect(HttpStatusCode.BadRequest)

        val open = api.ledger(demo.flatmate, id)
        val month = open.month
        assertEquals(PeriodStatusDto.OPEN, open.status)
        assertEquals(0L, open.balances.sumOf { it.netPaise })
        assertTrue(open.transfers.isNotEmpty() && open.transfers.all { it.settlementId == null })
        api.ledgerResponse(demo.cook, id).expect(HttpStatusCode.Forbidden)
        api.ledgerResponse(demo.owner, id, "1999-01").expect(HttpStatusCode.NotFound)

        api.closeMonth(demo.flatmate, id, month).expect(HttpStatusCode.Forbidden)
        api.closeMonth(demo.owner, id, "1999-01").expect(HttpStatusCode.Conflict)
        val closed: LedgerDto = api.closeMonth(demo.owner, id, month).expect(HttpStatusCode.OK).body()
        assertEquals(PeriodStatusDto.LOCKED, closed.status)
        assertEquals(open.balances, closed.balances)
        val transfer = closed.transfers.single()
        assertEquals(SettlementStatusDto.PENDING, transfer.status)

        // What the app opens to pay: the receiver's address and the amount, in rupees.
        val receiver = if (transfer.to == demo.owner.user.id) demo.owner else demo.flatmate
        val link = UpiLink.build(
            transfer.payeeUpiVpa.orEmpty(),
            receiver.user.displayName,
            Money(transfer.amountPaise),
            "Rasoi ${closed.month}",
        )
        assertTrue(link?.startsWith("upi://pay?pa=${transfer.payeeUpiVpa}&") == true, link)

        val settlementId = transfer.settlementId.orEmpty()
        api.markPaid(demo.cook, settlementId).expect(HttpStatusCode.Forbidden)
        val paid: TransferDto = api.markPaid(demo.flatmate, settlementId).expect(HttpStatusCode.OK).body()
        assertEquals(SettlementStatusDto.PAID, paid.status)
        api.markPaid(demo.owner, settlementId).expect(HttpStatusCode.OK)
        assertEquals(SettlementStatusDto.PAID, api.ledger(demo.owner, id, month).transfers.single().status)

        val next = api.ledger(demo.owner, id)
        val following = requireNotNull(PeriodMonth.parse(month)).next().toString()
        assertEquals(following, next.month)
        assertEquals(listOf(following, month), next.months)
        assertTrue(api.billList(demo.owner, id).any { it.status == BillStatusDto.LOCKED })
    }

    /** Step 9: a non-member cannot read or change any of it. */
    private suspend fun nonMembersSeeNothing(demo: DemoHousehold) {
        val id = demo.household.id
        api.household(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.locations(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.addLocation(demo.outsider, id, "Sneaky", LocationKindDto.OTHER).expect(HttpStatusCode.NotFound)
        api.invite(demo.outsider, id, RoleDto.MEMBER).expect(HttpStatusCode.NotFound)
        api.importInvoices(demo.outsider, id, Invoices.upload(Invoices.pdf("zepto"))).expect(HttpStatusCode.NotFound)
        api.pantry(demo.outsider, id).expect(HttpStatusCode.NotFound)
        val someItem = api.pantryItems(demo.owner, id).first()
        api.updatePantryItem(demo.outsider, id, someItem.id, UpdatePantryItemRequestDto(name = "Mine now"))
            .expect(HttpStatusCode.NotFound)
        val basmati =
            CreatePantryItemRequestDto("Sneaky", CategoryDto.STAPLES, QuantityDto(1_000, QuantityUnitDto.COUNT))
        api.addPantryItem(demo.outsider, id, basmati).expect(HttpStatusCode.NotFound)
        api.bills(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.ledgerResponse(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.closeMonth(demo.outsider, id, "1999-01").expect(HttpStatusCode.NotFound)
        val gas = CreateBillRequestDto(
            "Sneaky",
            "2026-10-05",
            listOf(BillLineEditDto(displayName = "Cylinder", units = 1, amountPaise = 1)),
        )
        api.createBill(demo.outsider, id, gas).expect(HttpStatusCode.NotFound)
        demo.bills.forEach { bill ->
            api.submitBill(demo.outsider, bill.id).expect(HttpStatusCode.NotFound)
            api.decide(demo.outsider, bill.id, DecideApprovalRequestDto(ApprovalStateDto.APPROVED))
                .expect(HttpStatusCode.NotFound)
            api.bill(demo.outsider, bill.id).expect(HttpStatusCode.NotFound)
            api.editBill(demo.outsider, bill.id, UpdateBillRequestDto(totalPaise = 1)).expect(HttpStatusCode.NotFound)
        }
    }
}
