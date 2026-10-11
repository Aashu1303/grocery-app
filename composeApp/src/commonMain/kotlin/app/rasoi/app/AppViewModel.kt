package app.rasoi.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.auth.AuthState
import app.rasoi.core.domain.household.HouseholdRepository
import app.rasoi.core.model.Account
import app.rasoi.core.model.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Restores the session, then routes: signed out → sign-in, signed in without a household → setup, otherwise the
 * first household (Phase 1 flats have one).
 */
class AppViewModel(
    private val sessions: SessionHolder,
    private val auth: AuthRepository,
    private val households: HouseholdRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AppState>(AppState.Starting)
    val state: StateFlow<AppState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            sessions.restore()
            route(auth.state.value)
            auth.state.drop(1).collect { route(it) }
        }
    }

    fun refresh() {
        val account = (auth.state.value as? AuthState.SignedIn)?.account ?: return
        viewModelScope.launch { loadHouseholds(account) }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    private suspend fun route(authState: AuthState) {
        when (authState) {
            AuthState.SignedOut -> mutableState.value = AppState.SignedOut
            is AuthState.SignedIn -> loadHouseholds(authState.account)
        }
    }

    private suspend fun loadHouseholds(account: Account) {
        mutableState.value = AppState.Starting
        mutableState.value = when (val result = households.myHouseholds()) {
            is AppResult.Success -> result.value.firstOrNull()?.let { AppState.Ready(account, it) }
                ?: AppState.NeedsHousehold(account)
            is AppResult.Failure -> AppState.LoadFailed
        }
    }
}
