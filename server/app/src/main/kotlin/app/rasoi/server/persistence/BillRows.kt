package app.rasoi.server.persistence

import app.rasoi.core.model.Allocation
import app.rasoi.core.model.Approval
import app.rasoi.core.model.ApprovalState
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import app.rasoi.core.model.SplitMode
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.uuid.Uuid

/** Reads whole bills for [rows] with four queries however many bills there are. */
internal fun JdbcTransaction.loadBills(rows: List<ResultRow>): List<Bill> {
    if (rows.isEmpty()) return emptyList()
    val ids = rows.map { it[BillsTable.id] }
    val lines = BillLinesTable.selectAll().where { BillLinesTable.billId inList ids }
        .orderBy(BillLinesTable.position to SortOrder.ASC).groupBy({ it[BillLinesTable.billId] }, { it.toLine() })
    val charges = BillChargesTable.selectAll().where { BillChargesTable.billId inList ids }
        .orderBy(BillChargesTable.position to SortOrder.ASC).groupBy({ it[BillChargesTable.billId] }, { it.toCharge() })
    val allocations = (BillLinesTable innerJoin AllocationsTable).selectAll()
        .where { BillLinesTable.billId inList ids }
        .groupBy({ it[BillLinesTable.billId] }, { it.toAllocation() })
    val approvals = ApprovalsTable.selectAll().where { ApprovalsTable.billId inList ids }
        .groupBy({ it[ApprovalsTable.billId] }, { it.toApproval() })
    return rows.map { row ->
        val id = row[BillsTable.id]
        row.toBill(lines[id].orEmpty(), charges[id].orEmpty(), allocations[id].orEmpty(), approvals[id].orEmpty())
    }
}

internal fun JdbcTransaction.insertLines(billId: BillId, lines: List<IndexedValue<BillLine>>) {
    BillLinesTable.batchInsert(lines) { (position, line) ->
        this[BillLinesTable.id] = Uuid.parse(line.id.value)
        writeLine(billId.uuid(), position, line)
    }
}

internal fun JdbcTransaction.insertCharges(billId: BillId, charges: List<BillCharge>) {
    BillChargesTable.batchInsert(charges.withIndex().toList()) { (position, charge) ->
        this[BillChargesTable.id] = Uuid.parse(charge.id.value)
        this[BillChargesTable.billId] = billId.uuid()
        this[BillChargesTable.position] = position
        this[BillChargesTable.kind] = charge.kind.name
        this[BillChargesTable.label] = charge.label
        this[BillChargesTable.amountPaise] = charge.amount.paise
        this[BillChargesTable.distribution] = charge.distribution.name
    }
}

/** Allocations and approvals are written whole: they are derived from the bill and small. */
internal fun JdbcTransaction.replaceSplits(bill: Bill) {
    val lineIds = bill.lines.map { Uuid.parse(it.id.value) }
    AllocationsTable.deleteWhere { AllocationsTable.lineId inList lineIds }
    AllocationsTable.batchInsert(bill.allocations) { allocation ->
        this[AllocationsTable.lineId] = Uuid.parse(allocation.lineId.value)
        this[AllocationsTable.userId] = allocation.userId.uuid()
        this[AllocationsTable.mode] = allocation.mode.name
        this[AllocationsTable.units] = allocation.units
        this[AllocationsTable.amountPaise] = allocation.amount.paise
    }
    ApprovalsTable.deleteWhere { ApprovalsTable.billId eq bill.id.uuid() }
    ApprovalsTable.batchInsert(bill.approvals) { approval ->
        this[ApprovalsTable.billId] = bill.id.uuid()
        this[ApprovalsTable.userId] = approval.userId.uuid()
        this[ApprovalsTable.state] = approval.state.name
        this[ApprovalsTable.comment] = approval.comment
        this[ApprovalsTable.decidedAt] = approval.decidedAt
        this[ApprovalsTable.automatic] = approval.automatic
    }
}

