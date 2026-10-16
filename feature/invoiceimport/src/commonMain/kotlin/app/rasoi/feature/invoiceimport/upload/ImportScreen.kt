package app.rasoi.feature.invoiceimport.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.rasoi.core.designsystem.component.AppButton
import app.rasoi.core.designsystem.component.AppButtonVariant
import app.rasoi.core.designsystem.component.AppText
import app.rasoi.core.designsystem.component.AppTopBar
import app.rasoi.core.designsystem.component.EmptyView
import app.rasoi.core.designsystem.component.FileRow
import app.rasoi.core.designsystem.theme.AppTheme
import app.rasoi.core.domain.bills.InvoiceFile
import app.rasoi.core.model.BillId
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.invoiceimport.picker.rememberPdfPicker
import app.rasoi.feature.invoiceimport.resources.Res
import app.rasoi.feature.invoiceimport.resources.import_add_another
import app.rasoi.feature.invoiceimport.resources.import_choose
import app.rasoi.feature.invoiceimport.resources.import_empty_message
import app.rasoi.feature.invoiceimport.resources.import_empty_title
import app.rasoi.feature.invoiceimport.resources.import_file_size
import app.rasoi.feature.invoiceimport.resources.import_remove_file
import app.rasoi.feature.invoiceimport.resources.import_title
import app.rasoi.feature.invoiceimport.resources.import_upload
import app.rasoi.feature.invoiceimport.text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val BYTES_PER_KB = 1024

@Composable
fun ImportRoute(
    householdId: HouseholdId,
    myRole: Role,
    onBack: () -> Unit,
    onBillImported: (BillId) -> Unit,
    modifier: Modifier = Modifier,
    sharedFiles: List<InvoiceFile> = emptyList(),
    onSharedFilesTaken: () -> Unit = {},
    viewModel: ImportViewModel = koinViewModel(key = householdId.value) { parametersOf(householdId, myRole) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberPdfPicker { viewModel.onEvent(ImportUiEvent.FilesPicked(it)) }
    // PDFs shared from another app arrive already picked.
    LaunchedEffect(sharedFiles) {
        if (sharedFiles.isNotEmpty()) {
            viewModel.onEvent(ImportUiEvent.FilesPicked(sharedFiles))
            onSharedFilesTaken()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ImportEffect.BillImported -> onBillImported(effect.billId)
            }
        }
    }
    ImportScreen(state, viewModel::onEvent, onPick = picker::pick, onBack = onBack, modifier = modifier)
}

/** Choose invoice PDFs, then import them as one bill. */
@Composable
fun ImportScreen(
    state: ImportUiState,
    onEvent: (ImportUiEvent) -> Unit,
    onPick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(AppTheme.colors.surface)) {
        AppTopBar(title = stringResource(Res.string.import_title), onBack = onBack)
        if (state.files.isEmpty()) {
            EmptyView(
                title = stringResource(Res.string.import_empty_title),
                message = state.error?.text() ?: stringResource(Res.string.import_empty_message),
                actionLabel = stringResource(Res.string.import_choose),
                onAction = onPick,
                modifier = Modifier.weight(1f),
            )
        } else {
            FileList(state, onEvent, onPick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun FileList(state: ImportUiState, onEvent: (ImportUiEvent) -> Unit, onPick: () -> Unit, modifier: Modifier) {
    val remove = stringResource(Res.string.import_remove_file)
    Column(
        modifier.padding(AppTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(state.files, key = { index, file -> "$index-${file.name}" }) { index, file ->
                FileRow(
                    name = file.name,
                    detail = stringResource(
                        Res.string.import_file_size,
                        (file.bytes.size / BYTES_PER_KB).coerceAtLeast(1),
                    ),
                    removeLabel = remove,
                    onRemove = if (state.isUploading) null else ({ onEvent(ImportUiEvent.RemoveFile(index)) }),
                )
            }
        }
        state.error?.let { AppText(it.text(), color = AppTheme.colors.danger) }
        if (state.canAddMore) {
            AppButton(
                text = stringResource(Res.string.import_add_another),
                onClick = onPick,
                variant = AppButtonVariant.SECONDARY,
                enabled = !state.isUploading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AppButton(
            text = stringResource(Res.string.import_upload),
            onClick = { onEvent(ImportUiEvent.Upload) },
            enabled = state.canUpload || state.isUploading,
            loading = state.isUploading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
