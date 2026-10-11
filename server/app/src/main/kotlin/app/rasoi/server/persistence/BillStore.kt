package app.rasoi.server.persistence

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.Category
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.ImportConfidence
import app.rasoi.core.model.Money
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.Platform
import app.rasoi.core.model.Quantity
import app.rasoi.core.model.QuantityUnit
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Instant
import kotlin.uuid.Uuid

class BillStore(private val db: RasoiDatabase) {
    /** True when any of these files or invoice numbers was already imported into the household. */
    suspend fun isDuplicate(
        householdId: HouseholdId,
        platform: Platform,
        sha256s: List<String>,
        invoiceNumbers: List<String>,
    ): Boolean = db.query {
        BillDocumentsTable.selectAll()
            .where {
                (BillDocumentsTable.householdId eq householdId.uuid()) and (
                    (BillDocumentsTable.sha256 inList sha256s) or (
                        (BillDocumentsTable.platform eq platform.name) and
                            (BillDocumentsTable.invoiceNo inList invoiceNumbers)
                        )
                    )
            }
            .any()
    }

    /** Inserts the bill, its documents, lines and charges atomically; a concurrent duplicate is a conflict. */
    suspend fun create(
        bill: Bill,
        documents: List<NewDocument>,
        pantryItems: List<PantryItem>,
        now: Instant,
    ): AppResult<Bill> = try {
        db.query {
            BillsTable.insert {
                it[id] = bill.id.uuid()
                it[householdId] = bill.householdId.uuid()
                it[platform] = bill.platform.name
                it[invoiceNo] = bill.invoiceNumber
                it[orderId] = bill.orderId
                it[invoiceDate] = bill.invoiceDate
                it[payerId] = bill.payerId.uuid()
                it[createdBy] = bill.createdBy.uuid()
                it[status] = bill.status.name
                it[confidence] = bill.confidence.name
                it[totalPaise] = bill.total.paise
                it[createdAt] = now
                it[updatedAt] = now
            }
            BillDocumentsTable.batchInsert(documents) { document ->
                this[BillDocumentsTable.id] = Uuid.random()
                this[BillDocumentsTable.billId] = bill.id.uuid()
                this[BillDocumentsTable.householdId] = bill.householdId.uuid()
                this[BillDocumentsTable.platform] = bill.platform.name
                this[BillDocumentsTable.invoiceNo] = document.invoiceNumber
                this[BillDocumentsTable.storageKey] = document.storageKey
                this[BillDocumentsTable.sha256] = document.sha256
                this[BillDocumentsTable.createdAt] = now
            }
            insertLines(bill.id, bill.lines.withIndex().toList())
            insertCharges(bill.id, bill.charges)
            insertPantryItems(pantryItems, now)
        }
        AppResult.Success(bill)
    } catch (error: ExposedSQLException) {
        if (error.sqlState ==
            UNIQUE_VIOLATION
        ) {
            AppResult.Failure(AppError.Conflict("duplicate_invoice"))
        } else {
            throw error
        }
    }

    suspend fun find(id: BillId): Bill? = db.query {
        val row = BillsTable.selectAll().where { BillsTable.id eq id.uuid() }.singleOrNull() ?: return@query null
        val lines = BillLinesTable.selectAll()
            .where { BillLinesTable.billId eq id.uuid() }
            .orderBy(BillLinesTable.position to SortOrder.ASC)
            .map { it.toLine() }
        val charges = BillChargesTable.selectAll()
            .where { BillChargesTable.billId eq id.uuid() }
            .orderBy(BillChargesTable.position to SortOrder.ASC)
            .map { it.toCharge() }
        row.toBill(lines, charges)
    }

    /**
     * Saves edited lines, charges and total. Lines keep their ids (allocations will reference them), so existing
     * rows are updated in place, removed ones deleted and new ones inserted.
     */
    suspend fun updateContents(bill: Bill, pantry: PantryChanges, now: Instant): Bill = db.query {
        val billId = bill.id.uuid()
        val keep = bill.lines.map { it.id.value.let(Uuid::parse) }
        BillLinesTable.deleteWhere { (BillLinesTable.billId eq billId) and (BillLinesTable.id notInList keep) }
        val existing = BillLinesTable.selectAll().where { BillLinesTable.billId eq billId }
            .map { it[BillLinesTable.id] }.toSet()
        val (updated, added) = bill.lines.withIndex().partition { Uuid.parse(it.value.id.value) in existing }
        updated.forEach { (position, line) ->
            BillLinesTable.update({
                BillLinesTable.id eq Uuid.parse(line.id.value)
            }) { it.write(billId, position, line) }
        }
        insertLines(bill.id, added)
        BillChargesTable.deleteWhere { BillChargesTable.billId eq billId }
        insertCharges(bill.id, bill.charges)
        if (pantry.removeForLines.isNotEmpty()) deleteInStockItemsFor(pantry.removeForLines)
        insertPantryItems(pantry.add, now)
        BillsTable.update({ BillsTable.id eq billId }) {
            it[totalPaise] = bill.total.paise
            it[updatedAt] = now
        }
        bill
    }

    private fun JdbcTransaction.insertLines(billId: BillId, lines: List<IndexedValue<BillLine>>) {
        BillLinesTable.batchInsert(lines) { (position, line) ->
            this[BillLinesTable.id] = Uuid.parse(line.id.value)
            write(billId.uuid(), position, line)
        }
    }

    private fun JdbcTransaction.insertCharges(billId: BillId, charges: List<BillCharge>) {
        BillChargesTable.batchInsert(charges.withIndex().toList()) { (position, charge) ->
            this[BillChargesTable.id] = Uuid.parse(charge.id.value)
            this[BillChargesTable.billId] = billId.uuid()
            this[BillChargesTable.position] = position
            this[BillChargesTable.kind] = charge.kind.name
            this[BillChargesTable.label] = charge.label
            this[BillChargesTable.amountPaise] = charge.amount.paise
        }
    }

    private fun UpdateBuilder<*>.write(billId: Uuid, position: Int, line: BillLine) {
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

    private fun ResultRow.toBill(lines: List<BillLine>, charges: List<BillCharge>) = Bill(
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
    )

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
