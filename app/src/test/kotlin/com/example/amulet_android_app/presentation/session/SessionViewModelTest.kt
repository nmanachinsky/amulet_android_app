package com.example.amulet_android_app.presentation.session

import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.model.UserId
import com.example.amulet_android_app.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Rule
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits LoggedOut when session provider provides logged out`() = runTest {
        val provider = FakeUserSessionProvider(UserSessionContext.LoggedOut)
        val viewModel = SessionViewModel(provider)

        assertEquals(AuthState.LoggedOut, viewModel.state.value)
    }

    @Test
    fun `emits LoggedIn when session provider provides user`() = runTest {
        val context = UserSessionContext.LoggedIn(
            userId = UserId("id"),
            displayName = null,
            avatarUrl = null,
            timezone = null,
            language = null,
            consents = UserConsents()
        )
        val provider = FakeUserSessionProvider(context)
        val viewModel = SessionViewModel(provider)

        assertEquals(AuthState.LoggedIn, viewModel.state.value)
    }

    @Test
    fun `emits Guest when session provider provides guest context`() = runTest {
        val context = UserSessionContext.Guest(
            sessionId = "guest-123",
            displayName = "Guest User",
            language = "ru"
        )
        val provider = FakeUserSessionProvider(context)
        val viewModel = SessionViewModel(provider)

        assertEquals(AuthState.Guest, viewModel.state.value)
    }
}

private class FakeUserSessionProvider(initial: UserSessionContext) : UserSessionProvider {
    private val flow = MutableStateFlow(initial)

    override val sessionContext: StateFlow<UserSessionContext> = flow

    override val currentContext: UserSessionContext
        get() = flow.value
}
