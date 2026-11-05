package it.gymlog.app.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.gymlog.app.data.remote.AuthRepository
import it.gymlog.app.data.remote.AuthState
import it.gymlog.app.data.sync.CatalogSyncRunner
import it.gymlog.app.data.sync.SyncStatus
import it.gymlog.app.data.settings.SettingsRepository
import it.gymlog.app.data.repo.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class AccountViewModel(
    private val auth: AuthRepository,
    private val sync: CatalogSyncRunner?,
    private val settings: SettingsRepository,
    private val exercises: ExerciseRepository,
) : ViewModel() {
    val state: StateFlow<AuthState> = auth.state
    val available: Boolean = auth.available
    val syncState: StateFlow<SyncStatus> = sync?.status ?: MutableStateFlow(SyncStatus.Idle).asStateFlow()
    val catalogConsent = combine(settings.settings, auth.state) { s, a ->
        val uid = (a as? AuthState.SignedIn)?.uid
        uid != null && s.catalogConsentFor == uid
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _shareCount = MutableStateFlow(0)
    val shareCount: StateFlow<Int> = _shareCount.asStateFlow()
    private val _imageEditors = MutableStateFlow<List<String>>(emptyList())
    val imageEditors: StateFlow<List<String>> = _imageEditors.asStateFlow()
    private val _imageEditorMessage = MutableStateFlow<String?>(null)
    val imageEditorMessage: StateFlow<String?> = _imageEditorMessage.asStateFlow()

    init {
        viewModelScope.launch { _shareCount.value = exercises.countToShare() }
        viewModelScope.launch {
            auth.state.collect { if ((it as? AuthState.SignedIn)?.isAdmin == true) refreshImageEditors() }
        }
    }

    fun refreshShareCount() = viewModelScope.launch { _shareCount.value = exercises.countToShare() }

    fun signIn(activity: Activity?) = viewModelScope.launch { auth.signIn(activity) }
    fun signOut() = viewModelScope.launch { auth.signOut() }
    fun deleteAccount() = viewModelScope.launch { auth.deleteAccount() }
    fun syncNow() = viewModelScope.launch { sync?.syncNow() }
    fun grantCatalogConsent() = viewModelScope.launch {
        val uid = (auth.state.value as? AuthState.SignedIn)?.uid ?: return@launch
        settings.update { it.copy(catalogConsentFor = uid) }
        sync?.syncNow()
    }

    fun grantImageEditor(email: String) = updateImageEditor(email, true)
    fun revokeImageEditor(email: String) = updateImageEditor(email, false)

    private fun updateImageEditor(email: String, enabled: Boolean) = viewModelScope.launch {
        runCatching { auth.setImageEditor(email, enabled); refreshImageEditors() }
            .onSuccess { _imageEditorMessage.value = if (enabled) "Permesso assegnato." else "Permesso rimosso." }
            .onFailure { _imageEditorMessage.value = "Operazione non riuscita. Verifica che l'account esista e riprova." }
    }

    private suspend fun refreshImageEditors() {
        _imageEditors.value = runCatching { auth.imageEditors() }.getOrDefault(emptyList())
    }

    fun refreshPermissions() = viewModelScope.launch { auth.refreshImagePermission() }

    fun dismissImageEditorMessage() { _imageEditorMessage.value = null }
}
