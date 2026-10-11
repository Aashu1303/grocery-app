package app.rasoi.server.demo

import app.rasoi.core.contract.ErrorDto
import app.rasoi.core.contract.auth.SessionDto
import app.rasoi.core.contract.bills.BillDto
import app.rasoi.core.contract.bills.BillWarningCodeDto
import app.rasoi.core.contract.bills.PlatformDto
import app.rasoi.core.contract.bills.UpdateBillRequestDto
import app.rasoi.core.contract.household.HouseholdDto
import app.rasoi.core.contract.household.LocationKindDto
import app.rasoi.core.contract.household.RoleDto
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
 */
class DemoScenario(private val api: DemoApi) {
    suspend fun run(): DemoHousehold {
        assertEquals("ok", api.health().status)
        val people = signInAndInvite()
        storagePlaces(people)
        val demo = people.copy(bills = importInvoices(people))
        duplicatesAreRejected(demo)
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

    /** Step 9: a non-member cannot read or change any of it. */
    private suspend fun nonMembersSeeNothing(demo: DemoHousehold) {
        val id = demo.household.id
        api.household(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.locations(demo.outsider, id).expect(HttpStatusCode.NotFound)
        api.addLocation(demo.outsider, id, "Sneaky", LocationKindDto.OTHER).expect(HttpStatusCode.NotFound)
        api.invite(demo.outsider, id, RoleDto.MEMBER).expect(HttpStatusCode.NotFound)
        api.importInvoices(demo.outsider, id, Invoices.upload(Invoices.pdf("zepto"))).expect(HttpStatusCode.NotFound)
        demo.bills.forEach { bill ->
            api.bill(demo.outsider, bill.id).expect(HttpStatusCode.NotFound)
            api.editBill(demo.outsider, bill.id, UpdateBillRequestDto(totalPaise = 1)).expect(HttpStatusCode.NotFound)
        }
    }
}
