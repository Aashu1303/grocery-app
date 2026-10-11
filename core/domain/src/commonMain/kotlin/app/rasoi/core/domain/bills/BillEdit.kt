package app.rasoi.core.domain.bills

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.ChargeId
import app.rasoi.core.model.ChargeKind
import app.rasoi.core.model.Money

/** A change to a bill's lines, charges or printed total; `null` leaves that part unchanged. */
data class BillEdit(val lines: List<LineEdit>? = null, val charges: List<ChargeEdit>? = null, val total: Money? = null)

/** A line after editing; [id] is null for a line the user added. [isFood] null keeps the line's current setting. */
data class LineEdit(
    val id: BillLineId?,
    val displayName: String,
    val units: Int,
    val amount: Money,
    val isFood: Boolean? = null,
)

/** A charge after editing; [id] is null for a charge the user added. */
data class ChargeEdit(val id: ChargeId?, val kind: ChargeKind, val label: String, val amount: Money)
