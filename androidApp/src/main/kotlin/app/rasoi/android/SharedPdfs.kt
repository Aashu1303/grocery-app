package app.rasoi.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import app.rasoi.core.domain.bills.InvoiceFile
import java.io.IOException

/** The files in a "Share" or "Open with" intent (the manifest only accepts PDFs); empty for any other intent. */
internal fun Intent.sharedPdfUris(): List<Uri> = when (action) {
    Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java))
    Intent.ACTION_SEND_MULTIPLE ->
        IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    Intent.ACTION_VIEW -> listOfNotNull(data)
    else -> emptyList()
}

/** Reads a shared PDF; one that cannot be read (revoked access, deleted file) is skipped. Call off the main thread. */
internal fun Context.readSharedPdf(uri: Uri): InvoiceFile? = try {
    contentResolver.openInputStream(uri)?.use { input -> InvoiceFile(displayName(uri), input.readBytes()) }
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}

private fun Context.displayName(uri: Uri): String = contentResolver
    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    ?: "invoice.pdf"
