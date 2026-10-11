package app.rasoi.core.domain.bills

import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.Bill
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role

/** Checks the picked files against the server's limits (ADR 0018) so a doomed upload never starts. */
class ImportInvoicesUseCase(private val repository: BillRepository) {
    suspend operator fun invoke(householdId: HouseholdId, myRole: Role, files: List<InvoiceFile>): AppResult<Bill> {
        if (!RolePolicy.allows(myRole, Permission.EDIT_BILLS)) return AppResult.Failure(AppError.Forbidden)
        val violation = when {
            files.size !in 1..MAX_FILES -> AppError.FieldViolation("documents", "count")
            files.sumOf {
                it.bytes.size.toLong()
            } > MAX_TOTAL_BYTES -> AppError.FieldViolation("documents", "too_large")
            else -> files.indexOfFirst { it.bytes.isEmpty() }.takeIf { it >= 0 }
                ?.let { AppError.FieldViolation("documents[$it]", "empty") }
        }
        if (violation != null) return AppResult.Failure(AppError.Validation(listOf(violation)))
        return repository.importInvoices(householdId, files)
    }

    companion object {
        const val MAX_FILES = 5

        /** Raw bytes that still fit the server's 15 MiB Base64 body. */
        const val MAX_TOTAL_BYTES = 10 * 1024 * 1024
    }
}
