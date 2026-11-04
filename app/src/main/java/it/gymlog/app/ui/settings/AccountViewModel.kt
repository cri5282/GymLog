package it.gymlog.app.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.remote.AuthRepository
import it.gymlog.app.data.remote.AuthState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountViewModel(private val auth: AuthRepository) : ViewModel() {
    val state: StateFlow<AuthState> = auth.state
    val available: Boolean = auth.available

    fun signIn(activity: Activity?) = viewModelScope.launch { auth.signIn(activity) }
    fun signOut() = viewModelScope.launch { auth.signOut() }
    fun deleteAccount() = viewModelScope.launch { auth.deleteAccount() }
}