internal fun UpdateBuilder<*>.writeLine(billId: Uuid, position: Int, line: BillLine) {
    this[BillLinesTable.billId] = billId
    this[BillLinesTable.position] = position
    this[BillLinesTable.rawName] = line.rawName
    this[BillLinesTable.displayName] = line.displayName
    this[BillLinesTable.units] = line.units
    this[BillLinesTable.amountPaise] = line.amount.paise
    this[BillLinesTable.measureAmount] = line.measure?.amount
    this[BillLinesTable.measureUnit] = line.measure?.unit?.name
    this[BillLinesTable.mrpPaise] = line.mrp?.paise
    this[BillLinesTable.hsn] = line.hsn
    this[BillLinesTable.upc] = line.upc
    this[BillLinesTable.category] = line.category.name
    this[BillLinesTable.isFood] = line.isFood
}

private fun ResultRow.toBill(
    lines: List<BillLine>,
    charges: List<BillCharge>,
    allocations: List<Allocation>,
    approvals: List<Approval>,
) = Bill(
    id = this[BillsTable.id].toBillId(),
    householdId = this[BillsTable.householdId].toHouseholdId(),
    platform = Platform.valueOf(this[BillsTable.platform]),
    invoiceNumber = this[BillsTable.invoiceNo],
    orderId = this[BillsTable.orderId],
    invoiceDate = this[BillsTable.invoiceDate],
    payerId = this[BillsTable.payerId].toUserId(),
    createdBy = this[BillsTable.createdBy].toUserId(),
    status = BillStatus.valueOf(this[BillsTable.status]),
    confidence = ImportConfidence.valueOf(this[BillsTable.confidence]),
    lines = lines,
    charges = charges,
    total = Money(this[BillsTable.totalPaise]),
    allocations = allocations,
    approvals = approvals,
    submittedAt = this[BillsTable.submittedAt],
    title = this[BillsTable.title],
    version = this[BillsTable.version],
)

private fun ResultRow.toLine() = BillLine(
    id = this[BillLinesTable.id].toBillLineId(),
    rawName = this[BillLinesTable.rawName],
    displayName = this[BillLinesTable.displayName],
    units = this[BillLinesTable.units],
    amount = Money(this[BillLinesTable.amountPaise]),
    measure = this[BillLinesTable.measureAmount]?.let { amount ->
        Quantity(amount, QuantityUnit.valueOf(requireNotNull(this[BillLinesTable.measureUnit])))
    },
    mrp = this[BillLinesTable.mrpPaise]?.let(::Money),
    hsn = this[BillLinesTable.hsn],
    upc = this[BillLinesTable.upc],
    category = this[BillLinesTable.category]?.let(Category::valueOf) ?: Category.OTHER,
    isFood = this[BillLinesTable.isFood] ?: false,
)

private fun ResultRow.toCharge() = BillCharge(
    id = this[BillChargesTable.id].toChargeId(),
    kind = ChargeKind.valueOf(this[BillChargesTable.kind]),
    label = this[BillChargesTable.label],
    amount = Money(this[BillChargesTable.amountPaise]),
    distribution = ChargeDistribution.valueOf(this[BillChargesTable.distribution]),
)

private fun ResultRow.toAllocation() = Allocation(
    lineId = this[AllocationsTable.lineId].toBillLineId(),
    userId = this[AllocationsTable.userId].toUserId(),
    mode = SplitMode.valueOf(this[AllocationsTable.mode]),
    units = this[AllocationsTable.units],
    amount = Money(this[AllocationsTable.amountPaise]),
)

private fun ResultRow.toApproval() = Approval(
    userId = this[ApprovalsTable.userId].toUserId(),
    state = ApprovalState.valueOf(this[ApprovalsTable.state]),
    comment = this[ApprovalsTable.comment],
    decidedAt = this[ApprovalsTable.decidedAt],
    automatic = this[ApprovalsTable.automatic],
)
