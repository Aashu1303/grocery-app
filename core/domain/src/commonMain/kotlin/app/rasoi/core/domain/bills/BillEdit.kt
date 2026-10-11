package app.rasoi.core.domain.bills

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeDistribution
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.LineSplit
import app.rasoi.core.model.Money
import app.rasoi.core.model.UserId

/**
 * A change to a bill; `null` leaves that part unchanged. [splits] replaces the split of the lines it names, and the
 * existing split of every other line is recomputed when its amount changes.
 */
data class BillEdit(
    val lines: List<LineEdit>? = null,
    val charges: List<ChargeEdit>? = null,
    val total: Money? = null,
    val payerId: UserId? = null,
    val splits: List<LineSplitEdit>? = null,
)

/** A line after editing; [id] is null for a line the user added. [isFood] null keeps the line's current setting. */
data class LineEdit(
    val id: BillLineId?,
    val displayName: String,
    val units: Int,
    val amount: Money,
    val isFood: Boolean? = null,
)

/** A charge after editing; [id] is null for a charge the user added; a null [distribution] keeps the current one. */
data class ChargeEdit(
    val id: ChargeId?,
    val kind: ChargeKind,
    val label: String,
    val amount: Money,
    val distribution: ChargeDistribution? = null,
)

/** How one existing line is shared. */
data class LineSplitEdit(val lineId: BillLineId, val split: LineSplit)
