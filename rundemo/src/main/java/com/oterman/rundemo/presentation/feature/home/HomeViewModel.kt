package com.oterman.rundemo.presentation.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.oterman.rundemo.data.local.PreferencesManager
import com.oterman.rundemo.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * HomeScreen ViewModel
 * Manages tab navigation and profile/auth state
 * Corresponds to iOS NavigationCoordinator functionality
 */
class HomeViewModel(
    private val context: Context,
    private val preferencesManager: PreferencesManager = PreferencesManager(context)
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAuthState()
    }

    /**
     * Load authentication state from PreferencesManager
     */
    private fun loadAuthState() {
        val isLoggedIn = preferencesManager.isUserLoggedIn()
        val userName = preferencesManager.getUserName()

        Logger.d(TAG, "Auth state loaded: isLoggedIn=$isLoggedIn, userName=$userName")

        _uiState.update { state ->
            state.copy(
                isLoggedIn = isLoggedIn,
                userName = userName
            )
        }
    }

    /**
     * Refresh auth state (call when returning from login)
     */
    fun refreshAuthState() {
        loadAuthState()
    }

    /**
     * Switch selected tab
     */
    fun selectTab(tab: HomeTab) {
        Logger.d(TAG, "Tab selected: $tab")
        _uiState.update { state ->
            state.copy(selectedTab = tab)
        }
    }

    /**
     * Show logout confirmation dialog
     */
    fun showLogoutConfirmation() {
        _uiState.update { state ->
            state.copy(showLogoutConfirmDialog = true)
        }
    }

    /**
     * Dismiss logout confirmation dialog
     */
    fun dismissLogoutConfirmation() {
        _uiState.update { state ->
            state.copy(showLogoutConfirmDialog = false)
        }
    }

    /**
     * Perform logout
     * Corresponds to iOS coordinator.logout()
     */
    fun logout() {
        viewModelScope.launch {
            Logger.i(TAG, "User logging out")
            _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirmDialog = false) }

            // Clear user data
            preferencesManager.clearUserData()

            _uiState.update { state ->
                state.copy(
                    isLoggingOut = false,
                    isLoggedIn = false,
                    userName = null
                )
            }

            Logger.i(TAG, "Logout completed")
        }
    }

    /**
     * Trigger navigation to login page
     */
    fun navigateToLogin() {
        Logger.d(TAG, "Navigate to login requested")
        _uiState.update { state ->
            state.copy(navigateToLogin = true)
        }
    }

    /**
     * Reset login navigation event
     */
    fun resetNavigateToLogin() {
        _uiState.update { state ->
            state.copy(navigateToLogin = false)
        }
    }

    /**
     * Trigger navigation to welcome page
     * Corresponds to iOS coordinator.showWelcomePage()
     */
    fun navigateToWelcome() {
        Logger.d(TAG, "Navigate to welcome requested")
        _uiState.update { state ->
            state.copy(navigateToWelcome = true)
        }
    }

    /**
     * Reset welcome navigation event
     */
    fun resetNavigateToWelcome() {
        _uiState.update { state ->
            state.copy(navigateToWelcome = false)
        }
    }

    /**
     * Reset first launch state (for debugging)
     * Corresponds to iOS coordinator.resetFirstLaunch()
     */
    fun resetFirstLaunch() {
        Logger.d(TAG, "Reset first launch state")
        navigateToWelcome()
    }
}

/**
 * HomeViewModel factory
 */
class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
