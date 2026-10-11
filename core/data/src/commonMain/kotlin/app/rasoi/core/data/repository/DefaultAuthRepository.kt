package app.rasoi.core.data.repository

import app.rasoi.core.contract.auth.CreateSessionRequestDto
import app.rasoi.core.data.network.RasoiApi
import app.rasoi.core.data.session.SessionHolder
import app.rasoi.core.data.session.StoredSession
import app.rasoi.core.domain.auth.AuthRepository
import app.rasoi.core.domain.auth.AuthState
import app.rasoi.core.model.Account
import app.rasoi.core.model.AppResult
import app.rasoi.core.model.UserId
import app.rasoi.core.model.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus

/**
 * Auth state is derived from the stored session, so a 401 anywhere signs the user out everywhere. The derivation runs
 * unconfined so [state] is already current when `SessionHolder.restore` or `save` returns; on another dispatcher the
 * start-up routing read a stale SignedOut and a restored session landed on the sign-in screen.
 */
class DefaultAuthRepository(private val api: RasoiApi, private val sessions: SessionHolder, scope: CoroutineScope) :
    AuthRepository {
    override val state: StateFlow<AuthState> = sessions.session
        .map { it?.let { session -> AuthState.SignedIn(session.toAccount()) } ?: AuthState.SignedOut }
        .stateIn(scope + Dispatchers.Unconfined, SharingStarted.Eagerly, AuthState.SignedOut)

    override suspend fun signIn(idToken: String, displayName: String?): AppResult<Account> =
        api.createSession(CreateSessionRequestDto(idToken, displayName)).map { dto ->
            val session = StoredSession(dto.token, dto.expiresAt, dto.user.id, dto.user.displayName)
            sessions.save(session)
            session.toAccount()
        }

    override suspend fun signOut() = sessions.clear()

    private fun StoredSession.toAccount() = Account(UserId(userId), displayName)
}
