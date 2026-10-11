package app.rasoi.server.persistence

import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillId
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.PantryItem
import app.rasoi.core.model.Platform
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
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
                it[title] = bill.title
                it[version] = 0
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
        loadBills(BillsTable.selectAll().where { BillsTable.id eq id.uuid() }.toList()).singleOrNull()
    }

    /** A household's bills, newest invoice first. */
    suspend fun list(householdId: HouseholdId): List<Bill> = db.query {
        loadBills(
            BillsTable.selectAll()
                .where { BillsTable.householdId eq householdId.uuid() }
                .orderBy(BillsTable.invoiceDate to SortOrder.DESC, BillsTable.createdAt to SortOrder.DESC)
                .toList(),
        )
    }

    /** Pending bills submitted at or before [cutoff], whose silent approvals the system now gives. */
    suspend fun pendingSince(cutoff: Instant): List<Bill> = db.query {
        loadBills(
            BillsTable.selectAll()
                .where { (BillsTable.status eq BillStatus.PENDING.name) and (BillsTable.submittedAt lessEq cutoff) }
                .toList(),
        )
    }

    /**
     * Saves everything an edit, a submission or a decision can change: the header, lines, charges, allocations and
     * approvals, plus pantry rows. Lines keep their ids (allocations reference them), so existing rows are updated in
     * place, removed ones deleted and new ones inserted. The save fails with `Conflict("bill_changed")`, writing
     * nothing, when someone saved the bill since [bill] was read.
     */
    suspend fun save(bill: Bill, pantry: PantryChanges, now: Instant): AppResult<Bill> = try {
        db.query {
            val billId = bill.id.uuid()
            val claimed = BillsTable.update({ (BillsTable.id eq billId) and (BillsTable.version eq bill.version) }) {
                it[totalPaise] = bill.total.paise
                it[payerId] = bill.payerId.uuid()
                it[status] = bill.status.name
                it[submittedAt] = bill.submittedAt
                it[updatedAt] = now
                it[version] = bill.version + 1
            }
            if (claimed == 0) throw StaleBill()
            rewrite(bill, pantry, now)
        }
        AppResult.Success(bill.copy(version = bill.version + 1))
    } catch (_: StaleBill) {
        AppResult.Failure(AppError.Conflict("bill_changed"))
    }

    private fun JdbcTransaction.rewrite(bill: Bill, pantry: PantryChanges, now: Instant) {
        val billId = bill.id.uuid()
        val keep = bill.lines.map { Uuid.parse(it.id.value) }
        BillLinesTable.deleteWhere { (BillLinesTable.billId eq billId) and (BillLinesTable.id notInList keep) }
        val existing = BillLinesTable.selectAll().where { BillLinesTable.billId eq billId }
            .map { it[BillLinesTable.id] }.toSet()
        val (updated, added) = bill.lines.withIndex().partition { Uuid.parse(it.value.id.value) in existing }
        updated.forEach { (position, line) ->
            BillLinesTable.update({ BillLinesTable.id eq Uuid.parse(line.id.value) }) {
                it.writeLine(billId, position, line)
            }
        }
        insertLines(bill.id, added)
        BillChargesTable.deleteWhere { BillChargesTable.billId eq billId }
        insertCharges(bill.id, bill.charges)
        replaceSplits(bill)
        if (pantry.removeForLines.isNotEmpty()) deleteInStockItemsFor(pantry.removeForLines)
        insertPantryItems(pantry.add, now)
    }

    private class StaleBill : RuntimeException()

    private companion object {
        const val UNIQUE_VIOLATION = "23505"
    }
}
