package app.rasoi.server.persistence

/** A stored PDF that belongs to a new bill. */
data class NewDocument(val invoiceNumber: String, val storageKey: String, val sha256: String)
