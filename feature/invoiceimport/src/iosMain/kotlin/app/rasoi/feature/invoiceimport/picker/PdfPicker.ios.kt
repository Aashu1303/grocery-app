package app.rasoi.feature.invoiceimport.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.rasoi.core.domain.bills.InvoiceFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberPdfPicker(onPicked: (List<InvoiceFile>) -> Unit): PdfPicker {
    val latest = rememberUpdatedState(onPicked)
    // UIKit holds a picker's delegate weakly, so the composition keeps it alive.
    val delegate = remember { PickerDelegate { files -> latest.value(files) } }
    return remember(delegate) { PdfPicker { present(delegate) } }
}

private fun present(delegate: PickerDelegate) {
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOfNotNull(UTTypePDF), asCopy = true)
    picker.allowsMultipleSelection = true
    picker.delegate = delegate
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

private fun topViewController(): UIViewController? {
    val scene = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>().firstOrNull()
    val root = scene?.windows?.filterIsInstance<UIWindow>()?.firstOrNull { it.isKeyWindow() }?.rootViewController
    return generateSequence(root) { it.presentedViewController }.lastOrNull()
}

private class PickerDelegate(private val onPicked: (List<InvoiceFile>) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onPicked(didPickDocumentsAtURLs.filterIsInstance<NSURL>().mapNotNull(::read))
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun read(url: NSURL): InvoiceFile? {
    val data = NSData.dataWithContentsOfURL(url) ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return InvoiceFile(url.lastPathComponent ?: "invoice.pdf", bytes)
}
