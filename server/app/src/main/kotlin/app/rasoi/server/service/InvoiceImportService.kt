package app.rasoi.server.service

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.pantry.Categorizer
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.BillCharge
import app.rasoi.core.model.BillLine
import app.rasoi.core.model.BillStatus
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Platform
import app.rasoi.core.model.UserId
import app.rasoi.core.model.flatMap
import app.rasoi.server.parsing.InvoicePipeline
import app.rasoi.server.parsing.ParseResult
import app.rasoi.server.parsing.extract.PdfTextExtractor
import app.rasoi.server.parsing.model.ParsedLine
import app.rasoi.server.persistence.BillStore
import app.rasoi.server.persistence.NewDocument
import app.rasoi.server.persistence.toBillId
import app.rasoi.server.persistence.toBillLineId
import app.rasoi.server.persistence.toChargeId
import app.rasoi.server.storage.DocumentStorage
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Turns uploaded PDFs into one draft bill (design-plan 6.5): extract, parse (template, then fallback), merge, guard
 * against duplicates, store the files, then save. Failures name the document by position, never by content.
 */
class InvoiceImportService(
    private val households: HouseholdService,
    private val bills: BillStore,
    private val storage: DocumentStorage,
    private val pipeline: InvoicePipeline,
    private val planner: PantryPlanner,
    private val clock: Clock,
    private val extractor: PdfTextExtractor = PdfTextExtractor(),
) {
    private val log = LoggerFactory.getLogger(InvoiceImportService::class.java)

    suspend fun import(userId: UserId, householdId: HouseholdId, documents: List<ByteArray>): AppResult<Bill> {
        if (documents.size !in 1..MAX_DOCUMENTS) {
            return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("documents", "count"))))
        }
        return households.authorize(userId, householdId, Permission.EDIT_BILLS)
            .flatMap { parseAll(documents) }
            .flatMap(InvoiceMerge::merge)
            .flatMap { merged -> save(userId, householdId, documents, merged) }
    }

    private suspend fun parseAll(documents: List<ByteArray>): AppResult<List<ParseResult>> {
        val results = documents.mapIndexed { index, bytes ->
            when (val parsed = parse(bytes)) {
                is AppResult.Success -> parsed.value
                is AppResult.Failure -> return AppResult.Failure(parsed.error.forDocument(index))
            }
        }
        return AppResult.Success(results)
    }

    private suspend fun parse(bytes: ByteArray): AppResult<ParseResult> {
        val result = extractor.extract(bytes).flatMap { pipeline.parseWithFallback(it) }
        if (result !is AppResult.Failure || result.error is AppError.Validation) return result
        // The fallback's own outage must not block manual entry: report the document as unsupported.
        log.warn("Invoice fallback failed: {}", result.error::class.simpleName)
        return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("document", "unsupported_format"))))
    }

    private suspend fun save(
        userId: UserId,
        householdId: HouseholdId,
        documents: List<ByteArray>,
        merged: InvoiceMerge.Merged,
    ): AppResult<Bill> {
        val invoice = merged.invoice
        val hashes = documents.map(::sha256)
        if (bills.isDuplicate(householdId, invoice.platform, hashes, merged.invoiceNumbers)) {
            return AppResult.Failure(AppError.Conflict("duplicate_invoice"))
        }
        val stored = documents.indices.map { index ->
            NewDocument(
                merged.invoiceNumbers[index],
                "households/${householdId.value}/${hashes[index]}.pdf",
                hashes[index],
            )
        }
        stored.forEachIndexed { index, document ->
            val put = storage.put(document.storageKey, documents[index])
            if (put is AppResult.Failure) {
                stored.take(index).forEach { storage.delete(it.storageKey) }
                return put
            }
        }
        val bill = Bill(
            id = Uuid.random().toBillId(),
            householdId = householdId,
            platform = invoice.platform,
            invoiceNumber = invoice.invoiceNumber,
            orderId = invoice.orderId,
            invoiceDate = invoice.invoiceDate,
            payerId = userId,
            createdBy = userId,
            status = BillStatus.DRAFT,
            confidence = merged.confidence,
            lines = invoice.lines.map { line -> line.toBillLine(invoice.platform) },
            charges = invoice.charges.map { BillCharge(Uuid.random().toChargeId(), it.kind, it.label, it.amount) },
            total = invoice.total,
        )
        return bills.create(bill, stored, planner.itemsFor(bill), clock.now()).also { result ->
            if (result is AppResult.Failure) stored.forEach { storage.delete(it.storageKey) }
        }
    }

    private fun ParsedLine.toBillLine(platform: Platform): BillLine {
        val categorization = Categorizer.categorize(rawName, hsn, platform)
        return BillLine(
            id = Uuid.random().toBillLineId(),
            rawName = rawName,
            displayName = rawName,
            units = units,
            amount = amount,
            measure = measure,
            mrp = mrp,
            hsn = hsn,
            upc = upc,
            category = categorization.category,
            isFood = categorization.isFood,
        )
    }

    private fun AppError.forDocument(index: Int): AppError = when (this) {
        is AppError.Validation -> AppError.Validation(violations.map { it.copy(field = "documents[$index]") })
        else -> this
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_DOCUMENTS = 5
    }
}
