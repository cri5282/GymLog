package it.gymlog.app.testutil

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import it.gymlog.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import java.io.File

fun testSettings(dir: File, scope: CoroutineScope) =
    SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(dir, "s.preferences_pb") })
