package app.rasoi.server.persistence

import app.rasoi.core.model.BillLineId
import app.rasoi.core.model.PantryItem

/** Pantry rows to write in the same transaction as a bill edit. */
data class PantryChanges(val add: List<PantryItem> = emptyList(), val removeForLines: List<BillLineId> = emptyList())
