package com.example.amulet.core.auth.session.impl

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.example.amulet.core.auth.UserSessionManagerImpl
import com.example.amulet.core.auth.datastore.UserSessionPreferencesSerializer
import com.example.amulet.core.auth.session.proto.UserSessionPreferences
import com.example.amulet.shared.domain.privacy.model.UserConsents
import com.example.amulet.shared.domain.user.model.User
import com.example.amulet.shared.domain.user.model.UserId
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class UserSessionManagerImplTest {

    private val resources = mutableListOf<SessionResource>()

    @After
    fun tearDown() {
        resources.forEach(SessionResource::close)
        resources.clear()
    }

    @Test
    fun `выдает LoggedOut если сессия не сохранена`() = runTest {
        val resource = createResource()
        val state = resource.manager.sessionContext.first { it !is UserSessionContext.Loading }

        assertTrue(state is UserSessionContext.LoggedOut)
    }

    @Test
    fun `обновление сессии выдает LoggedIn состояние`() = runTest {
        val resource = createResource()
        val user = User(
            id = UserId("user-123"),
            displayName = "Sample",
            avatarUrl = "https://example.com/avatar.png",
            timezone = "Europe/Moscow",
            language = "ru",
            consents = UserConsents(analytics = true)
        )

        resource.manager.updateSession(user)
        val state = resource.manager.sessionContext.first { it is UserSessionContext.LoggedIn }

        val loggedIn = state as UserSessionContext.LoggedIn
        assertEquals(user.id, loggedIn.userId)
        assertEquals(user.displayName, loggedIn.displayName)
        assertEquals(user.avatarUrl, loggedIn.avatarUrl)
        assertEquals(user.timezone, loggedIn.timezone)
        assertEquals(user.language, loggedIn.language)
        assertEquals(user.consents, loggedIn.consents)
    }

    @Test
    fun `очистка сессии возвращает LoggedOut`() = runTest {
        val resource = createResource()
        val user = User(id = UserId("user"), consents = UserConsents())

        resource.manager.updateSession(user)
        resource.manager.sessionContext.first { it is UserSessionContext.LoggedIn }

        resource.manager.clearSession()
        val state = resource.manager.sessionContext.first { it is UserSessionContext.LoggedOut }

        assertTrue(state is UserSessionContext.LoggedOut)
    }

    @Test
    fun `сессия сохраняется в datastore`() = runTest {
        val resource = createResource()
        val user = User(id = UserId("persisted"), displayName = "First", consents = UserConsents(analytics = true))

        resource.manager.updateSession(user)
        resource.manager.sessionContext.first { it is UserSessionContext.LoggedIn }

        val stored = resource.dataStore.data.first()
        assertEquals(user.id.value, stored.userId)
        assertEquals(user.displayName, stored.displayName)
    }

    @Test
    fun `активация гостевого режима выдает Guest состояние`() = runTest {
        val resource = createResource()
        
        resource.manager.enableGuestMode(displayName = "Тестовый гость", language = "ru")
        val state = resource.manager.sessionContext.first { it is UserSessionContext.Guest }
        
        assertTrue(state is UserSessionContext.Guest)
        val guest = state as UserSessionContext.Guest
        assertEquals("Тестовый гость", guest.displayName)
        assertEquals("ru", guest.language)
        assertTrue(guest.sessionId.isNotBlank())
    }

    @Test
    fun `гостевая сессия сохраняется в datastore`() = runTest {
        val resource = createResource()
        
        resource.manager.enableGuestMode(displayName = "Guest User")
        resource.manager.sessionContext.first { it is UserSessionContext.Guest }
        
        val stored = resource.dataStore.data.first()
        assertTrue(stored.isGuest)
        assertTrue(stored.guestSessionId.isNotBlank())
        assertEquals("Guest User", stored.displayName)
    }

    @Test
    fun `переход из гостя в зарегистрированного пользователя`() = runTest {
        val resource = createResource()
        
        // Сначала гостевой режим
        resource.manager.enableGuestMode()
        resource.manager.sessionContext.first { it is UserSessionContext.Guest }
        
        // Затем регистрация
        val user = User(
            id = UserId("registered-user"),
            displayName = "Registered",
            consents = UserConsents(analytics = true)
        )
        resource.manager.updateSession(user)
        val state = resource.manager.sessionContext.first { it is UserSessionContext.LoggedIn }
        
        assertTrue(state is UserSessionContext.LoggedIn)
        val loggedIn = state as UserSessionContext.LoggedIn
        assertEquals(user.id, loggedIn.userId)
        
        // Проверка что гостевой режим отключен
        val stored = resource.dataStore.data.first()
        assertFalse(stored.isGuest)
    }

    @Test
    fun `очистка гостевой сессии возвращает LoggedOut`() = runTest {
        val resource = createResource()
        
        resource.manager.enableGuestMode()
        resource.manager.sessionContext.first { it is UserSessionContext.Guest }
        
        resource.manager.clearSession()
        val state = resource.manager.sessionContext.first { it is UserSessionContext.LoggedOut }
        
        assertTrue(state is UserSessionContext.LoggedOut)
    }

    private fun TestScope.createManager(storeFile: File): SessionResource {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val dataStore: DataStore<UserSessionPreferences> = DataStoreFactory.create(
            serializer = UserSessionPreferencesSerializer,
            scope = scope
        ) {
            storeFile
        }
        val resource = SessionResource(UserSessionManagerImpl(dataStore), dataStore, scope, storeFile)
        resources += resource
        return resource
    }

    private fun TestScope.createResource(file: File = newStoreFile()): SessionResource =
        createManager(file)

    private fun newStoreFile(): File {
        val directory = createTempDirectory(prefix = "session-manager").toFile()
        directory.deleteOnExit()
        return File(directory, "session.pb").apply { deleteOnExit() }
    }

    private data class SessionResource(
        val manager: UserSessionManagerImpl,
        val dataStore: DataStore<UserSessionPreferences>,
        val scope: TestScope,
        val file: File
    ) {
        fun close() {
            scope.cancel()
            file.parentFile?.deleteRecursively()
        }
    }
}
