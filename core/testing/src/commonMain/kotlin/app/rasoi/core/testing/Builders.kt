package app.rasoi.core.testing

import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.domain.split.LineAllocator
import app.rasoi.core.model.Allocation
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Approval
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Household
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Location
import app.rasoi.core.model.LocationId
import app.rasoi.core.model.LocationKind
import app.rasoi.core.model.Member
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.PantryItemId
import app.rasoi.core.model.PantryStatus
import app.rasoi.core.model.PhoneNumber
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.Role
import app.rasoi.core.model.SplitEntry
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.User
import app.rasoi.core.model.UserId
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** Test data builders with sensible defaults; override only what a test cares about. */
object Builders {
    fun user(id: String = "u1", name: String = "Meera", phone: String = "+919812345678") =
        User(UserId(id), requireNotNull(PhoneNumber.parse(phone)), name)

    fun household(id: String = "h1", name: String = "Flat 4B") =
        Household(HouseholdId(id), name, TimeZone.of("Asia/Kolkata"))

    fun member(id: String = "u1", name: String = "Meera", role: Role = Role.OWNER) = Member(UserId(id), name, role)

    fun location(id: String = "l1", name: String = "Fridge", kind: LocationKind = LocationKind.FRIDGE) =
        Location(LocationId(id), HouseholdId("h1"), name, kind)

    fun details(
        household: Household = household(),
        members: List<Member> = listOf(member()),
        locations: List<Location> = listOf(location()),
    ) = HouseholdDetails(household, members, locations)

    fun line(id: String = "line1", name: String = "Onion", amount: Long = 4_000, units: Int = 1) =
        BillLine(BillLineId(id), rawName = name, displayName = name, units = units, amount = Money(amount))

    fun charge(
        id: String = "c1",
        kind: ChargeKind = ChargeKind.HANDLING,
        label: String = "Handling",
        amount: Long = 500,
    ) = BillCharge(ChargeId(id), kind, label, Money(amount))

    /** A reconciled draft unless [total] says otherwise. */
    fun bill(
        id: String = "b1",
        lines: List<BillLine> = listOf(line()),
        charges: List<BillCharge> = emptyList(),
        total: Money = Money(lines.sumOf { it.amount.paise } + charges.sumOf { it.amount.paise }),
        status: BillStatus = BillStatus.DRAFT,
        confidence: ImportConfidence = ImportConfidence.HIGH,
        payerId: String = "u1",
        createdBy: String = payerId,
        allocations: List<Allocation> = emptyList(),
        approvals: List<Approval> = emptyList(),
        title: String? = null,
        platform: Platform = Platform.ZEPTO,
    ) = Bill(
        id = BillId(id),
        householdId = HouseholdId("h1"),
        platform = platform,
        invoiceNumber = "INV-1",
        orderId = "ORDER-1",
        invoiceDate = LocalDate.parse("2026-10-04"),
        payerId = UserId(payerId),
        createdBy = UserId(createdBy),
        status = status,
        confidence = confidence,
        lines = lines,
        charges = charges,
        total = total,
        allocations = allocations,
        approvals = approvals,
        title = title,
    )

    /** The allocations of [line] shared by [mode] among [users]; fails the test if the split is invalid. */
    fun split(line: BillLine, mode: SplitMode, vararg users: String): List<Allocation> {
        val split = LineSplit(mode, users.map { SplitEntry(UserId(it)) })
        return (LineAllocator.allocate(line, split) as AppResult.Success).value
    }

    fun pantryItem(
        id: String = "p1",
        name: String = "Rice",
        category: Category = Category.STAPLES,
        quantity: Quantity = Quantity.count(1),
        locationId: String? = "l1",
        expiresOn: LocalDate? = null,
        status: PantryStatus = PantryStatus.IN_STOCK,
    ) = PantryItem(
        id = PantryItemId(id),
        householdId = HouseholdId("h1"),
        name = name,
        category = category,
        quantity = quantity,
        locationId = locationId?.let(::LocationId),
        expiresOn = expiresOn,
        status = status,
        addedOn = LocalDate.parse("2026-10-04"),
    )
}
