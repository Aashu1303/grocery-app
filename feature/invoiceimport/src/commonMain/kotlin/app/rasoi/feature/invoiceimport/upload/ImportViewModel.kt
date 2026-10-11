package app.rasoi.feature.invoiceimport.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.bills.ImportInvoicesUseCase
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.invoiceimport.billError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** Collects the PDFs of one order and uploads them; the server answers with a draft bill to review. */
@KoinViewModel
class ImportViewModel(
    @InjectedParam private val householdId: HouseholdId,
    @InjectedParam private val myRole: Role,
    private val importInvoices: ImportInvoicesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<ImportEffect>(Channel.BUFFERED)
    val effects: Flow<ImportEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: ImportUiEvent) {
        when (event) {
            is ImportUiEvent.FilesPicked -> mutableState.update {
                it.copy(files = (it.files + event.files).take(ImportInvoicesUseCase.MAX_FILES), error = null)
            }
            is ImportUiEvent.RemoveFile -> mutableState.update {
                it.copy(files = it.files.filterIndexed { index, _ -> index != event.index }, error = null)
            }
            ImportUiEvent.Upload -> upload()
        }
    }

    private fun upload() {
        val current = mutableState.value
        if (!current.canUpload) return
        mutableState.update { it.copy(isUploading = true, error = null) }
        viewModelScope.launch {
            when (val result = importInvoices(householdId, myRole, current.files)) {
                is AppResult.Success -> {
                    mutableState.update { it.copy(isUploading = false) }
                    effectChannel.send(ImportEffect.BillImported(result.value.id))
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(isUploading = false, error = result.error.billError())
                }
            }
        }
    }
}
