package app.rasoi.core.domain.bills

/** A picked invoice PDF; [name] is only shown to the user and never sent as part of the bill. */
class InvoiceFile(val name: String, val bytes: ByteArray)
