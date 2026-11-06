package com.example.constructorapp.tier1_coverage

import com.example.constructorapp.ui.viewmodel.AuthUiState
import com.example.constructorapp.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun testInitialState_isIdle() = runTest {
        val viewModel = AuthViewModel()
        assertEquals(AuthUiState.Idle, viewModel.uiState.first())
    }

    @Test
    fun testLogin_emptyEmail_emitsError() = runTest {
        val viewModel = AuthViewModel()
        var successCalled = false
        viewModel.login("", "password123") { successCalled = true }

        val state = viewModel.uiState.first()
        assertTrue(state is AuthUiState.Error)
        assertEquals("Please fill all fields", (state as AuthUiState.Error).message)
        assertFalse(successCalled)
    }

    @Test
    fun testLogin_emptyPassword_emitsError() = runTest {
        val viewModel = AuthViewModel()
        var successCalled = false
        viewModel.login("user@example.com", "") { successCalled = true }

        val state = viewModel.uiState.first()
        assertTrue(state is AuthUiState.Error)
        assertEquals("Please fill all fields", (state as AuthUiState.Error).message)
        assertFalse(successCalled)
    }

    @Test
    fun testLogin_blankSpaces_emitsError() = runTest {
        val viewModel = AuthViewModel()
        var successCalled = false
        viewModel.login("   ", "   ") { successCalled = true }

        val state = viewModel.uiState.first()
        assertTrue(state is AuthUiState.Error)
        assertEquals("Please fill all fields", (state as AuthUiState.Error).message)
        assertFalse(successCalled)
    }
}
