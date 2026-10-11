package app.rasoi.feature.bills.detail

import app.rasoi.core.model.AppError
import app.rasoi.core.model.SplitMode
import app.rasoi.core.model.UserId

/** Switching mode: a split between people becomes one person's when the mode is ONE. */
internal fun BillSheet.Split.withMode(mode: SplitMode) = copy(
    mode = mode,
    units = if (mode == SplitMode.ONE) units.entries.take(1).associate { it.key to 1 } else units,
    error = null,
)

/** Ticking a person: ONE keeps only them, other modes add or remove them. */
internal fun BillSheet.Split.toggled(userId: UserId): BillSheet.Split {
    val chosen = when {
        mode == SplitMode.ONE -> mapOf(userId to 1)
        userId in units -> units - userId
        else -> units + (userId to 1)
    }
    return copy(units = chosen, error = null)
}

/** Maps the allocator's refusal of a split to what the sheet explains. */
internal fun AppError.splitError(): SplitError {
    val code = (this as? AppError.Validation)?.violations?.firstOrNull()?.code
    return when (code) {
        "members_required" -> SplitError.NO_MEMBERS
        "one_member" -> SplitError.ONE_MEMBER
        else -> SplitError.UNITS
    }
}
