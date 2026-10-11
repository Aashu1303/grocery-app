package app.rasoi.core.testing

import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.auth.AuthState
import app.rasoi.core.model.Account
import app.rasoi.core.model.AppError
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Behaves like the server: an unknown [knownUsers] token needs a display name on first sign-in. */
class FakeAuthRepository(private val knownUsers: MutableMap<String, Account> = mutableMapOf()) : AuthRepository {
    private val mutableState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    override val state: StateFlow<AuthState> = mutableState
    var failure: AppError? = null
    val signInCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun signIn(idToken: String, displayName: String?): AppResult<Account> {
        signInCalls += idToken to displayName
        failure?.let { return AppResult.Failure(it) }
        val user = knownUsers[idToken]
            ?: displayName?.let { name -> Account(UserId("u${knownUsers.size + 1}"), name) }
                ?.also { knownUsers[idToken] = it }
            ?: return AppResult.Failure(AppError.Validation(listOf(AppError.FieldViolation("displayName", "required"))))
        mutableState.value = AuthState.SignedIn(user)
        return AppResult.Success(user)
    }

    override suspend fun signOut() {
        mutableState.value = AuthState.SignedOut
    }
}
