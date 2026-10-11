package app.rasoi.feature.household.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.household.CreateHouseholdUseCase
import app.rasoi.core.domain.household.HouseholdDetails
import app.rasoi.core.domain.household.JoinHouseholdUseCase
import app.rasoi.core.model.AppResult
import app.rasoi.feature.household.fieldError
import app.rasoi.feature.household.screenError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.core.annotation.KoinViewModel

/** Create a household in the device's time zone, or join one with an invite code. */
@KoinViewModel
class HouseholdSetupViewModel(
    private val createHousehold: CreateHouseholdUseCase,
    private val joinHousehold: JoinHouseholdUseCase,
    private val timeZoneId: () -> String = { TimeZone.currentSystemDefault().id },
) : ViewModel() {
    private val mutableState = MutableStateFlow(HouseholdSetupUiState())
    val state: StateFlow<HouseholdSetupUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<HouseholdSetupEffect>(Channel.BUFFERED)
    val effects: Flow<HouseholdSetupEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: HouseholdSetupUiEvent) {
        when (event) {
            is HouseholdSetupUiEvent.NameChanged -> mutableState.update {
                it.copy(name = event.value, nameError = null, error = null)
            }
            is HouseholdSetupUiEvent.CodeChanged -> mutableState.update {
                it.copy(code = event.value, codeError = null, error = null)
            }
            HouseholdSetupUiEvent.Create -> create()
            HouseholdSetupUiEvent.Join -> join()
        }
    }

    private fun create() {
        val current = mutableState.value
        if (current.isBusy) return
        mutableState.update { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            handle(createHousehold(current.name, timeZoneId()), field = "name")
            mutableState.update { it.copy(isCreating = false) }
        }
    }

    private fun join() {
        val current = mutableState.value
        if (current.isBusy) return
        mutableState.update { it.copy(isJoining = true, error = null) }
        viewModelScope.launch {
            handle(joinHousehold(current.code), field = "code")
            mutableState.update { it.copy(isJoining = false) }
        }
    }

    private suspend fun handle(result: AppResult<HouseholdDetails>, field: String) {
        when (result) {
            is AppResult.Success -> effectChannel.send(HouseholdSetupEffect.HouseholdReady(result.value.household.id))
            is AppResult.Failure -> {
                val fieldError = result.error.fieldError(field)
                mutableState.update {
                    if (field == "name") {
                        it.copy(
                            nameError = fieldError,
                            error = if (fieldError ==
                                null
                            ) {
                                result.error.screenError()
                            } else {
                                null
                            },
                        )
                    } else {
                        it.copy(
                            codeError = fieldError,
                            error = if (fieldError ==
                                null
                            ) {
                                result.error.screenError()
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}
