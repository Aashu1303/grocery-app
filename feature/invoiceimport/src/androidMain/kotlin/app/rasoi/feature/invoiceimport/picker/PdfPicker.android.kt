package app.rasoi.feature.invoiceimport.picker

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import app.rasoi.core.domain.bills.InvoiceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val PDF_MIME_TYPE = "application/pdf"

@Composable
actual fun rememberPdfPicker(onPicked: (List<InvoiceFile>) -> Unit): PdfPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        scope.launch {
            val files = withContext(Dispatchers.IO) { uris.mapNotNull { context.readPdf(it) } }
            latest.value(files)
        }
    }
    return remember(launcher) { PdfPicker { launcher.launch(arrayOf(PDF_MIME_TYPE)) } }
}

private fun Context.readPdf(uri: Uri): InvoiceFile? = try {
    contentResolver.openInputStream(uri)?.use { input ->
        InvoiceFile(displayName(uri), input.readBytes())
    }
} catch (_: IOException) {
    null
} catch (_: SecurityException) {
    null
}

private fun Context.displayName(uri: Uri): String = contentResolver
    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    ?: "invoice.pdf"
