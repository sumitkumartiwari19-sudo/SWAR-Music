package com.example.ui.screens.login

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.UserProfile
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository : AuthRepository {
        val userFlow = MutableStateFlow<UserProfile?>(null)
        var shouldSucceedSignIn = true
        var mockProfile = UserProfile(uid = "test_123", email = "test@example.com", displayName = "Test User")

        override val currentUser: Flow<UserProfile?> = userFlow.asStateFlow()

        override fun getCurrentUser(): UserProfile? = userFlow.value

        override suspend fun signInWithGoogle(context: Context): Result<UserProfile> {
            return if (shouldSucceedSignIn) {
                userFlow.value = mockProfile
                Result.success(mockProfile)
            } else {
                Result.failure(Exception("Sign in failed"))
            }
        }

        override suspend fun signOut(): Result<Unit> {
            userFlow.value = null
            return Result.success(Unit)
        }

        override suspend fun syncUserProfile(user: UserProfile): Result<Unit> {
            return Result.success(Unit)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialAuthState_isSignedOutWhenNoUser() = runTest(testDispatcher) {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AuthViewModel(fakeRepo)
        advanceUntilIdle()

        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun signInWithGoogle_success_emitsAuthenticated() = runTest(testDispatcher) {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AuthViewModel(fakeRepo)
        val context = ApplicationProvider.getApplicationContext<Context>()

        viewModel.signInWithGoogle(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Authenticated)
        assertEquals("test_123", (state as AuthUiState.Authenticated).user.uid)
    }

    @Test
    fun signInWithGoogle_failure_emitsError() = runTest(testDispatcher) {
        val fakeRepo = FakeAuthRepository().apply { shouldSucceedSignIn = false }
        val viewModel = AuthViewModel(fakeRepo)
        val context = ApplicationProvider.getApplicationContext<Context>()

        viewModel.signInWithGoogle(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Sign in failed", (state as AuthUiState.Error).message)

        viewModel.clearError()
        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
    }

    @Test
    fun signOut_resetsToSignedOut() = runTest(testDispatcher) {
        val fakeRepo = FakeAuthRepository().apply {
            userFlow.value = mockProfile
        }
        val viewModel = AuthViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Authenticated)

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(AuthUiState.SignedOut, viewModel.uiState.value)
    }
}
