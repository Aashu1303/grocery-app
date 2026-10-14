package app.rasoi.feature.household.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.domain.household.AddLocationUseCase
import app.rasoi.core.domain.household.CreateInviteUseCase
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.domain.household.Permission
import app.rasoi.core.domain.household.RolePolicy
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.HouseholdId
import app.rasoi.core.model.Role
import app.rasoi.feature.household.fieldError
import app.rasoi.feature.household.screenError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** Members and storage places of one household; owners invite people, owners and members add places. */
@KoinViewModel
class HouseholdViewModel(
    @InjectedParam private val householdId: HouseholdId,
    @InjectedParam private val myRole: Role,
    private val repository: HouseholdRepository,
    private val createInvite: CreateInviteUseCase,
    private val addLocation: AddLocationUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow<HouseholdUiState>(HouseholdUiState.Loading)
    val state: StateFlow<HouseholdUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: HouseholdUiEvent) {
        when (event) {
            HouseholdUiEvent.Retry -> load()
            HouseholdUiEvent.PullToRefresh -> pullToRefresh()
            HouseholdUiEvent.OpenInvite -> updateContent { it.copy(invite = InviteSheetState()) }
            is HouseholdUiEvent.InviteRoleSelected -> updateInvite {
                it.copy(role = event.role, code = null, error = null)
            }
            HouseholdUiEvent.CreateInvite -> submitInvite()
            HouseholdUiEvent.DismissInvite -> updateContent { it.copy(invite = null) }
            HouseholdUiEvent.OpenAddLocation -> updateContent { it.copy(addLocation = AddLocationState()) }
            is HouseholdUiEvent.LocationNameChanged -> updateLocation {
                it.copy(name = event.value, nameError = null, error = null)
            }
            is HouseholdUiEvent.LocationKindSelected -> updateLocation { it.copy(kind = event.kind) }
            HouseholdUiEvent.SaveLocation -> saveLocation()
            HouseholdUiEvent.DismissAddLocation -> updateContent { it.copy(addLocation = null) }
        }
    }

    /** A quiet reload ([showLoading] false) keeps what is on screen and any open sheet. */
    private fun load(showLoading: Boolean = true) {
        if (showLoading) mutableState.value = HouseholdUiState.Loading
        viewModelScope.launch {
            when (val result = repository.details(householdId)) {
                is AppResult.Success -> mutableState.update { previous ->
                    val kept = previous as? HouseholdUiState.Content
                    HouseholdUiState.Content(
                        details = result.value,
                        myRole = myRole,
                        canInvite = RolePolicy.allows(myRole, Permission.INVITE_MEMBERS),
                        canAddLocation = RolePolicy.allows(myRole, Permission.MANAGE_LOCATIONS),
                        invite = kept?.invite,
                        addLocation = kept?.addLocation,
                    )
                }
                is AppResult.Failure -> if (showLoading) {
                    mutableState.value = HouseholdUiState.Error(result.error.screenError())
                } else {
                    updateContent { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    private fun submitInvite() {
        val sheet = (mutableState.value as? HouseholdUiState.Content)?.invite ?: return
        if (sheet.isCreating) return
        updateInvite { it.copy(isCreating = true, error = null) }
        viewModelScope.launch {
            val result = createInvite(householdId, myRole, sheet.role)
            updateInvite {
                when (result) {
                    is AppResult.Success -> it.copy(isCreating = false, code = result.value.code.value)
                    is AppResult.Failure -> it.copy(isCreating = false, error = result.error.screenError())
                }
            }
        }
    }

    private fun saveLocation() {
        val form = (mutableState.value as? HouseholdUiState.Content)?.addLocation ?: return
        if (form.isSaving) return
        updateLocation { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = addLocation(householdId, myRole, form.name, form.kind)) {
                is AppResult.Success -> updateContent { content ->
                    val locations = (content.details.locations + result.value).sortedBy { it.name.lowercase() }
                    content.copy(details = content.details.copy(locations = locations), addLocation = null)
                }
                is AppResult.Failure -> updateLocation {
                    val nameError = result.error.fieldError("name")
                    it.copy(
                        isSaving = false,
                        nameError = nameError,
                        error = if (nameError == null) result.error.screenError() else null,
                    )
                }
            }
        }
    }

    private fun pullToRefresh() {
        val content = mutableState.value as? HouseholdUiState.Content ?: return
        mutableState.value = content.copy(isRefreshing = true)
        load(showLoading = false)
    }

    private fun updateContent(transform: (HouseholdUiState.Content) -> HouseholdUiState.Content) {
        mutableState.update { state -> if (state is HouseholdUiState.Content) transform(state) else state }
    }

    private fun updateInvite(transform: (InviteSheetState) -> InviteSheetState) =
        updateContent { content -> content.invite?.let { content.copy(invite = transform(it)) } ?: content }

    private fun updateLocation(transform: (AddLocationState) -> AddLocationState) =
        updateContent { content -> content.addLocation?.let { content.copy(addLocation = transform(it)) } ?: content }
}
