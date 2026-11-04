package it.gymlog.app.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.remote.AuthRepository
import it.gymlog.app.data.remote.AuthState
import it.gymlog.app.data.sync.CatalogSyncRunner
import it.gymlog.app.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(
    private val auth: AuthRepository,
    private val sync: CatalogSyncRunner?,
) : ViewModel() {
    val state: StateFlow<AuthState> = auth.state
    val available: Boolean = auth.available
    val syncState: StateFlow<SyncStatus> = sync?.status ?: MutableStateFlow(SyncStatus.Idle).asStateFlow()

    fun signIn(activity: Activity?) = viewModelScope.launch { auth.signIn(activity) }
    fun signOut() = viewModelScope.launch { auth.signOut() }
    fun deleteAccount() = viewModelScope.launch { auth.deleteAccount() }
    fun syncNow() = viewModelScope.launch { sync?.syncNow() }
}
